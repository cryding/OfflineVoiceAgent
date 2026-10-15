package com.themalogroup.offlinevoiceagent

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test fun routesCommonPhoneTools() {
        assertEquals(AssistantCommand.TIME, CommandRouter.route("What time is it?"))
        assertEquals(AssistantCommand.DATE, CommandRouter.route("What is today's date?"))
        assertEquals(AssistantCommand.DATE, CommandRouter.route("আজকের তারিখ"))
        assertEquals(AssistantCommand.OPEN_SETTINGS, CommandRouter.route("open settings"))
        assertEquals(AssistantCommand.OPEN_CONTACTS, CommandRouter.route("open contacts"))
        assertEquals(AssistantCommand.OPEN_CALENDAR, CommandRouter.route("open calendar"))
        assertEquals(AssistantCommand.PICK_FILE, CommandRouter.route("choose file"))
    }

    @Test fun avoidsSubstringFalsePositives() {
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("sometimes I feel tired"))
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("send money now"))
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("open my settings for a poem"))
    }

    @Test fun normalizesWhitespaceAndRoutesBengaliCommands() {
        assertEquals(AssistantCommand.TIME, CommandRouter.route("  WHAT   TIME is it "))
        assertEquals(AssistantCommand.HELLO, CommandRouter.route("নমস্কার"))
        assertEquals(AssistantCommand.OPEN_SETTINGS, CommandRouter.route("সেটিংস খোলো"))
        assertEquals(AssistantCommand.OPEN_CONTACTS, CommandRouter.route("কন্টাক্ট খোলো"))
        assertEquals(AssistantCommand.OPEN_CALENDAR, CommandRouter.route("ক্যালেন্ডার খোলো"))
        assertEquals(AssistantCommand.PICK_FILE, CommandRouter.route("ফাইল খোলো"))
        assertEquals(AssistantCommand.STOP_SPEAKING, CommandRouter.route("চুপ করো"))
        assertEquals(AssistantCommand.HELP, CommandRouter.route("সাহায্য"))
    }

    @Test fun memoryCommandsRequireExplicitConfirmationForDeletion() {
        assertEquals(AssistantCommand.CLEAR_MEMORY, CommandRouter.route("forget all memories"))
        assertEquals(AssistantCommand.CONFIRM_CLEAR, CommandRouter.route("confirm forget all"))
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("please remember my memories are important"))
    }

    @Test fun routesLocalMemoryCommands() {
        assertEquals(AssistantCommand.REMEMBER, CommandRouter.route("remember that I prefer Bengali"))
        assertEquals("I prefer Bengali", CommandRouter.memoryText("remember that I prefer Bengali"))
        assertEquals(AssistantCommand.LIST_MEMORY, CommandRouter.route("list memories"))
        assertEquals(AssistantCommand.RECALL, CommandRouter.route("what do you remember"))
        assertEquals(AssistantCommand.CLEAR_MEMORY, CommandRouter.route("forget all memories"))
        assertEquals(AssistantCommand.CONFIRM_CLEAR, CommandRouter.route("confirm clear memory"))
    }
}
