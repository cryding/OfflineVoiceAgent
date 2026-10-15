package com.themalogroup.offlinevoiceagent

import java.util.Locale

enum class AssistantCommand {
    TIME, DATE, HELLO, OPEN_SETTINGS, OPEN_CONTACTS, OPEN_CALENDAR, PICK_FILE,
    OPEN_CAMERA, OPEN_BROWSER, OPEN_MAPS, OPEN_YOUTUBE, OPEN_PHONE, OPEN_ALARM,
    OPEN_WIFI_SETTINGS, OPEN_BLUETOOTH_SETTINGS, WEB_SEARCH,
    REMEMBER, RECALL, LIST_MEMORY, CLEAR_MEMORY, CONFIRM_CLEAR, STOP_SPEAKING, HELP, UNKNOWN
}

/**
 * Conservative, deterministic command matching. Unknown or ambiguous input is left for the
 * local language model instead of triggering a phone action by substring accident.
 */
object CommandRouter {
    private val timePattern = Regex("""\b(time|clock)\b|সময়|সময়""")
    private val datePattern = Regex("""\b(date|today|day)\b|তারিখ""")

    fun route(raw: String): AssistantCommand {
        val command = raw.lowercase(Locale.ROOT).trim().replace(Regex("""\s+"""), " ")
        return when {
            command in setOf("confirm forget all", "confirm clear memory", "হ্যাঁ, সব স্মৃতি মুছে দাও") ->
                AssistantCommand.CONFIRM_CLEAR
            containsAny(command, "forget all memories", "clear memory", "delete all memories", "forget everything",
                "সব স্মৃতি মুছে দাও", "সব মনে রাখা কথা মুছে দাও") -> AssistantCommand.CLEAR_MEMORY
            containsAny(command, "what do you remember", "what did i ask you to remember", "recall memory",
                "আমার কথা মনে আছে", "আমি কী মনে রাখতে বলেছিলাম") -> AssistantCommand.RECALL
            command in setOf("list memories", "show memories", "show my notes", "আমার মনে রাখা কথা", "নোট দেখাও") ->
                AssistantCommand.LIST_MEMORY
            startsWithAny(command, "remember that ", "remember ", "মনে রেখো ", "মনে রাখো ", "মনে রাখবে ") ->
                AssistantCommand.REMEMBER
            command in setOf("stop", "stop speaking", "quiet", "চুপ করো", "বলা বন্ধ করো") ->
                AssistantCommand.STOP_SPEAKING
            command == "help" || command == "what can you do" || command == "সাহায্য" ->
                AssistantCommand.HELP
            command in setOf("settings", "open settings", "phone settings", "সেটিংস খোলো") -> AssistantCommand.OPEN_SETTINGS
            command in setOf("contacts", "open contacts", "contact list", "my contacts", "কন্টাক্ট খোলো") ->
                AssistantCommand.OPEN_CONTACTS
            command in setOf("calendar", "open calendar", "my calendar", "schedule", "ক্যালেন্ডার খোলো") ->
                AssistantCommand.OPEN_CALENDAR
            command in setOf("files", "open file", "open files", "choose file", "pick a file", "ফাইল খোলো") ->
                AssistantCommand.PICK_FILE
            command in setOf("open camera", "camera kholo", "ক্যামেরা খোলো", "ক্যামেরা চালু করো") ->
                AssistantCommand.OPEN_CAMERA
            command in setOf("open browser", "open internet", "browser kholo", "ব্রাউজার খোলো") ->
                AssistantCommand.OPEN_BROWSER
            command in setOf("open maps", "open google maps", "navigate", "ম্যাপ খোলো", "গুগল ম্যাপ খোলো") ->
                AssistantCommand.OPEN_MAPS
            command in setOf("open youtube", "youtube kholo", "ইউটিউব খোলো") ->
                AssistantCommand.OPEN_YOUTUBE
            command in setOf("open phone", "open dialer", "dialer kholo", "ফোন খোলো", "ডায়ালার খোলো") ->
                AssistantCommand.OPEN_PHONE
            command in setOf("set alarm", "open alarm", "alarm kholo", "অ্যালার্ম দাও", "অ্যালার্ম খোলো") ->
                AssistantCommand.OPEN_ALARM
            command in setOf("wifi settings", "open wifi settings", "ওয়াইফাই সেটিংস") ->
                AssistantCommand.OPEN_WIFI_SETTINGS
            command in setOf("bluetooth settings", "open bluetooth settings", "ব্লুটুথ সেটিংস") ->
                AssistantCommand.OPEN_BLUETOOTH_SETTINGS
            command.startsWith("search web for ") || command.startsWith("google ") || command.startsWith("ওয়েবে খোঁজো ") ->
                AssistantCommand.WEB_SEARCH
            timePattern.containsMatchIn(command) -> AssistantCommand.TIME
            datePattern.containsMatchIn(command) -> AssistantCommand.DATE
            command == "hello" || command == "hi" || command == "hey" || command == "হ্যালো" || command == "নমস্কার" ->
                AssistantCommand.HELLO
            else -> AssistantCommand.UNKNOWN
        }
    }

    fun memoryText(raw: String): String {
        val value = raw.trim()
        val prefixes = listOf("remember that ", "remember ", "মনে রেখো ", "মনে রাখো ", "মনে রাখবে ")
        val prefix = prefixes.firstOrNull { value.lowercase(Locale.ROOT).startsWith(it) } ?: return ""
        return value.drop(prefix.length).trim().trimEnd('.', '!', '?', ' ')
    }

    private fun containsAny(value: String, vararg phrases: String): Boolean = phrases.any(value::contains)
    private fun startsWithAny(value: String, vararg prefixes: String): Boolean = prefixes.any(value::startsWith)
}
