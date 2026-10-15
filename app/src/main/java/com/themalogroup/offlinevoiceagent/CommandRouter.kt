package com.themalogroup.offlinevoiceagent

import java.util.Locale

enum class AssistantCommand {
    TIME, DATE, HELLO, OPEN_SETTINGS, OPEN_CONTACTS, OPEN_CALENDAR, PICK_FILE,
    REMEMBER, RECALL, LIST_MEMORY, CLEAR_MEMORY, CONFIRM_CLEAR, STOP_SPEAKING, HELP, UNKNOWN
}

object CommandRouter {
    fun route(raw: String): AssistantCommand {
        val command = raw.lowercase(Locale.ROOT).trim()
        return when {
            command == "confirm forget all" || command == "confirm clear memory" -> AssistantCommand.CONFIRM_CLEAR
            command.contains("forget all memories") || command.contains("clear memory") || command.contains("delete all memories") -> AssistantCommand.CLEAR_MEMORY
            command.contains("what do you remember") || command.contains("what did i ask you to remember") ||
                command.contains("recall memory") || command.contains("আমার কথা মনে আছে") -> AssistantCommand.RECALL
            command == "list memories" || command == "show memories" || command.contains("আমার মনে রাখা কথা") -> AssistantCommand.LIST_MEMORY
            command.startsWith("remember that ") || command.startsWith("remember ") ||
                command.startsWith("মনে রেখো ") || command.startsWith("মনে রাখো ") -> AssistantCommand.REMEMBER
            command.contains("time") || command.contains("সময়") || command.contains("সময়") -> AssistantCommand.TIME
            command.contains("date") || command.contains("তারিখ") -> AssistantCommand.DATE
            command.contains("hello") || command.contains("হ্যালো") -> AssistantCommand.HELLO
            command.contains("open settings") || command == "settings" || command.contains("সেটিংস") -> AssistantCommand.OPEN_SETTINGS
            command.contains("open contacts") || command == "contacts" || command.contains("contact list") -> AssistantCommand.OPEN_CONTACTS
            command.contains("open calendar") || command == "calendar" || command.contains("schedule") -> AssistantCommand.OPEN_CALENDAR
            command.contains("open file") || command == "files" || command.contains("choose file") -> AssistantCommand.PICK_FILE
            command.contains("stop speaking") || command == "stop" -> AssistantCommand.STOP_SPEAKING
            command == "help" || command.contains("what can you do") || command.contains("সাহায্য") -> AssistantCommand.HELP
            else -> AssistantCommand.UNKNOWN
        }
    }

    fun memoryText(raw: String): String {
        val value = raw.trim()
        val prefixes = listOf("remember that ", "remember ", "মনে রেখো ", "মনে রাখো ")
        val prefix = prefixes.firstOrNull { value.lowercase(Locale.ROOT).startsWith(it) }
        return if (prefix == null) "" else value.drop(prefix.length).trim().trimEnd('.', '!', ' ')
    }
}
