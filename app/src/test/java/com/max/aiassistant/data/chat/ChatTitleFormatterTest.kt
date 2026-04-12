package com.max.aiassistant.data.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatTitleFormatterTest {

    @Test
    fun `fallback title trims prompt and keeps a short label`() {
        val title = ChatTitleFormatter.fallbackTitleFromPrompt(
            "   Organise ma semaine de travail avec trois priorites claires   "
        )

        assertEquals("Organise ma semaine de travail avec trois priorites", title)
    }

    @Test
    fun `normalize generated title strips wrappers`() {
        val title = ChatTitleFormatter.normalizeGeneratedTitle("Titre : \"Plan de semaine\"")

        assertEquals("Plan de semaine", title)
    }

    @Test
    fun `blank title falls back to default`() {
        val title = ChatTitleFormatter.normalizeGeneratedTitle("   ")

        assertEquals("Conversation sans titre", title)
    }
}
