package com.anotai.app.alarm

import com.anotai.app.data.TaskRecord
import java.time.ZonedDateTime

data class PlannedTaskNotice(
    val eventAt: ZonedDateTime,
    val triggerAtMillis: Long,
    val isCatchUp: Boolean
)

object AlarmSchedulingPlanner {

    fun plan(
        task: TaskRecord,
        minutesBefore: Int,
        now: ZonedDateTime
    ): PlannedTaskNotice? {
        if (task.time == null) {
            return null
        }

        if (minutesBefore < 0) {
            return null
        }

        val eventAt =
            NextOccurrenceCalculator
                .nextEvent(
                    task,
                    now
                ) ?: return null

        val triggerPlan =
            NoticeCatchUpPolicy.plan(
                eventAtMillis =
                    eventAt
                        .toInstant()
                        .toEpochMilli(),
                minutesBefore =
                    minutesBefore,
                nowMillis =
                    now
                        .toInstant()
                        .toEpochMilli()
            ) ?: return null

        return PlannedTaskNotice(
            eventAt =
                eventAt,
            triggerAtMillis =
                triggerPlan
                    .triggerAtMillis,
            isCatchUp =
                triggerPlan
                    .isCatchUp
        )
    }
}
