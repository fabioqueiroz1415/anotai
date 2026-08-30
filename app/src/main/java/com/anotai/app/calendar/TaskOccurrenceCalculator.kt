package com.anotai.app.calendar

import com.anotai.app.data.TaskRecord
import com.anotai.app.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate

object TaskOccurrenceCalculator {

    fun occursOn(
        task: TaskRecord,
        date: LocalDate
    ): Boolean {
        val start = task.date ?: return false

        if (date.isBefore(start)) return false

        val until = task.untilDate
        if (until != null && date.isAfter(until)) {
            return false
        }

        return when (task.recurrenceType) {
            RecurrenceType.NONE ->
                date == start

            RecurrenceType.DAILY ->
                true

            RecurrenceType.WEEKLY -> {
                val days = task.weekdaysCsv
                    .split(",")
                    .mapNotNull { it.toIntOrNull() }
                    .mapNotNull {
                        runCatching {
                            DayOfWeek.of(it)
                        }.getOrNull()
                    }
                    .toSet()

                date.dayOfWeek in days
            }

            RecurrenceType.MONTHLY ->
                task.monthDay == date.dayOfMonth
        }
    }
}
