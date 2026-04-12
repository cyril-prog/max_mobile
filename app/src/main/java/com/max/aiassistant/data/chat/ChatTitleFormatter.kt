package com.max.aiassistant.data.chat

object ChatTitleFormatter {
    private const val MAX_TITLE_WORDS = 8
    private const val MAX_TITLE_LENGTH = 60

    fun fallbackTitleFromPrompt(prompt: String): String {
        val normalizedPrompt = prompt
            .replace("\\s+".toRegex(), " ")
            .trim()
            .trim('"', '\'', '.', ',', ';', ':', '!', '?')

        if (normalizedPrompt.isBlank()) {
            return "Conversation sans titre"
        }

        val candidate = normalizedPrompt
            .split(" ")
            .filter { it.isNotBlank() }
            .take(MAX_TITLE_WORDS)
            .joinToString(" ")

        return candidate.take(MAX_TITLE_LENGTH).trim()
    }

    fun normalizeGeneratedTitle(rawTitle: String): String {
        val withoutMarkdown = rawTitle
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .replace("#", "")
            .replace("Titre :", "", ignoreCase = true)
            .replace("Title:", "", ignoreCase = true)
            .trim()
            .trim('"', '\'', '.', ',', ';', ':', '!', '?')

        val collapsed = withoutMarkdown.replace("\\s+".toRegex(), " ").trim()
        if (collapsed.isBlank()) {
            return "Conversation sans titre"
        }
        return collapsed.take(MAX_TITLE_LENGTH).trim()
    }
}
