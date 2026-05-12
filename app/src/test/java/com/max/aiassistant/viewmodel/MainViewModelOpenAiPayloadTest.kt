package com.max.aiassistant.viewmodel

import com.max.aiassistant.data.api.OpenAiResponsesPayloadBuilder
import com.max.aiassistant.data.api.OpenAiResponsesService
import com.max.aiassistant.data.local.db.ChatMessageEntity
import com.max.aiassistant.data.local.db.ChatMessageRole
import com.max.aiassistant.data.local.db.ChatMessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MainViewModelOpenAiPayloadTest {

    @Test
    fun `openai payload keeps user assistant user history with assistant output text`() {
        val history = listOf(
            completedMessage(
                id = "user-1",
                role = ChatMessageRole.USER,
                content = "Premier prompt"
            ),
            completedMessage(
                id = "assistant-1",
                role = ChatMessageRole.ASSISTANT,
                content = "Premiere reponse"
            )
        )

        val historyInput = history.map { message ->
            val role = when (message.role) {
                ChatMessageRole.USER -> "user"
                ChatMessageRole.ASSISTANT -> "assistant"
                else -> error("Role non supporte")
            }
            OpenAiResponsesPayloadBuilder.ChatTurn(
                role = role,
                text = message.content
            )
        }
        val requestBody = mapOf(
            "model" to OpenAiResponsesService.GPT_5_5_MODEL,
            "input" to OpenAiResponsesPayloadBuilder.buildInput(
                history = historyInput,
                userText = "Second prompt"
            )
        )

        val input = requestBody["input"] as List<Map<String, Any>>
        assertEquals(listOf("user", "assistant", "user"), input.map { it["role"] })

        val assistantContent = input[1]["content"] as List<Map<String, Any>>
        assertEquals("output_text", assistantContent.single()["type"])
        assertEquals("Premiere reponse", assistantContent.single()["text"])
        assertFalse(assistantContent.any { it["type"] == "input_text" })
    }

    private fun completedMessage(
        id: String,
        role: String,
        content: String
    ) = ChatMessageEntity(
        id = id,
        conversationId = "conversation-1",
        role = role,
        content = content,
        createdAt = 1L,
        imageUri = null,
        status = ChatMessageStatus.COMPLETED
    )

}
