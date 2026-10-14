package com.anotai.app.alarm

import com.anotai.app.data.TaskRecord
import com.anotai.app.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

object NextOccurrenceCalculator {

    fun nextEvent(
        task: TaskRecord,
        now: ZonedDateTime =
            ZonedDateTime.now()
    ): ZonedDateTime? =
        nextCandidate(
            task = task,
            now = now,
            minutesBefore = 0
        )

    fun nextAlarm(
        task: TaskRecord,
        now: ZonedDateTime =
            ZonedDateTime.now()
    ): ZonedDateTime? =
        nextCandidate(
            task = task,
            now = now,
            minutesBefore =
                task.reminderMinutesBefore
        )

    private fun nextCandidate(
        task: TaskRecord,
        now: ZonedDateTime,
        minutesBefore: Int
    ): ZonedDateTime? {
        if (task.completed) return null

        val startDate =
            task.date ?: return null

        val time =
            task.time ?: return null

        val zone =
            now.zone

        val until =
            task.untilDate

        fun candidateFor(
            eventDate: LocalDate
        ): ZonedDateTime? {
            if (
                eventDate.isBefore(
                    startDate
                )
            ) {
                return null
            }

            if (
                until != null &&
                eventDate.isAfter(
                    until
                )
            ) {
                return null
            }

            val event =
                eventDate
                    .atTime(time)
                    .atZone(zone)

            return event.minusMinutes(
                minutesBefore.toLong()
            )
        }

        fun validFuture(
            eventDate: LocalDate
        ): ZonedDateTime? {
            val candidate =
                candidateFor(
                    eventDate
                ) ?: return null

            return if (
                candidate.isAfter(now)
            ) {
                candidate
            } else {
                null
            }
        }

        return when (
            task.recurrenceType
        ) {
            RecurrenceType.NONE ->
                validFuture(
                    startDate
                )

            RecurrenceType.DAILY -> {
                var d =
                    maxOf(
                        startDate,
                        now
                            .toLocalDate()
                            .minusDays(1)
                    )

                repeat(3700) {
                    validFuture(d)
                        ?.let {
                            return it
                        }

                    d =
                        d.plusDays(1)
                }

                null
            }

            RecurrenceType.WEEKLY -> {
                val days =
                    task.weekdaysCsv
                        .split(",")
                        .mapNotNull {
                            it.toIntOrNull()
                        }
                        .mapNotNull {
                            runCatching {
                                DayOfWeek.of(it)
                            }.getOrNull()
                        }
                        .toSet()

                var d =
                    maxOf(
                        startDate,
                        now
                            .toLocalDate()
                            .minusDays(1)
                    )

                repeat(3700) {
                    if (
                        d.dayOfWeek in days
                    ) {
                        validFuture(d)
                            ?.let {
                                return it
                            }
                    }

                    d =
                        d.plusDays(1)
                }

                null
            }

            RecurrenceType.MONTHLY -> {
                val day =
                    task.monthDay
                        ?: return null

                var ym =
                    YearMonth.from(
                        maxOf(
                            startDate,
                            now
                                .toLocalDate()
                                .minusMonths(1)
                        )
                    )

                repeat(240) {
                    if (
                        day <=
                        ym.lengthOfMonth()
                    ) {
                        val d =
                            ym.atDay(day)

                        validFuture(d)
                            ?.let {
                                return it
                            }
                    }

                    ym =
                        ym.plusMonths(1)
                }

                null
            }
        }
    }
}
