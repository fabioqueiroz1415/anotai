package com.anotai.app.alarm

import com.anotai.app.data.TaskRecord
import java.time.ZonedDateTime

data class PlannedMultiLeadNotice(
    val eventAt: ZonedDateTime,
    val leadMinutes: Int,
    val triggerAtMillis: Long,
    val isCatchUp: Boolean = false
)

object MultiLeadAlarmPlanner {

    fun plan(
        task: TaskRecord,
        leads: List<Int>,
        now: ZonedDateTime
    ): PlannedMultiLeadNotice? {
        val eventAt =
            NextOccurrenceCalculator.nextEvent(task, now)
                ?: return null

        return planForEvent(eventAt, leads, now)
    }

    fun planForEvent(
        eventAt: ZonedDateTime,
        leads: List<Int>,
        now: ZonedDateTime
    ): PlannedMultiLeadNotice? {
        if (!eventAt.isAfter(now)) {
            return null
        }

        val chosenLead =
            leads
                .filter { it >= 0 }
                .distinct()
                .filter { lead ->
                    eventAt
                        .minusMinutes(lead.toLong())
                        .isAfter(now)
                }
                .maxOrNull()
                ?: return null

        val triggerAt =
            eventAt.minusMinutes(chosenLead.toLong())

        return PlannedMultiLeadNotice(
            eventAt = eventAt,
            leadMinutes = chosenLead,
            triggerAtMillis =
                triggerAt.toInstant().toEpochMilli(),
            isCatchUp = false
        )
    }

    fun planAfterFiring(
        task: TaskRecord,
        leads: List<Int>,
        firedEventAt: ZonedDateTime,
        firedLeadMinutes: Int,
        now: ZonedDateTime
    ): PlannedMultiLeadNotice? {
        if (firedEventAt.isAfter(now)) {
            planForEvent(
                eventAt = firedEventAt,
                leads = leads.filter { it < firedLeadMinutes },
                now = now
            )?.let {
                return it
            }
        }

        val nextSearch =
            maxOf(
                now,
                firedEventAt.plusSeconds(1)
            )

        val nextEvent =
            NextOccurrenceCalculator.nextEvent(task, nextSearch)
                ?: return null

        return planForEvent(nextEvent, leads, now)
    }
}
