package com.themalogroup.offlinevoiceagent

import java.util.Locale

enum class AssistantCommand {
    TIME, DATE, HELLO, OPEN_SETTINGS, OPEN_CONTACTS, OPEN_CALENDAR, PICK_FILE, STOP_SPEAKING, HELP, UNKNOWN
}

object CommandRouter {
    fun route(raw: String): AssistantCommand {
        val command = raw.lowercase(Locale.ROOT).trim()
        return when {
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
}
