package com.anotai.app.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.anotai.app.MainActivity
import com.anotai.app.data.TaskRepository
import java.time.ZonedDateTime

class AlarmReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val taskId =
            intent.getLongExtra(EXTRA_TASK_ID, -1L)

        if (taskId < 0L) {
            return
        }

        val kind =
            intent.getStringExtra(EXTRA_KIND) ?: KIND_ALERT

        val eventAtMillis =
            intent.getLongExtra(EXTRA_EVENT_AT_MILLIS, -1L)

        val firedLeadMinutes =
            intent.getIntExtra(EXTRA_LEAD_MINUTES, -1)

        val targetAlertLeadMinutes =
            intent.getIntExtra(
                EXTRA_TARGET_ALERT_LEAD_MINUTES,
                -1
            )

        val task =
            TaskRepository(context).get(taskId) ?: return

        createChannels(context)

        val notificationAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        when (kind) {
            KIND_NOTIFICATION -> {
                if (notificationAllowed) {
                    manager.notify(
                        normalNotificationId(
                            taskId,
                            eventAtMillis,
                            firedLeadMinutes
                        ),
                        buildNormalNotification(
                            context,
                            taskId,
                            task.title,
                            task.originalText,
                            firedLeadMinutes
                        )
                    )
                }
            }

            KIND_PRE_ALERT -> {
                if (
                    notificationAllowed &&
                    targetAlertLeadMinutes >= 0
                ) {
                    val id =
                        preAlertNotificationId(
                            taskId,
                            eventAtMillis,
                            targetAlertLeadMinutes
                        )

                    manager.notify(
                        id,
                        buildPreAlertNotification(
                            context,
                            taskId,
                            eventAtMillis,
                            targetAlertLeadMinutes,
                            id,
                            task.title,
                            originalText = task.originalText
                        )
                    )
                }
            }

            KIND_ALERT -> {
                if (notificationAllowed) {
                    val id =
                        StrongAlertPlaybackService
                            .visibleNotificationId(
                                taskId,
                                eventAtMillis,
                                firedLeadMinutes
                            )

                    val visibleNotification =
                        StrongAlertPlaybackService
                            .buildVisibleNotification(
                                context = context,
                                taskId = taskId,
                                eventAtMillis = eventAtMillis,
                                alertLeadMinutes = firedLeadMinutes,
                                title = task.title,
                                originalText = task.originalText
                            )

                    // Primeiro existe interface cancelável.
                    manager.notify(id, visibleNotification)

                    // Só depois pode vibrar/tocar.
                    runCatching {
                        StrongAlertPlaybackService.start(
                            context,
                            taskId,
                            eventAtMillis,
                            firedLeadMinutes
                        )
                    }.onFailure { error ->
                        Log.e(
                            "AnotaiAlarm",
                            "Falha ao iniciar serviço do alerta forte.",
                            error
                        )
                    }
                } else {
                    Log.w(
                        "AnotaiAlarm",
                        "Alerta forte não tocou: notificações bloqueadas e não haveria interface cancelável."
                    )
                }
            }
        }

        AlarmScheduler(context).scheduleAfterFiring(
            taskId = taskId,
            kind = kind,
            eventAtMillis = eventAtMillis,
            firedLeadMinutes = firedLeadMinutes,
            now = ZonedDateTime.now()
        )
    }

    private fun buildNormalNotification(
        context: Context,
        taskId: Long,
        title: String,
        originalText: String,
        leadMinutes: Int
    ): Notification {
        val openIntent =
            PendingIntent.getActivity(
                context,
                stablePositiveId(
                    "open-normal:$taskId:$leadMinutes"
                ),
                Intent(
                    context,
                    MainActivity::class.java
                ).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(
            context,
            NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_lock_idle_alarm
            )
            .setContentTitle(
                if (
                    leadMinutes ==
                        0
                ) {
                    "Anotaí"
                } else {
                    "Anotaí • ${leadLabel(leadMinutes)} antes"
                }
            )
            .setContentText(
                originalText
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(originalText)
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setVibrate(
                longArrayOf(0L, 450L, 180L, 450L)
            )
            .setContentIntent(openIntent)
            .build()
    }

    private fun buildPreAlertNotification(
        context: Context,
        taskId: Long,
        eventAtMillis: Long,
        targetAlertLeadMinutes: Int,
        notificationId: Int,
        title: String,
        originalText: String
    ): Notification {
        val cancelIntent =
            Intent(
                context,
                AlertControlReceiver::class.java
            ).apply {
                action = AlertControlReceiver.ACTION_CANCEL_ALERT
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_EVENT_AT_MILLIS, eventAtMillis)
                putExtra(
                    EXTRA_TARGET_ALERT_LEAD_MINUTES,
                    targetAlertLeadMinutes
                )
                putExtra(
                    AlertControlReceiver.EXTRA_NOTIFICATION_ID,
                    notificationId
                )
            }

        val cancelPi =
            PendingIntent.getBroadcast(
                context,
                stablePositiveId(
                    "cancel-alert:$taskId:$eventAtMillis:$targetAlertLeadMinutes"
                ),
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val dismissIntent =
            Intent(
                context,
                AlertControlReceiver::class.java
            ).apply {
                action =
                    AlertControlReceiver.ACTION_DISMISS_NOTIFICATION
                putExtra(
                    AlertControlReceiver.EXTRA_NOTIFICATION_ID,
                    notificationId
                )
            }

        val dismissPi =
            PendingIntent.getBroadcast(
                context,
                stablePositiveId(
                    "dismiss-pre:$taskId:$eventAtMillis:$targetAlertLeadMinutes"
                ),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(
            context,
            PRE_ALERT_CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_lock_idle_alarm
            )
            .setContentTitle("Alerta em 30 minutos")
            .setContentText(
                originalText
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setVibrate(
                longArrayOf(0L, 450L, 180L, 450L)
            )
            .addAction(0, "CANCELAR ALERTA", cancelPi)
            .addAction(0, "DISPENSAR", dismissPi)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(originalText)
            )
            .build()
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val notificationAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()

        val normal =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Notificações de tarefas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Notificações antecipadas e no horário da tarefa."
                enableVibration(true)
                vibrationPattern =
                    longArrayOf(0L, 450L, 180L, 450L)
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    notificationAttributes
                )
            }

        val preAlert =
            NotificationChannel(
                PRE_ALERT_CHANNEL_ID,
                "Avisos antes de alertas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Avisa 30 minutos antes de cada alerta e permite cancelá-lo."
                enableVibration(true)
                vibrationPattern =
                    longArrayOf(0L, 450L, 180L, 450L)
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    notificationAttributes
                )
            }

        val strong =
            NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas fortes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Alerta visível e cancelável antes de vibrar e tocar."
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

        manager.createNotificationChannel(normal)
        manager.createNotificationChannel(preAlert)
        manager.createNotificationChannel(strong)
    }

    private fun leadLabel(minutes: Int): String =
        if (minutes % 60 == 0) {
            "${minutes / 60}h"
        } else {
            "${minutes}min"
        }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_KIND = "alarm_kind"
        const val EXTRA_EVENT_AT_MILLIS = "event_at_millis"
        const val EXTRA_LEAD_MINUTES = "lead_minutes"
        const val EXTRA_TARGET_ALERT_LEAD_MINUTES =
            "target_alert_lead_minutes"

        const val KIND_NOTIFICATION = "notification"
        const val KIND_PRE_ALERT = "pre_alert"
        const val KIND_ALERT = "alert"

        const val NOTIFICATION_CHANNEL_ID =
            "task_notification_v4"
        const val PRE_ALERT_CHANNEL_ID =
            "task_pre_alert_v1"
        const val ALERT_CHANNEL_ID =
            "task_alert_v8"

        const val CHANNEL_ID = "task_alert_v5"

        fun normalNotificationId(
            taskId: Long,
            eventAtMillis: Long,
            leadMinutes: Int
        ): Int =
            stablePositiveId(
                "normal:\$taskId:\$eventAtMillis:\$leadMinutes"
            )

        fun preAlertNotificationId(
            taskId: Long,
            eventAtMillis: Long,
            alertLeadMinutes: Int
        ): Int =
            stablePositiveId(
                "pre:\$taskId:\$eventAtMillis:\$alertLeadMinutes"
            )

        private fun stablePositiveId(value: String): Int =
            value.hashCode().and(0x7fffffff)
    }
}
