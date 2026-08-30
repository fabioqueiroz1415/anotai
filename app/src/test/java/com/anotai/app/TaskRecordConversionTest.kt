package com.anotai.app

import com.anotai.app.data.TaskRecord
import com.anotai.app.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskRecordConversionTest {

    @Test
    fun taskRecordMantemTextoOriginal() {
        val task = TaskRecord(
            id = 1,
            title = "Buscar filha",
            originalText =
                "buscar filha amanhã 8h e alertar 1 hora antes",
            date =
                LocalDate.of(2026, 8, 30),
            time =
                LocalTime.of(8, 0),
            reminderMinutesBefore = 60,
            recurrenceType =
                RecurrenceType.NONE,
            weekdaysCsv = "",
            monthDay = null,
            untilDate = null
        )

        assertEquals(
            "buscar filha amanhã 8h e alertar 1 hora antes",
            task.originalText
        )
    }
}
