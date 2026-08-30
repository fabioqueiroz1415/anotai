package com.anotai.app.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

class AlertControlReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val taskId =
            intent.getLongExtra(
                AlarmReceiver.EXTRA_TASK_ID,
                -1L
            )

        val eventAtMillis =
            intent.getLongExtra(
                AlarmReceiver.EXTRA_EVENT_AT_MILLIS,
                -1L
            )

        val alertLeadMinutes =
            intent.getIntExtra(
                AlarmReceiver.EXTRA_TARGET_ALERT_LEAD_MINUTES,
                -1
            )

        val notificationId =
            intent.getIntExtra(
                EXTRA_NOTIFICATION_ID,
                -1
            )

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        when (intent.action) {
            ACTION_CANCEL_ALERT -> {
                if (
                    taskId >= 0L &&
                    eventAtMillis > 0L &&
                    alertLeadMinutes >= 0
                ) {
                    AlarmScheduler(context)
                        .cancelSpecificAlert(
                            taskId,
                            eventAtMillis,
                            alertLeadMinutes,
                            ZonedDateTime.now()
                        )
                }

                if (notificationId >= 0) {
                    manager.cancel(notificationId)
                }
            }

            ACTION_DISMISS_NOTIFICATION -> {
                if (notificationId >= 0) {
                    manager.cancel(notificationId)
                }
            }

            ACTION_STOP_ACTIVE_ALERT -> {
                StrongAlertPlaybackService.stop(context)

                if (notificationId >= 0) {
                    manager.cancel(notificationId)
                }
            }
        }
    }

    companion object {
        const val ACTION_CANCEL_ALERT =
            "com.anotai.app.action.CANCEL_SPECIFIC_ALERT"

        const val ACTION_DISMISS_NOTIFICATION =
            "com.anotai.app.action.DISMISS_PRE_ALERT_NOTIFICATION"

        const val ACTION_STOP_ACTIVE_ALERT =
            "com.anotai.app.action.STOP_ACTIVE_STRONG_ALERT"

        const val EXTRA_NOTIFICATION_ID =
            "notification_id"
    }
}
