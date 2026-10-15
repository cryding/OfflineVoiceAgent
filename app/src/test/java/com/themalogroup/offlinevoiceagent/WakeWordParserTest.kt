package com.themalogroup.offlinevoiceagent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordParserTest {
    @Test fun detectsWakePhraseAndExtractsFollowingCommand() {
        assertTrue(WakeWordParser.containsWakePhrase("Hey Maya"))
        assertTrue(WakeWordParser.containsWakePhrase("hey maya, what time is it?"))
        assertEquals("what time is it?", WakeWordParser.commandAfterWake("hey maya, what time is it?"))
        assertFalse(WakeWordParser.containsWakePhrase("maya, what time is it"))
    }

    @Test fun extractsCommandsAfterWakePhraseWithPunctuation() {
        assertEquals("open settings", WakeWordParser.commandAfterWake("Hey Maya: open settings"))
        assertEquals("তারিখ বলো", WakeWordParser.commandAfterWake("হেই মায়া, তারিখ বলো"))
        assertEquals("", WakeWordParser.commandAfterWake("Hey Maya"))
    }

    @Test fun rejectsPartialOrEmbeddedEnglishWakePhrase() {
        assertFalse(WakeWordParser.containsWakePhrase("hey mayas, open settings"))
        assertFalse(WakeWordParser.containsWakePhrase("they maya said hello"))
    }

    @Test fun normalizesDisplayTextWithoutChangingOriginalCommandParsing() {
        assertEquals("hey maya, hello", WakeWordParser.normalizeForDisplay("  Hey Maya, Hello  "))
        assertEquals("open settings", WakeWordParser.commandAfterWake("HEY MAYA! open settings"))
    }

    @Test fun supportsBengaliUnicodeVariantsAndRejectsPartialPhrase() {
        assertTrue(WakeWordParser.containsWakePhrase("হেই মায়া, সময় বলো"))
        assertFalse(WakeWordParser.containsWakePhrase("হে মায়াবী"))
    }

    @Test fun detectsBengaliWakePhrase() {
        assertTrue(WakeWordParser.containsWakePhrase("\u09b9\u09c7\u0987 \u09ae\u09be\u09af\u09bc\u09be"))
    }
}
