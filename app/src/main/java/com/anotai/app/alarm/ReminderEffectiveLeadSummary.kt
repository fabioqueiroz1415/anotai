package com.anotai.app.alarm

import com.anotai.app.data.TaskRecord
import com.anotai.app.model.TaskDraft
import java.time.ZonedDateTime

enum class EffectiveLeadKind {
    NOTIFICATION,
    ALERT,
    PRE_ALERT
}

object ReminderEffectiveLeadSummary {

    fun forDraft(
        draft: TaskDraft,
        kind: EffectiveLeadKind,
        now: ZonedDateTime =
            ZonedDateTime.now()
    ): String =
        forTask(
            task =
                TaskRecord(
                    id = -1L,
                    title = draft.title,
                    originalText = draft.originalText,
                    date = draft.date,
                    time = draft.time,
                    reminderMinutesBefore =
                        draft.reminderMinutesBefore,
                    recurrenceType =
                        draft.recurrenceType,
                    weekdaysCsv =
                        draft.weekdays
                            .sortedBy {
                                it.value
                            }
                            .joinToString(
                                separator = ","
                            ) {
                                it.value
                                    .toString()
                            },
                    monthDay =
                        draft.monthDay,
                    untilDate =
                        draft.untilDate,
                    notificationMinutesBefore =
                        draft.notificationMinutesBefore
                ),
            kind = kind,
            now = now
        )

    fun forTask(
        task: TaskRecord,
        kind: EffectiveLeadKind,
        now: ZonedDateTime =
            ZonedDateTime.now()
    ): String {
        if (
            task.date == null ||
            task.time == null
        ) {
            return "Sem horário"
        }

        val leads =
            when (kind) {
                EffectiveLeadKind.NOTIFICATION ->
                    ReminderLeadPolicy
                        .notificationLeads(
                            task.originalText
                        )

                EffectiveLeadKind.ALERT ->
                    ReminderLeadPolicy
                        .alertLeads(
                            task.originalText
                        )

                EffectiveLeadKind.PRE_ALERT ->
                    ReminderLeadPolicy
                        .preAlertSpecs(
                            task.originalText
                        )
                        .map {
                            it.notificationLeadMinutes
                        }
            }

        if (leads.isEmpty()) {
            return "Desativado"
        }

        val eventAt =
            NextOccurrenceCalculator
                .nextEvent(
                    task,
                    now
                )
                ?: return "Nenhum horário futuro"

        val possible =
            leads
                .filter {
                    it >= 0
                }
                .distinct()
                .filter {
                    lead ->

                    eventAt
                        .minusMinutes(
                            lead.toLong()
                        )
                        .isAfter(now)
                }
                .sortedDescending()

        if (possible.isEmpty()) {
            return "Nenhum horário futuro"
        }

        return possible
            .joinToString(
                separator = " "
            ) {
                leadToken(it)
            } +
            " antes"
    }

    private fun leadToken(
        minutes: Int
    ): String =
        when {
            minutes == 0 ->
                "0min"

            minutes % 60 == 0 ->
                "${minutes / 60}h"

            else ->
                "${minutes}min"
        }
}
