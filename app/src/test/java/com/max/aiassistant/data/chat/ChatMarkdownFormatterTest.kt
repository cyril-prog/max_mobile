package com.max.aiassistant.data.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMarkdownFormatterTest {

    @Test
    fun `normalizes inline bullet list into separate lines`() {
        val normalized = ChatMarkdownFormatter.normalizeForDisplay(
            "Un agent IA sur telephone peut : *Assister a la communication *Automatiser des taches *Fournir des informations"
        )

        assertEquals(
            """
            Un agent IA sur telephone peut :
            * Assister a la communication
            * Automatiser des taches
            * Fournir des informations
            """.trimIndent(),
            normalized
        )
    }

    @Test
    fun `keeps markdown bold markers intact`() {
        val normalized = ChatMarkdownFormatter.normalizeForDisplay(
            "Voici **un point important** a retenir."
        )

        assertEquals("Voici **un point important** a retenir.", normalized)
    }

    @Test
    fun `preserves existing bullet lines`() {
        val normalized = ChatMarkdownFormatter.normalizeForDisplay(
            """
            Points utiles
            * Premier point
            * Second point
            """.trimIndent()
        )

        assertEquals(
            """
            Points utiles
            * Premier point
            * Second point
            """.trimIndent(),
            normalized
        )
    }
}
