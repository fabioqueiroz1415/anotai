package com.anotai.app.calendar

import com.anotai.app.data.TaskRecord
import com.anotai.app.model.RecurrenceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskOccurrenceCalculatorTest {

    private fun task(
        date: LocalDate,
        recurrence: RecurrenceType,
        weekdays: String = "",
        monthDay: Int? = null,
        until: LocalDate? = null
    ) = TaskRecord(
        id = 1,
        title = "Teste",
        originalText = "teste",
        date = date,
        time = LocalTime.of(8, 0),
        reminderMinutesBefore = 0,
        recurrenceType = recurrence,
        weekdaysCsv = weekdays,
        monthDay = monthDay,
        untilDate = until
    )

    @Test
    fun tarefaUnicaSoApareceNoDia() {
        val t = task(
            LocalDate.of(2026, 8, 30),
            RecurrenceType.NONE
        )

        assertTrue(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 8, 30)
            )
        )

        assertFalse(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 8, 31)
            )
        )
    }

    @Test
    fun diariaApareceDepoisDoInicio() {
        val t = task(
            LocalDate.of(2026, 8, 30),
            RecurrenceType.DAILY
        )

        assertTrue(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 9, 5)
            )
        )

        assertFalse(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 8, 29)
            )
        )
    }

    @Test
    fun semanalApareceNosDiasCorretos() {
        val t = task(
            LocalDate.of(2026, 8, 31),
            RecurrenceType.WEEKLY,
            weekdays = "1,3,5"
        )

        assertTrue(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 9, 2)
            )
        )

        assertFalse(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 9, 1)
            )
        )
    }

    @Test
    fun mensalApareceNoDiaDoMes() {
        val t = task(
            LocalDate.of(2026, 9, 5),
            RecurrenceType.MONTHLY,
            monthDay = 5
        )

        assertTrue(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 10, 5)
            )
        )

        assertFalse(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 10, 6)
            )
        )
    }

    @Test
    fun recorrenciaRespeitaDataFinal() {
        val t = task(
            LocalDate.of(2026, 9, 1),
            RecurrenceType.DAILY,
            until = LocalDate.of(2026, 9, 10)
        )

        assertTrue(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 9, 10)
            )
        )

        assertFalse(
            TaskOccurrenceCalculator.occursOn(
                t,
                LocalDate.of(2026, 9, 11)
            )
        )
    }
}
