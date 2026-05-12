package com.max.aiassistant.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiResponsesPayloadBuilderTest {
    @Test
    fun buildRequestBody_usesOfficialContentTypesForMultiTurnHistory() {
        val requestBody = OpenAiResponsesPayloadBuilder.buildRequestBody(
            model = "gpt-5.5",
            history = listOf(
                OpenAiResponsesPayloadBuilder.ChatTurn(
                    role = OpenAiResponsesPayloadBuilder.ROLE_USER,
                    text = "Premier prompt"
                ),
                OpenAiResponsesPayloadBuilder.ChatTurn(
                    role = OpenAiResponsesPayloadBuilder.ROLE_ASSISTANT,
                    text = "Premiere reponse"
                )
            ),
            userText = "Deuxieme prompt",
            systemInstruction = "Sois concis."
        )
        val input = requestBody.inputItems()

        assertEquals(3, input.size)
        assertEquals("gpt-5.5", requestBody["model"])
        assertEquals("Sois concis.", requestBody["instructions"])
        assertEquals("user", input[0]["role"])
        assertEquals("assistant", input[1]["role"])
        assertEquals("user", input[2]["role"])

        val firstUserContent = input[0].contentItems()
        val assistantContent = input[1].contentItems()
        val secondUserContent = input[2].contentItems()

        assertEquals("input_text", firstUserContent[0]["type"])
        assertEquals("output_text", assistantContent[0]["type"])
        assertEquals("input_text", secondUserContent[0]["type"])
        assertFalse(
            "Un message assistant ne doit jamais repartir avec input_text.",
            assistantContent.any { it["type"] == "input_text" }
        )
    }

    @Test
    fun buildInput_keepsImagesOnlyOnUserMessages() {
        val input = OpenAiResponsesPayloadBuilder.buildInput(
            history = listOf(
                OpenAiResponsesPayloadBuilder.ChatTurn(
                    role = OpenAiResponsesPayloadBuilder.ROLE_ASSISTANT,
                    text = "Voici une reponse",
                    imageDataUrl = "data:image/png;base64,ignored"
                )
            ),
            userText = "Regarde cette image",
            userImageDataUrl = "data:image/png;base64,ok"
        )

        val assistantContent = input[0].contentItems()
        val userContent = input[1].contentItems()

        assertFalse(assistantContent.any { it["type"] == "input_image" })
        assertTrue(userContent.any { it["type"] == "input_image" })
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.contentItems(): List<Map<String, Any>> {
        return this["content"] as List<Map<String, Any>>
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.inputItems(): List<Map<String, Any>> {
        return this["input"] as List<Map<String, Any>>
    }
}
