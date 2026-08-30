package com.anotai.app.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anotai.app.TestAlertActivity

class TestAlarmReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        Log.i(
            TAG,
            "Teste de 10 segundos disparou."
        )

        createChannel(
            context
        )

        val activityIntent =
            Intent(
                context,
                TestAlertActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }

        val fullScreenIntent =
            PendingIntent.getActivity(
                context,
                771001,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(
                Manifest.permission
                    .POST_NOTIFICATIONS
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            try {
                context.startActivity(
                    activityIntent
                )
            } catch (
                error: Throwable
            ) {
                Log.e(
                    TAG,
                    "Sem permissão de notificação e falha ao abrir teste.",
                    error
                )
            }

            return
        }

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable
                        .ic_lock_idle_alarm
                )
                .setContentTitle(
                    "Teste de alerta"
                )
                .setContentText(
                    "Som + vibração. O teste para sozinho em até 15 segundos."
                )
                .setCategory(
                    NotificationCompat
                        .CATEGORY_ALARM
                )
                .setPriority(
                    NotificationCompat
                        .PRIORITY_MAX
                )
                .setVisibility(
                    NotificationCompat
                        .VISIBILITY_PUBLIC
                )
                .setAutoCancel(true)
                .setContentIntent(
                    fullScreenIntent
                )
                .setFullScreenIntent(
                    fullScreenIntent,
                    true
                )
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                NOTIFICATION_ID,
                notification
            )

        // Quando o app está em primeiro plano, abre diretamente
        // para garantir que o teste também valide áudio/vibração.
        try {
            context.startActivity(
                activityIntent
            )
        } catch (
            error: Throwable
        ) {
            Log.w(
                TAG,
                "Abertura direta do teste bloqueada; notificação permanece.",
                error
            )
        }
    }

    private fun createChannel(
        context: Context
    ) {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val sound =
            RingtoneManager
                .getDefaultUri(
                    RingtoneManager
                        .TYPE_ALARM
                )

        val attributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes
                        .USAGE_ALARM
                )
                .setContentType(
                    AudioAttributes
                        .CONTENT_TYPE_SONIFICATION
                )
                .build()

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Teste de alerta",
                NotificationManager
                    .IMPORTANCE_HIGH
            ).apply {
                description =
                    "Canal exclusivo do teste de alerta."

                setSound(
                    sound,
                    attributes
                )

                enableVibration(true)

                vibrationPattern =
                    longArrayOf(
                        0L,
                        250L,
                        200L,
                        250L
                    )

                lockscreenVisibility =
                    android.app.Notification
                        .VISIBILITY_PUBLIC
            }

        manager.createNotificationChannel(
            channel
        )
    }

    companion object {
        const val NOTIFICATION_ID =
            771002

        const val CHANNEL_ID =
            "task_test_alert_v3"

        private const val TAG =
            "AnotaiTest"
    }
}
