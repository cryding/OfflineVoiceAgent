package com.themalogroup.offlinevoiceagent

import java.util.Locale

enum class AssistantCommand {
    TIME, DATE, HELLO, OPEN_SETTINGS, OPEN_CONTACTS, OPEN_CALENDAR, PICK_FILE,
    OPEN_CAMERA, OPEN_BROWSER, OPEN_MAPS, OPEN_YOUTUBE, SEARCH_YOUTUBE, OPEN_PHONE, OPEN_ALARM,
    OPEN_WIFI_SETTINGS, OPEN_BLUETOOTH_SETTINGS, WEB_SEARCH, ACCESSIBILITY_SETTINGS, READ_SCREEN, TAP_SCREEN_LABEL, SCROLL_DOWN, SCROLL_UP, TYPE_SCREEN_TEXT, TAP_AT, SWIPE, PRESS_BACK, PRESS_HOME, PRESS_ENTER,
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
            command in setOf("press back", "go back", "পিছনে যাও") -> AssistantCommand.PRESS_BACK
            command in setOf("press home", "go home", "হোমে যাও") -> AssistantCommand.PRESS_HOME
            command in setOf("press enter", "press search", "submit search", "সার্চ করো") -> AssistantCommand.PRESS_ENTER
            command.matches(Regex("""tap at\s+\d+\s*,\s*\d+""")) -> AssistantCommand.TAP_AT
            command.matches(Regex("""swipe from\s+\d+\s*,\s*\d+\s+to\s+\d+\s*,\s*\d+""")) -> AssistantCommand.SWIPE
            command in setOf("accessibility settings", "open accessibility settings", "screen control settings", "অ্যাক্সেসিবিলিটি সেটিংস") -> AssistantCommand.ACCESSIBILITY_SETTINGS
            command in setOf("read screen", "read this screen", "what is on my screen", "screen reader", "স্ক্রিন পড়ো", "স্ক্রিনে কী আছে") -> AssistantCommand.READ_SCREEN
            command in setOf("scroll down", "swipe down", "নিচে স্ক্রল করো", "নিচে যাও") -> AssistantCommand.SCROLL_DOWN
            command in setOf("scroll up", "swipe up", "উপরে স্ক্রল করো", "উপরে যাও") -> AssistantCommand.SCROLL_UP
            command.startsWith("tap ") || command.startsWith("click ") || command.startsWith("ট্যাপ করো ") -> AssistantCommand.TAP_SCREEN_LABEL
            command.startsWith("type ") || command.startsWith("টাইপ করো ") -> AssistantCommand.TYPE_SCREEN_TEXT
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
            command.startsWith("search youtube for ") || command.startsWith("youtube search ") ||
                command.startsWith("ইউটিউবে খোঁজো ") || command.startsWith("ইউটিউবে সার্চ করো ") ->
                AssistantCommand.SEARCH_YOUTUBE
            command in setOf("open youtube", "youtube kholo", "ইউটিউব খোলো", "ইউটিউব চালু করো") ->
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
