package com.max.aiassistant.data.chat

object ChatMarkdownFormatter {

    private val inlineBulletRegex = Regex("""(?<!\*)[ \t]+\*(?![\*\s])""")
    private val lineStartBulletRegex = Regex("""(^|\n)\*(?![\*\s])""")

    fun normalizeForDisplay(text: String): String {
        if (text.isBlank() || !text.contains('*')) return text

        return text
            .replace(inlineBulletRegex, "\n* ")
            .replace(lineStartBulletRegex, "$1* ")
    }
}
