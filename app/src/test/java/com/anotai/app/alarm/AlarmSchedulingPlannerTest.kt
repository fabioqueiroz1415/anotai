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
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulingPlannerTest {

    private val zone =
        ZoneId.of(
            "America/Sao_Paulo"
        )

    private fun task(
        date: LocalDate,
        time: LocalTime,
        recurrence:
            RecurrenceType =
                RecurrenceType.NONE,
        weekdaysCsv:
            String =
                "",
        monthDay:
            Int? =
                null,
        until:
            LocalDate? =
                null
    ) =
        TaskRecord(
            id = 1L,
            title = "Teste",
            originalText =
                "Teste",
            date = date,
            time = time,
            reminderMinutesBefore =
                60,
            recurrenceType =
                recurrence,
            weekdaysCsv =
                weekdaysCsv,
            monthDay =
                monthDay,
            untilDate =
                until,
            notificationMinutesBefore =
                120
        )

    @Test
    fun tarefa20hCriada1930Alerta1hFazCatchUp() {
        val date =
            LocalDate.of(
                2026,
                8,
                30
            )

        val now =
            ZonedDateTime.of(
                date,
                LocalTime.of(
                    19,
                    30
                ),
                zone
            )

        val plan =
            AlarmSchedulingPlanner
                .plan(
                    task =
                        task(
                            date,
                            LocalTime.of(
                                20,
                                0
                            )
                        ),
                    minutesBefore =
                        60,
                    now =
                        now
                )

        requireNotNull(plan)

        assertTrue(
            plan.isCatchUp
        )

        assertEquals(
            now
                .plusSeconds(3)
                .toInstant()
                .toEpochMilli(),
            plan.triggerAtMillis
        )

        assertEquals(
            LocalTime.of(
                20,
                0
            ),
            plan.eventAt
                .toLocalTime()
        )
    }

    @Test
    fun tarefa20hCriada17hAlerta1hMantem19h() {
        val date =
            LocalDate.of(
                2026,
                8,
                30
            )

        val now =
            ZonedDateTime.of(
                date,
                LocalTime.of(
                    17,
                    0
                ),
                zone
            )

        val plan =
            AlarmSchedulingPlanner
                .plan(
                    task =
                        task(
                            date,
                            LocalTime.of(
                                20,
                                0
                            )
                        ),
                    minutesBefore =
                        60,
                    now =
                        now
                )

        requireNotNull(plan)

        assertFalse(
            plan.isCatchUp
        )

        assertEquals(
            ZonedDateTime.of(
                date,
                LocalTime.of(
                    19,
                    0
                ),
                zone
            )
                .toInstant()
                .toEpochMilli(),
            plan.triggerAtMillis
        )
    }

    @Test
    fun depoisDoEventoRecorrenciaDiariaVaiParaAmanha() {
        val date =
            LocalDate.of(
                2026,
                8,
                30
            )

        val now =
            ZonedDateTime.of(
                date,
                LocalTime.of(
                    20,
                    30
                ),
                zone
            )

        val plan =
            AlarmSchedulingPlanner
                .plan(
                    task =
                        task(
                            date =
                                date,
                            time =
                                LocalTime.of(
                                    20,
                                    0
                                ),
                            recurrence =
                                RecurrenceType
                                    .DAILY
                        ),
                    minutesBefore =
                        60,
                    now =
                        now
                )

        requireNotNull(plan)

        assertEquals(
            date.plusDays(1),
            plan.eventAt
                .toLocalDate()
        )

        assertEquals(
            LocalTime.of(
                20,
                0
            ),
            plan.eventAt
                .toLocalTime()
        )

        assertFalse(
            plan.isCatchUp
        )
    }

    @Test
    fun mensalDia31PulaMesSemDia31() {
        val start =
            LocalDate.of(
                2026,
                1,
                31
            )

        val now =
            ZonedDateTime.of(
                LocalDate.of(
                    2026,
                    4,
                    30
                ),
                LocalTime.of(
                    22,
                    0
                ),
                zone
            )

        val plan =
            AlarmSchedulingPlanner
                .plan(
                    task =
                        task(
                            date =
                                start,
                            time =
                                LocalTime.of(
                                    20,
                                    0
                                ),
                            recurrence =
                                RecurrenceType
                                    .MONTHLY,
                            monthDay =
                                31
                        ),
                    minutesBefore =
                        60,
                    now =
                        now
                )

        requireNotNull(plan)

        assertEquals(
            LocalDate.of(
                2026,
                5,
                31
            ),
            plan.eventAt
                .toLocalDate()
        )
    }

    @Test
    fun untilImpedeOcorrenciaPosterior() {
        val start =
            LocalDate.of(
                2026,
                8,
                30
            )

        val now =
            ZonedDateTime.of(
                LocalDate.of(
                    2026,
                    8,
                    31
                ),
                LocalTime.of(
                    21,
                    0
                ),
                zone
            )

        assertNull(
            AlarmSchedulingPlanner
                .plan(
                    task =
                        task(
                            date =
                                start,
                            time =
                                LocalTime.of(
                                    20,
                                    0
                                ),
                            recurrence =
                                RecurrenceType
                                    .DAILY,
                            until =
                                LocalDate.of(
                                    2026,
                                    8,
                                    31
                                )
                        ),
                    minutesBefore =
                        60,
                    now =
                        now
                )
        )
    }

    @Test
    fun desativadoNaoAgenda() {
        val date =
            LocalDate.of(
                2026,
                8,
                30
            )

        val now =
            ZonedDateTime.of(
                date,
                LocalTime.of(
                    10,
                    0
                ),
                zone
            )

        assertNull(
            AlarmSchedulingPlanner
                .plan(
                    task =
                        task(
                            date,
                            LocalTime.of(
                                20,
                                0
                            )
                        ),
                    minutesBefore =
                        -1,
                    now =
                        now
                )
        )
    }
}
