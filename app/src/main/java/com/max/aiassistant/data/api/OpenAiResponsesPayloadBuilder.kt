package com.max.aiassistant.data.api

object OpenAiResponsesPayloadBuilder {

    const val MESSAGE_TYPE = "message"
    const val ROLE_USER = "user"
    const val ROLE_ASSISTANT = "assistant"
    const val INPUT_TEXT_TYPE = "input_text"
    const val OUTPUT_TEXT_TYPE = "output_text"
    const val INPUT_IMAGE_TYPE = "input_image"
    private const val IMAGE_DETAIL_AUTO = "auto"

    data class ChatTurn(
        val role: String,
        val text: String,
        val imageDataUrl: String? = null
    )

    fun buildRequestBody(
        model: String,
        history: List<ChatTurn>,
        userText: String,
        userImageDataUrl: String? = null,
        systemInstruction: String = ""
    ): Map<String, Any> {
        val requestBody = mutableMapOf<String, Any>(
            "model" to model,
            "input" to buildInput(
                history = history,
                userText = userText,
                userImageDataUrl = userImageDataUrl
            )
        )

        systemInstruction.trim()
            .takeIf { it.isNotBlank() }
            ?.let { requestBody["instructions"] = it }

        return requestBody
    }

    fun buildInput(
        history: List<ChatTurn>,
        userText: String,
        userImageDataUrl: String? = null
    ): List<Map<String, Any>> {
        return history.mapNotNull { turn ->
            buildMessage(
                role = turn.role,
                text = turn.text,
                encodedImageUrl = turn.imageDataUrl
            )
        } + listOfNotNull(
            buildMessage(
                role = ROLE_USER,
                text = userText,
                encodedImageUrl = userImageDataUrl
            )
        )
    }

    fun buildMessage(
        role: String,
        text: String,
        encodedImageUrl: String? = null
    ): Map<String, Any>? {
        val content = buildContent(
            role = role,
            text = text,
            encodedImageUrl = encodedImageUrl
        )

        if (content.isEmpty()) {
            return null
        }

        return mapOf(
            "type" to "message",
            "role" to role,
            "content" to content
        )
    }

    fun buildContent(
        role: String,
        text: String,
        encodedImageUrl: String? = null
    ): List<Map<String, Any>> {
        if (role != ROLE_USER && role != ROLE_ASSISTANT) {
            return emptyList()
        }

        val content = mutableListOf<Map<String, Any>>()
        val textType = if (role == ROLE_ASSISTANT) OUTPUT_TEXT_TYPE else INPUT_TEXT_TYPE

        text.trim()
            .takeIf { it.isNotBlank() }
            ?.let { normalizedText ->
                content += mapOf(
                    "type" to textType,
                    "text" to normalizedText
                )
            }

        if (role == ROLE_USER && encodedImageUrl != null) {
            content += mapOf(
                "type" to INPUT_IMAGE_TYPE,
                "detail" to IMAGE_DETAIL_AUTO,
                "image_url" to encodedImageUrl
            )
        }

        return content
    }
}
