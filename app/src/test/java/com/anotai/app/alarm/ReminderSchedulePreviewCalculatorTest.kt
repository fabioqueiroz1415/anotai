package com.anotai.app.alarm

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSchedulePreviewCalculatorTest {

    @Test
    fun example1619To1700ShowsOnlyFutureTimes() {
        val now = LocalDateTime.of(2026, 8, 30, 16, 19)
        val event = LocalDateTime.of(2026, 8, 30, 17, 0)

        val result =
            ReminderSchedulePreviewCalculator.calculate(
                originalText = "tarefa 17h",
                eventAt = event,
                now = now
            )

        assertTrue(
            result.all { it.triggerAt.isAfter(now) }
        )

        assertEquals(
            listOf(
                LocalDateTime.of(2026, 8, 30, 16, 30),
                LocalDateTime.of(2026, 8, 30, 16, 30),
                LocalDateTime.of(2026, 8, 30, 16, 45),
                LocalDateTime.of(2026, 8, 30, 16, 55),
                LocalDateTime.of(2026, 8, 30, 17, 0),
                LocalDateTime.of(2026, 8, 30, 17, 0)
            ),
            result.map { it.triggerAt }
        )
    }

    @Test
    fun preAlertAt1630TargetsEventTimeAlert() {
        val result =
            ReminderSchedulePreviewCalculator.calculate(
                originalText = "tarefa 17h",
                eventAt = LocalDateTime.of(2026, 8, 30, 17, 0),
                now = LocalDateTime.of(2026, 8, 30, 16, 19)
            )

        val pre =
            result.first {
                it.kind == PreviewNoticeKind.PRE_ALERT
            }

        assertEquals(0, pre.targetAlertLeadMinutes)
    }
}
