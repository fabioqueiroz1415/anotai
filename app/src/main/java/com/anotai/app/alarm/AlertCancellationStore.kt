package com.anotai.app.alarm

import android.content.Context

class AlertCancellationStore(
    context: Context
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun cancel(
        taskId: Long,
        eventAtMillis: Long,
        alertLeadMinutes: Int
    ) {
        val current =
            prefs
                .getStringSet(KEY_CANCELED, emptySet())
                .orEmpty()
                .toMutableSet()

        current += key(taskId, eventAtMillis, alertLeadMinutes)

        prefs.edit()
            .putStringSet(KEY_CANCELED, current)
            .apply()
    }

    fun isCanceled(
        taskId: Long,
        eventAtMillis: Long,
        alertLeadMinutes: Int
    ): Boolean =
        prefs
            .getStringSet(KEY_CANCELED, emptySet())
            .orEmpty()
            .contains(
                key(taskId, eventAtMillis, alertLeadMinutes)
            )

    fun clearTask(taskId: Long) {
        val prefix = "$taskId:"

        val remaining =
            prefs
                .getStringSet(KEY_CANCELED, emptySet())
                .orEmpty()
                .filterNot { it.startsWith(prefix) }
                .toSet()

        prefs.edit()
            .putStringSet(KEY_CANCELED, remaining)
            .apply()
    }

    private fun key(
        taskId: Long,
        eventAtMillis: Long,
        alertLeadMinutes: Int
    ): String =
        "$taskId:$eventAtMillis:$alertLeadMinutes"

    companion object {
        private const val PREFS_NAME =
            "alert_cancellation_store"

        private const val KEY_CANCELED =
            "canceled_alerts"
    }
}
