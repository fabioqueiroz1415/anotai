package com.anotai.app.alarm

import com.anotai.app.data.TaskRecord
import com.anotai.app.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertNull
import org.junit.Test

class CompletedTaskSchedulingTest {

    @Test
    fun completedTaskHasNoNextOccurrence() {
        val now = ZonedDateTime.of(
            2026, 9, 3, 10, 0, 0, 0,
            ZoneId.of("America/Sao_Paulo")
        )
        val task = TaskRecord(
            id = 42L,
            title = "Consulta",
            originalText = "consulta amanhã 14h",
            date = LocalDate.of(2026, 9, 4),
            time = LocalTime.of(14, 0),
            reminderMinutesBefore = 60,
            recurrenceType = RecurrenceType.NONE,
            weekdaysCsv = "",
            monthDay = null,
            untilDate = null,
            completed = true
        )

        assertNull(NextOccurrenceCalculator.nextEvent(task, now))
        assertNull(NextOccurrenceCalculator.nextAlarm(task, now))
    }
}
