package com.max.aiassistant.data.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject

object OpenAiResponsesParser {
    fun extractErrorMessage(response: JsonObject): String? {
        return extractErrorMessage(response.get("error"))
    }

    internal fun extractErrorMessage(errorElement: JsonElement?): String? {
        if (errorElement == null || errorElement.isJsonNull) {
            return null
        }

        return if (errorElement.isJsonObject) {
            errorElement.asJsonObject
                .get("message")
                .toNonBlankJsonString()
        } else {
            errorElement.toNonBlankJsonString()
        }
    }

    fun extractOutputText(response: JsonObject): String {
        response.get("output_text")
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val builder = StringBuilder()
        response.get("output")
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.forEach { outputElement ->
                if (!outputElement.isJsonObject) return@forEach
                val outputObject = outputElement.asJsonObject
                val role = outputObject.get("role")
                    ?.takeIf { !it.isJsonNull }
                    ?.asString
                if (role != null && role != "assistant") {
                    return@forEach
                }

                outputObject.get("content")
                    ?.takeIf { it.isJsonArray }
                    ?.asJsonArray
                    ?.forEach { contentElement ->
                        if (!contentElement.isJsonObject) return@forEach
                        val contentObject = contentElement.asJsonObject
                        val text = when (contentObject.get("type")?.takeIf { !it.isJsonNull }?.asString) {
                            "output_text", "text" -> contentObject.get("text")
                            "refusal" -> contentObject.get("refusal")
                            else -> null
                        }
                            ?.takeIf { !it.isJsonNull }
                            ?.asString
                            .orEmpty()

                        if (text.isNotBlank()) {
                            if (builder.isNotEmpty()) builder.appendLine()
                            builder.append(text)
                        }
                    }
            }

        return builder.toString().trim()
    }

    private fun JsonElement?.toNonBlankJsonString(): String? {
        if (this == null || this.isJsonNull || !this.isJsonPrimitive) {
            return null
        }

        return this.asJsonPrimitive
            .toString()
            .trim()
            .removeSurrounding("\"")
            .takeIf { it.isNotBlank() }
    }
}
