package com.themalogroup.offlinevoiceagent

import android.content.Context
import org.json.JSONArray

/**
 * Explicit, user-controlled local notes. Stored only in app-private preferences.
 * Newest notes are kept first and storage is bounded to avoid unbounded growth.
 */
class LocalMemory(context: Context) {
    private val preferences = context.getSharedPreferences("offline_voice_memory", Context.MODE_PRIVATE)

    @Synchronized
    fun add(note: String): Boolean {
        val clean = note.trim().take(MAX_NOTE_CHARS)
        if (clean.isBlank()) return false
        val notes = all().toMutableList()
        notes.removeAll { it.equals(clean, ignoreCase = true) }
        notes.add(0, clean)
        save(notes.take(MAX_NOTES))
        return true
    }

    @Synchronized
    fun all(): List<String> {
        val encoded = try {
            preferences.getString(KEY_NOTES_JSON, null)
        } catch (_: ClassCastException) {
            null
        }
        if (encoded != null) {
            return try {
                val array = JSONArray(encoded)
                buildList {
                    for (i in 0 until array.length()) {
                        val value = array.optString(i).trim().take(MAX_NOTE_CHARS)
                        if (value.isNotBlank() && none { it.equals(value, ignoreCase = true) }) add(value)
                        if (size >= MAX_NOTES) break
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        // One-time migration from the original StringSet-backed format.
        val legacy = try {
            preferences.getStringSet(KEY_LEGACY_NOTES, emptySet()).orEmpty().toList().sorted()
        } catch (_: ClassCastException) {
            emptyList()
        }
        if (legacy.isNotEmpty()) {
            val migrated = legacy.map { it.trim().take(MAX_NOTE_CHARS) }
                .filter(String::isNotBlank).distinctBy { it.lowercase() }.takeLast(MAX_NOTES).reversed()
            save(migrated)
            preferences.edit().remove(KEY_LEGACY_NOTES).apply()
            return migrated
        }
        return emptyList()
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(KEY_NOTES_JSON).remove(KEY_LEGACY_NOTES).apply()
    }

    @Synchronized
    private fun save(notes: List<String>) {
        val array = JSONArray()
        notes.take(MAX_NOTES).forEach { array.put(it.take(MAX_NOTE_CHARS)) }
        preferences.edit().putString(KEY_NOTES_JSON, array.toString()).remove(KEY_LEGACY_NOTES).apply()
    }

    companion object {
        private const val KEY_NOTES_JSON = "notes_json"
        private const val KEY_LEGACY_NOTES = "notes"
        private const val MAX_NOTES = 100
        private const val MAX_NOTE_CHARS = 500
    }
}
