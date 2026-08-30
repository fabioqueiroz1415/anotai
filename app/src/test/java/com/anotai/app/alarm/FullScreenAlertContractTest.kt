package com.anotai.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenAlertContractTest {

    @Test
    fun modoDeTesteTemChaveEstavel() {
        assertEquals(
            "full_screen_test_mode",
            FullScreenAlertActivity.EXTRA_TEST_MODE
        )
    }

    @Test
    fun canalRealContinuaSendoDeLembretes() {
        assertTrue(
            AlarmReceiver.CHANNEL_ID.isNotBlank()
        )
    }
}
