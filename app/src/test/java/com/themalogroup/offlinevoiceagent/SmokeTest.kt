package com.themalogroup.offlinevoiceagent

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test fun routesMayaToolCommandsWithoutSubstringFalsePositives() {
        assertEquals(AssistantCommand.OPEN_CAMERA, CommandRouter.route("open camera"))
        assertEquals(AssistantCommand.OPEN_BROWSER, CommandRouter.route("open browser"))
        assertEquals(AssistantCommand.OPEN_MAPS, CommandRouter.route("open maps"))
        assertEquals(AssistantCommand.OPEN_YOUTUBE, CommandRouter.route("open youtube"))
        assertEquals(AssistantCommand.OPEN_PHONE, CommandRouter.route("open dialer"))
        assertEquals(AssistantCommand.OPEN_ALARM, CommandRouter.route("set alarm"))
        assertEquals(AssistantCommand.READ_SCREEN, CommandRouter.route("read screen"))
        assertEquals(AssistantCommand.SCREEN_TASK, CommandRouter.route("screen task search for science news"))
        assertEquals(AssistantCommand.SCREEN_TASK, CommandRouter.route("স্ক্রিনে কাজ সার্চ করো"))
        assertEquals(AssistantCommand.TAP_SCREEN_LABEL, CommandRouter.route("tap Search"))
        assertEquals(AssistantCommand.SCROLL_DOWN, CommandRouter.route("scroll down"))
        assertEquals(AssistantCommand.TYPE_SCREEN_TEXT, CommandRouter.route("type hello"))
        assertEquals(AssistantCommand.TAP_AT, CommandRouter.route("tap at 200, 400"))
        assertEquals(AssistantCommand.SWIPE, CommandRouter.route("swipe from 200, 800 to 200, 200"))
        assertEquals(AssistantCommand.PRESS_BACK, CommandRouter.route("press back"))
        assertEquals(AssistantCommand.PRESS_ENTER, CommandRouter.route("press enter"))
        assertEquals(AssistantCommand.OPEN_WIFI_SETTINGS, CommandRouter.route("open wifi settings"))
        assertEquals(AssistantCommand.OPEN_BLUETOOTH_SETTINGS, CommandRouter.route("open bluetooth settings"))
        assertEquals(AssistantCommand.WEB_SEARCH, CommandRouter.route("search web for weather"))
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("I watched a camera documentary"))
    }

    @Test fun configuresQwenTaskModelsByContextVariant() {
        assertEquals(1280, ModelRuntimeConfig.maxTokensFor("Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"))
        assertEquals(4096, ModelRuntimeConfig.maxTokensFor("Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task"))
        assertEquals(128, ModelRuntimeConfig.maxTokensFor("Qwen2.5-1.5B-Instruct_seq128_q8_ekv1280.task"))
        assertEquals(512, ModelRuntimeConfig.maxTokensFor("unknown-model.task"))
    }

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

    @Test fun routesBengaliTimeAndMemoryCommands() {
        assertEquals(AssistantCommand.TIME, CommandRouter.route("এখন সময় কত"))
        assertEquals(AssistantCommand.TIME, CommandRouter.route("এখন সময় কত"))
        assertEquals(AssistantCommand.REMEMBER, CommandRouter.route("মনে রেখো আমি বাংলায় কথা বলতে পছন্দ করি"))
        assertEquals("আমি বাংলায় কথা বলতে পছন্দ করি", CommandRouter.memoryText("মনে রেখো আমি বাংলায় কথা বলতে পছন্দ করি"))
        assertEquals(AssistantCommand.RECALL, CommandRouter.route("আমার কথা মনে আছে"))
        assertEquals(AssistantCommand.LIST_MEMORY, CommandRouter.route("নোট দেখাও"))
    }

    @Test fun rejectsAmbiguousPhoneActionPhrases() {
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("maybe open settings later"))
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("my calendar is busy"))
        assertEquals(AssistantCommand.UNKNOWN, CommandRouter.route("I like timeless music"))
    }

    @Test fun routesLocalMemoryCommands() {
        assertEquals(AssistantCommand.REMEMBER, CommandRouter.route("remember that I prefer Bengali"))
        assertEquals("I prefer Bengali", CommandRouter.memoryText("remember that I prefer Bengali"))
        assertEquals(AssistantCommand.LIST_MEMORY, CommandRouter.route("list memories"))
        assertEquals(AssistantCommand.RECALL, CommandRouter.route("what do you remember"))
        assertEquals(AssistantCommand.CLEAR_MEMORY, CommandRouter.route("forget all memories"))
        assertEquals(AssistantCommand.CONFIRM_CLEAR, CommandRouter.route("confirm clear memory"))
    }

    @Test fun screenTaskPlannerAcceptsOnlyBoundedAllowListedActions() {
        val valid = ScreenTaskPlanner.parse("""[{"action":"tap","value":"Search"},{"action":"type","value":"science facts"},{"action":"read","value":""}]""")
        assertEquals(3, valid.size)
        assertEquals("tap", valid.first().action)
        assertEquals(emptyList<ScreenTaskPlanner.Step>(), ScreenTaskPlanner.parse("""[{"action":"launch_app","value":"browser"}]"""))
        assertEquals(emptyList<ScreenTaskPlanner.Step>(), ScreenTaskPlanner.parse("""[{"action":"type","value":"my OTP is 123456"}]"""))
        assertEquals(emptyList<ScreenTaskPlanner.Step>(), ScreenTaskPlanner.parse("""[{"action":"tap","value":"Pay now"}]"""))
        assertEquals(emptyList<ScreenTaskPlanner.Step>(), ScreenTaskPlanner.parse("""[{"action":"read","value":""},{"action":"read","value":""},{"action":"read","value":""},{"action":"read","value":""},{"action":"read","value":""}]"""))
    }

    @Test fun screenTaskPlannerBlocksSensitiveGoalsButAllowsOrdinaryTasks() {
        assertEquals(true, ScreenTaskPlanner.isSensitiveGoal("open payment and pay now"))
        assertEquals(true, ScreenTaskPlanner.isSensitiveGoal("পেমেন্ট করো"))
        assertEquals(false, ScreenTaskPlanner.isSensitiveGoal("search for science news"))
    }
}
