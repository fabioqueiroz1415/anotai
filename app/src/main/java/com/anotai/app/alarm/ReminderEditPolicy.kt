package com.anotai.app.alarm

object ReminderEditPolicy {

    fun notificationPrimary(
        originalText: String
    ): Int =
        primary(
            ReminderLeadPolicy
                .notificationLeads(
                    originalText
                )
        )

    fun alertPrimary(
        originalText: String
    ): Int =
        primary(
            ReminderLeadPolicy
                .alertLeads(
                    originalText
                )
        )

    fun notificationSummary(
        originalText: String
    ): String =
        summary(
            ReminderLeadPolicy
                .notificationLeads(
                    originalText
                )
        )

    fun alertSummary(
        originalText: String
    ): String =
        summary(
            ReminderLeadPolicy
                .alertLeads(
                    originalText
                )
        )

    private fun primary(
        leads: List<Int>
    ): Int =
        leads
            .filter {
                it >= 0
            }
            .maxOrNull()
            ?: -1

    private fun summary(
        leads: List<Int>
    ): String {
        val normalized =
            leads
                .filter {
                    it >= 0
                }
                .distinct()
                .sortedDescending()

        if (normalized.isEmpty()) {
            return "Desativado"
        }

        return normalized
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
