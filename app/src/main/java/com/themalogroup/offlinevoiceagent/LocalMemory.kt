package com.themalogroup.offlinevoiceagent

import android.content.Context

/**
 * Explicit, user-controlled local notes. Data stays in this app's private preferences.
 * This is not cloud-synced and is not an LLM conversation history.
 */
class LocalMemory(context: Context) {
    private val preferences = context.getSharedPreferences("offline_voice_memory", Context.MODE_PRIVATE)

    fun add(note: String): Boolean {
        val clean = note.trim().take(500)
        if (clean.isBlank()) return false
        val notes = all().toMutableList()
        notes.add(clean)
        preferences.edit().putStringSet(KEY_NOTES, notes.toSet()).apply()
        return true
    }

    fun all(): List<String> = preferences.getStringSet(KEY_NOTES, emptySet())
        .orEmpty().toList().sorted()

    fun clear() {
        preferences.edit().remove(KEY_NOTES).apply()
    }

    companion object { private const val KEY_NOTES = "notes" }
}
