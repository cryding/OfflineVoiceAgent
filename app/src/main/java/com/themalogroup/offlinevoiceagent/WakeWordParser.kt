package com.themalogroup.offlinevoiceagent

import java.util.Locale

/**
 * Text-level wake phrase parsing for SpeechRecognizer results.
 * This is not an acoustic hotword detector; Android speech recognition must first produce text.
 */
object WakeWordParser {
    private val wakePattern = Regex("""\bhey\s+maya\b""", RegexOption.IGNORE_CASE)
    private val bengaliPattern = Regex("""হে\s*মায়া|হেই\s*মায়া|হে\s*মায়া|হেই\s*মায়া""")

    fun containsWakePhrase(text: String): Boolean =
        wakePattern.containsMatchIn(text) || bengaliPattern.containsMatchIn(text)

    fun commandAfterWake(text: String): String {
        val english = wakePattern.replaceFirst(text, "").trim().trimStart(',', '.', '!', '?', ':', ';').trim()
        if (english != text) return english
        return bengaliPattern.replaceFirst(text, "").trim().trimStart(',', '.', '!', '?', ':', ';').trim()
    }

    fun normalizeForDisplay(text: String): String = text.lowercase(Locale.ROOT).trim()
}
