package com.anotai.app.alarm

data class NoticeTriggerPlan(
    val triggerAtMillis: Long,
    val isCatchUp: Boolean
)

object NoticeCatchUpPolicy {

    const val DEFAULT_CATCH_UP_DELAY_MILLIS =
        3_000L

    fun plan(
        eventAtMillis: Long,
        minutesBefore: Int,
        nowMillis: Long,
        catchUpDelayMillis: Long =
            DEFAULT_CATCH_UP_DELAY_MILLIS
    ): NoticeTriggerPlan? {
        if (minutesBefore < 0) {
            return null
        }

        if (eventAtMillis <= nowMillis) {
            return null
        }

        val leadMillis =
            minutesBefore
                .toLong()
                .coerceAtLeast(0L)
                .times(60_000L)

        val requestedTrigger =
            eventAtMillis -
                leadMillis

        if (requestedTrigger > nowMillis) {
            return NoticeTriggerPlan(
                triggerAtMillis =
                    requestedTrigger,
                isCatchUp =
                    false
            )
        }

        val safeDelay =
            catchUpDelayMillis
                .coerceAtLeast(0L)

        val catchUpAt =
            (nowMillis + safeDelay)
                .coerceAtMost(
                    eventAtMillis - 1L
                )

        if (catchUpAt <= nowMillis) {
            return NoticeTriggerPlan(
                triggerAtMillis =
                    nowMillis,
                isCatchUp =
                    true
            )
        }

        return NoticeTriggerPlan(
            triggerAtMillis =
                catchUpAt,
            isCatchUp =
                true
        )
    }
}
