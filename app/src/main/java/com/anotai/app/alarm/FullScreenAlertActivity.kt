package com.anotai.app.alarm

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anotai.app.data.TaskRepository

class FullScreenAlertActivity :
    ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

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
                AlarmReceiver.EXTRA_LEAD_MINUTES,
                -1
            )

        val task =
            if (taskId >= 0L) {
                TaskRepository(this).get(taskId)
            } else {
                null
            }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(28.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            "ALERTA FORTE",
                            style =
                                MaterialTheme.typography.headlineLarge
                        )

                        Text(
                            task?.title ?: "Tarefa",
                            style =
                                MaterialTheme.typography.headlineMedium
                        )

                        task?.originalText
                            ?.takeIf { it.isNotBlank() }
                            ?.let {
                                Text(
                                    it,
                                    style =
                                        MaterialTheme.typography.bodyLarge
                                )
                            }

                        Text(
                            "15 segundos vibrando e depois 30 segundos tocando.",
                            style =
                                MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = {
                                StrongAlertPlaybackService.stop(
                                    this@FullScreenAlertActivity
                                )

                                val id =
                                    StrongAlertPlaybackService
                                        .visibleNotificationId(
                                            taskId,
                                            eventAtMillis,
                                            alertLeadMinutes
                                        )

                                (
                                    getSystemService(
                                        NOTIFICATION_SERVICE
                                    ) as NotificationManager
                                ).cancel(id)

                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("PARAR ALERTA")
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TEST_MODE =
            "full_screen_test_mode"
    }
}
