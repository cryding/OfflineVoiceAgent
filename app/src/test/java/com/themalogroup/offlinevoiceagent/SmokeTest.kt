package com.themalogroup.offlinevoiceagent

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test fun routesCommonPhoneTools() {
        assertEquals(AssistantCommand.TIME, CommandRouter.route("What time is it?"))
        assertEquals(AssistantCommand.DATE, CommandRouter.route("আজকের তারিখ"))
        assertEquals(AssistantCommand.OPEN_SETTINGS, CommandRouter.route("open settings"))
        assertEquals(AssistantCommand.OPEN_CONTACTS, CommandRouter.route("open contacts"))
        assertEquals(AssistantCommand.OPEN_CALENDAR, CommandRouter.route("open calendar"))
        assertEquals(AssistantCommand.PICK_FILE, CommandRouter.route("choose file"))
    }

    @Test fun routesLocalMemoryCommands() {
        assertEquals(AssistantCommand.REMEMBER, CommandRouter.route("remember that I prefer Bengali"))
        assertEquals("I prefer Bengali", CommandRouter.memoryText("remember that I prefer Bengali"))
        assertEquals(AssistantCommand.LIST_MEMORY, CommandRouter.route("list memories"))
        assertEquals(AssistantCommand.CLEAR_MEMORY, CommandRouter.route("forget all memories"))
        assertEquals(AssistantCommand.CONFIRM_CLEAR, CommandRouter.route("confirm clear memory"))
    }

    @Test fun unknownCommandDoesNotTriggerDangerousAction() {
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("send money now"))
    }
}
