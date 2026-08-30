package com.anotai.app.alarm

import com.anotai.app.model.RecurrenceType
import com.anotai.app.model.TaskDraft
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderEffectiveLeadSummaryTest {

    private val zone =
        ZoneId.of(
            "America/Sao_Paulo"
        )

    private val now =
        ZonedDateTime.of(
            2026,
            8,
            30,
            16,
            19,
            0,
            0,
            zone
        )

    @Test
    fun passedLeadsAreOmittedFromConfirmation() {
        val draft =
            draft(
                text =
                    "buscar filha 17h",
                date =
                    LocalDate.of(
                        2026,
                        8,
                        30
                    ),
                time =
                    LocalTime.of(
                        17,
                        0
                    )
            )

        assertEquals(
            "30min 15min 5min 0min antes",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.NOTIFICATION,
                    now
                )
        )

        assertEquals(
            "0min antes",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.ALERT,
                    now
                )
        )

        assertEquals(
            "30min antes",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.PRE_ALERT,
                    now
                )
        )
    }

    @Test
    fun tomorrowKeepsWholeDefaultPolicy() {
        val draft =
            draft(
                text =
                    "buscar filha amanhã 17h",
                date =
                    LocalDate.of(
                        2026,
                        8,
                        31
                    ),
                time =
                    LocalTime.of(
                        17,
                        0
                    )
            )

        assertEquals(
            "2h 30min 15min 5min 0min antes",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.NOTIFICATION,
                    now
                )
        )

        assertEquals(
            "1h 0min antes",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.ALERT,
                    now
                )
        )

        assertEquals(
            "90min 30min antes",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.PRE_ALERT,
                    now
                )
        )
    }

    @Test
    fun exclusiveAlertDisablesNotificationInConfirmation() {
        val draft =
            draft(
                text =
                    "buscar filha amanhã 17h só alertar 2h antes",
                date =
                    LocalDate.of(
                        2026,
                        8,
                        31
                    ),
                time =
                    LocalTime.of(
                        17,
                        0
                    )
            )

        assertEquals(
            "Desativado",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.NOTIFICATION,
                    now
                )
        )

        assertEquals(
            "2h antes",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.ALERT,
                    now
                )
        )

        assertEquals(
            "150min antes",
            ReminderEffectiveLeadSummary
                .forDraft(
                    draft,
                    EffectiveLeadKind.PRE_ALERT,
                    now
                )
        )
    }

    private fun draft(
        text: String,
        date: LocalDate,
        time: LocalTime
    ): TaskDraft =
        TaskDraft(
            title = "Teste",
            originalText = text,
            date = date,
            time = time,
            reminderMinutesBefore = 60,
            notificationMinutesBefore = 120,
            recurrenceType = RecurrenceType.NONE
        )
    @Test
    fun draftAndStoredTaskUseSameEffectiveCalculation() {
        val draft =
            draft(
                text =
                    "buscar filha amanhã 17h",
                date =
                    LocalDate.of(
                        2026,
                        8,
                        31
                    ),
                time =
                    LocalTime.of(
                        17,
                        0
                    )
            )

        val task =
            com.anotai.app.data.TaskRecord(
                id = 1L,
                title = draft.title,
                originalText = draft.originalText,
                date = draft.date,
                time = draft.time,
                reminderMinutesBefore =
                    draft.reminderMinutesBefore,
                recurrenceType =
                    draft.recurrenceType,
                weekdaysCsv = "",
                monthDay =
                    draft.monthDay,
                untilDate =
                    draft.untilDate,
                notificationMinutesBefore =
                    draft.notificationMinutesBefore
            )

        EffectiveLeadKind
            .values()
            .forEach {
                kind ->

                assertEquals(
                    ReminderEffectiveLeadSummary
                        .forDraft(
                            draft,
                            kind,
                            now
                        ),
                    ReminderEffectiveLeadSummary
                        .forTask(
                            task,
                            kind,
                            now
                        )
                )
            }
    }

}
