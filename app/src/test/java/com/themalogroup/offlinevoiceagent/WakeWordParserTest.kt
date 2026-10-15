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

    @Test fun detectsBengaliWakePhrase() {
        assertTrue(WakeWordParser.containsWakePhrase("\u09b9\u09c7\u0987 \u09ae\u09be\u09af\u09bc\u09be"))
    }
}
