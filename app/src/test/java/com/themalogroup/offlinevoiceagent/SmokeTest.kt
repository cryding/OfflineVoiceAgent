package com.themalogroup.offlinevoiceagent

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandRouterTest {
    @Test fun routesTimeAndDate() {
        assertEquals(AssistantCommand.TIME, CommandRouter.route("What time is it?"))
        assertEquals(AssistantCommand.DATE, CommandRouter.route("আজকের তারিখ"))
    }
    @Test fun routesPhoneTools() {
        assertEquals(AssistantCommand.OPEN_SETTINGS, CommandRouter.route("open settings"))
        assertEquals(AssistantCommand.OPEN_CONTACTS, CommandRouter.route("open contacts"))
        assertEquals(AssistantCommand.OPEN_CALENDAR, CommandRouter.route("open calendar"))
        assertEquals(AssistantCommand.PICK_FILE, CommandRouter.route("choose file"))
    }
    @Test fun unknownCommandIsSafe() {
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("send money now"))
    }
}
