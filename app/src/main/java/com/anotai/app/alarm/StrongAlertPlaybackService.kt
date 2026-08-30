package com.anotai.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.anotai.app.data.TaskRepository

class StrongAlertPlaybackService :
    Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val taskId =
            intent?.getLongExtra(
                AlarmReceiver.EXTRA_TASK_ID,
                -1L
            ) ?: -1L

        val eventAtMillis =
            intent?.getLongExtra(
                AlarmReceiver.EXTRA_EVENT_AT_MILLIS,
                -1L
            ) ?: -1L

        val alertLeadMinutes =
            intent?.getIntExtra(
                AlarmReceiver.EXTRA_LEAD_MINUTES,
                -1
            ) ?: -1

        val task =
            if (taskId >= 0L) {
                TaskRepository(this).get(taskId)
            } else {
                null
            }

        val notificationId =
            visibleNotificationId(
                taskId,
                eventAtMillis,
                alertLeadMinutes
            )

        startForeground(
            notificationId,
            buildVisibleNotification(
                context = this,
                taskId = taskId,
                eventAtMillis = eventAtMillis,
                alertLeadMinutes = alertLeadMinutes,
                title = task?.title ?: "Alerta forte",
                originalText = task?.originalText ?: ""
            )
        )

        startSequence()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun startSequence() {
        handler.removeCallbacksAndMessages(null)
        stopPlayback()

        handler.postDelayed(
            { startVibration() },
            VISUAL_FIRST_DELAY_MILLIS
        )

        handler.postDelayed(
            {
                stopVibration()
                startSound()
            },
            VISUAL_FIRST_DELAY_MILLIS +
                VIBRATION_DURATION_MILLIS
        )

        handler.postDelayed(
            { stopSelfCompletely() },
            VISUAL_FIRST_DELAY_MILLIS +
                VIBRATION_DURATION_MILLIS +
                SOUND_DURATION_MILLIS
        )
    }

    private fun startVibration() {
        vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (
                    getSystemService(
                        Context.VIBRATOR_MANAGER_SERVICE
                    ) as VibratorManager
                ).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as Vibrator
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    VIBRATION_DURATION_MILLIS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VIBRATION_DURATION_MILLIS)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun startSound() {
        stopSound()

        val uri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_RINGTONE
            )

        if (uri != null) {
            runCatching {
                MediaPlayer().also { player ->
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                            )
                            .build()
                    )
                    player.setDataSource(this, uri)
                    player.isLooping = true
                    player.prepare()
                    player.start()
                }
            }.onSuccess { player ->
                mediaPlayer = player
                return
            }
        }

        toneGenerator =
            ToneGenerator(
                AudioManager.STREAM_ALARM,
                100
            ).also {
                it.startTone(
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    SOUND_DURATION_MILLIS.toInt()
                )
            }
    }

    private fun stopPlayback() {
        handler.removeCallbacksAndMessages(null)
        stopVibration()
        stopSound()
    }

    private fun stopSound() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

        runCatching { toneGenerator?.stopTone() }
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }

    private fun stopSelfCompletely() {
        stopPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                AlarmReceiver.ALERT_CHANNEL_ID,
                "Alertas fortes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Alerta visível e cancelável antes de vibrar e tocar."
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    companion object {
        const val VISUAL_FIRST_DELAY_MILLIS = 350L
        const val VIBRATION_DURATION_MILLIS = 15_000L
        const val SOUND_DURATION_MILLIS = 30_000L

        fun start(
            context: Context,
            taskId: Long,
            eventAtMillis: Long,
            alertLeadMinutes: Int
        ) {
            val intent =
                Intent(
                    context,
                    StrongAlertPlaybackService::class.java
                ).apply {
                    putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
                    putExtra(
                        AlarmReceiver.EXTRA_EVENT_AT_MILLIS,
                        eventAtMillis
                    )
                    putExtra(
                        AlarmReceiver.EXTRA_LEAD_MINUTES,
                        alertLeadMinutes
                    )
                }

            androidx.core.content.ContextCompat
                .startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(
                Intent(
                    context,
                    StrongAlertPlaybackService::class.java
                )
            )
        }

        fun visibleNotificationId(
            taskId: Long,
            eventAtMillis: Long,
            alertLeadMinutes: Int
        ): Int =
            stablePositiveId(
                "strong:$taskId:$eventAtMillis:$alertLeadMinutes"
            )

        fun buildVisibleNotification(
            context: Context,
            taskId: Long,
            eventAtMillis: Long,
            alertLeadMinutes: Int,
            title: String,
            originalText: String
        ): Notification {
            val notificationId =
                visibleNotificationId(
                    taskId,
                    eventAtMillis,
                    alertLeadMinutes
                )

            val fullScreenIntent =
                Intent(
                    context,
                    FullScreenAlertActivity::class.java
                ).apply {
                    putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
                    putExtra(
                        AlarmReceiver.EXTRA_EVENT_AT_MILLIS,
                        eventAtMillis
                    )
                    putExtra(
                        AlarmReceiver.EXTRA_LEAD_MINUTES,
                        alertLeadMinutes
                    )
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

            val fullScreenPi =
                PendingIntent.getActivity(
                    context,
                    stablePositiveId(
                        "full:$taskId:$eventAtMillis:$alertLeadMinutes"
                    ),
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            val stopIntent =
                Intent(
                    context,
                    AlertControlReceiver::class.java
                ).apply {
                    action =
                        AlertControlReceiver.ACTION_STOP_ACTIVE_ALERT
                    putExtra(
                        AlertControlReceiver.EXTRA_NOTIFICATION_ID,
                        notificationId
                    )
                }

            val stopPi =
                PendingIntent.getBroadcast(
                    context,
                    stablePositiveId(
                        "stop:$taskId:$eventAtMillis:$alertLeadMinutes"
                    ),
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

            return NotificationCompat.Builder(
                context,
                AlarmReceiver.ALERT_CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_lock_idle_alarm
                )
                .setContentTitle(
                    "Alerta • Anotaí"
                )
                .setContentText(
                    originalText.ifBlank {
                        title
                    }
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            originalText.ifBlank {
                                title
                            }
                        )
                )
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPi, true)
                .addAction(0, "PARAR ALERTA", stopPi)
                .build()
        }

        private fun leadLabel(minutes: Int): String =
            if (minutes % 60 == 0) {
                "${minutes / 60}h"
            } else {
                "${minutes}min"
            }

        private fun stablePositiveId(value: String): Int =
            value.hashCode().and(0x7fffffff)
    }
}
