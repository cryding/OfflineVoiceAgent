package com.themalogroup.offlinevoiceagent

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small, bounded conversation history stored only in app-private preferences.
 * History is used as local context for the on-device model; it is not cloud-synced.
 */
class ChatHistoryStore(private val preferences: SharedPreferences) {
    data class Turn(val user: String, val assistant: String)

    @Synchronized
    fun load(): List<Turn> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val user = item.optString("user").trim()
                    val assistant = item.optString("assistant").trim()
                    if (user.isNotEmpty() && assistant.isNotEmpty()) {
                        add(Turn(user.take(MAX_USER_CHARS), assistant.take(MAX_ASSISTANT_CHARS)))
                    }
                }
            }.takeLast(MAX_TURNS)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun append(user: String, assistant: String) {
        val cleanUser = user.trim().take(MAX_USER_CHARS)
        val cleanAssistant = assistant.trim().take(MAX_ASSISTANT_CHARS)
        if (cleanUser.isEmpty() || cleanAssistant.isEmpty()) return
        val turns = load().toMutableList()
        turns.add(Turn(cleanUser, cleanAssistant))
        val array = JSONArray()
        turns.takeLast(MAX_TURNS).forEach { turn ->
            array.put(JSONObject().put("user", turn.user).put("assistant", turn.assistant))
        }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(KEY_HISTORY).apply()
    }

    fun buildPrompt(userText: String, notes: List<String>): String {
        // Keep prompts bounded for small on-device context windows (e.g. Qwen ekv1280).
        val recent = load().takeLast(2)
        val current = userText.trim().take(MAX_CURRENT_USER_CHARS)
        return buildString {
            append("You are a helpful offline voice assistant. Answer clearly and honestly. ")
            append("You have no internet access; do not claim to have checked live information. ")
            if (notes.isNotEmpty()) {
                append("\nUser-approved saved notes (use only when relevant):\n")
                notes.take(3).forEach { append("- ").append(it.take(120)).append('\n') }
            }
            if (recent.isNotEmpty()) {
                append("\nRecent conversation for context (most recent turns only):\n")
                recent.forEach { turn ->
                    append("User: ").append(turn.user.take(180)).append('\n')
                    append("Assistant: ").append(turn.assistant.take(220)).append('\n')
                }
            }
            append("\nCurrent user message: ").append(current)
            append("\nAssistant:")
        }.take(MAX_PROMPT_CHARS)
            .let { prompt ->
                // Preserve the current question even if saved context was unusually large.
                if (prompt.contains("Current user message:")) prompt
                else "You are a helpful offline assistant. Answer honestly.\nCurrent user message: $current\nAssistant:"
            }
    }

    companion object {
        private const val KEY_HISTORY = "conversation_history"
        private const val MAX_TURNS = 20
        private const val CONTEXT_TURNS = 6
        private const val MAX_USER_CHARS = 1000
        private const val MAX_CURRENT_USER_CHARS = 800
        private const val MAX_PROMPT_CHARS = 2600
        private const val MAX_ASSISTANT_CHARS = 2000
        private const val MAX_NOTES = 10
    }
}
