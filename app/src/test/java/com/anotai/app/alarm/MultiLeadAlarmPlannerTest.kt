package com.anotai.app.alarm

import com.anotai.app.data.TaskRecord
import com.anotai.app.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MultiLeadAlarmPlannerTest {

    private val zone = ZoneId.of("America/Sao_Paulo")

    private fun task() =
        TaskRecord(
            id = 1L,
            title = "Teste",
            originalText = "teste",
            date = LocalDate.of(2026, 8, 30),
            time = LocalTime.of(17, 0),
            reminderMinutesBefore = 60,
            recurrenceType = RecurrenceType.NONE,
            weekdaysCsv = "",
            monthDay = null,
            untilDate = null,
            notificationMinutesBefore = 120
        )

    @Test
    fun at1619PastTwoHourNotificationIsIgnored() {
        val now =
            ZonedDateTime.of(
                2026, 8, 30, 16, 19, 0, 0, zone
            )

        val plan =
            MultiLeadAlarmPlanner.plan(
                task(),
                ReminderLeadPolicy.DEFAULT_NOTIFICATION_MINUTES,
                now
            )!!

        assertEquals(30, plan.leadMinutes)
        assertFalse(plan.isCatchUp)
    }

    @Test
    fun at1619PastOneHourAlertIsIgnored() {
        val now =
            ZonedDateTime.of(
                2026, 8, 30, 16, 19, 0, 0, zone
            )

        val plan =
            MultiLeadAlarmPlanner.plan(
                task(),
                ReminderLeadPolicy.DEFAULT_ALERT_MINUTES,
                now
            )!!

        assertEquals(0, plan.leadMinutes)
    }

    @Test
    fun at1619PastPreAlertIsIgnoredAnd1630IsNext() {
        val now =
            ZonedDateTime.of(
                2026, 8, 30, 16, 19, 0, 0, zone
            )

        val plan =
            MultiLeadAlarmPlanner.plan(
                task(),
                ReminderLeadPolicy.preAlertSpecs("teste")
                    .map { it.notificationLeadMinutes },
                now
            )!!

        assertEquals(30, plan.leadMinutes)
    }

    @Test
    fun eventAlreadyPassedHasNoPlan() {
        val now =
            ZonedDateTime.of(
                2026, 8, 30, 18, 0, 0, 0, zone
            )

        assertNull(
            MultiLeadAlarmPlanner.plan(
                task(),
                ReminderLeadPolicy.DEFAULT_NOTIFICATION_MINUTES,
                now
            )
        )
    }
}
