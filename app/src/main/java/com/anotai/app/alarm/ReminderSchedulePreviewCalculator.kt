package com.anotai.app.alarm

import java.time.LocalDateTime

enum class PreviewNoticeKind {
    NOTIFICATION,
    PRE_ALERT,
    ALERT
}

data class PreviewNotice(
    val kind: PreviewNoticeKind,
    val triggerAt: LocalDateTime,
    val leadMinutes: Int,
    val targetAlertLeadMinutes: Int? = null
)

object ReminderSchedulePreviewCalculator {

    fun calculate(
        originalText: String,
        eventAt: LocalDateTime,
        now: LocalDateTime
    ): List<PreviewNotice> {
        if (!eventAt.isAfter(now)) {
            return emptyList()
        }

        val normal =
            ReminderLeadPolicy
                .notificationLeads(originalText)
                .mapNotNull { lead ->
                    val trigger =
                        eventAt.minusMinutes(lead.toLong())

                    if (trigger.isAfter(now)) {
                        PreviewNotice(
                            kind = PreviewNoticeKind.NOTIFICATION,
                            triggerAt = trigger,
                            leadMinutes = lead
                        )
                    } else {
                        null
                    }
                }

        val alerts =
            ReminderLeadPolicy
                .alertLeads(originalText)
                .mapNotNull { lead ->
                    val trigger =
                        eventAt.minusMinutes(lead.toLong())

                    if (trigger.isAfter(now)) {
                        PreviewNotice(
                            kind = PreviewNoticeKind.ALERT,
                            triggerAt = trigger,
                            leadMinutes = lead
                        )
                    } else {
                        null
                    }
                }

        val preAlerts =
            ReminderLeadPolicy
                .preAlertSpecs(originalText)
                .mapNotNull { spec ->
                    val trigger =
                        eventAt.minusMinutes(
                            spec.notificationLeadMinutes.toLong()
                        )

                    if (trigger.isAfter(now)) {
                        PreviewNotice(
                            kind = PreviewNoticeKind.PRE_ALERT,
                            triggerAt = trigger,
                            leadMinutes =
                                spec.notificationLeadMinutes,
                            targetAlertLeadMinutes =
                                spec.alertLeadMinutes
                        )
                    } else {
                        null
                    }
                }

        return (normal + preAlerts + alerts)
            .sortedWith(
                compareBy<PreviewNotice> {
                    it.triggerAt
                }.thenBy {
                    it.kind.ordinal
                }
            )
    }
}
