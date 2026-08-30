package com.anotai.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmBehaviorContractTest {
    @Test
    fun canalDeTarefaEhEstavel() {
        assertEquals("task_alert_v5", AlarmReceiver.CHANNEL_ID)
    }

    @Test
    fun extraDeIdEhEstavel() {
        assertEquals("task_id", AlarmReceiver.EXTRA_TASK_ID)
    }

    @Test
    fun canalDeTesteExiste() {
        assertTrue(TestAlarmReceiver.CHANNEL_ID.isNotBlank())
    }
}
