package com.anotai.app

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import com.anotai.app.alarm.TestAlarmReceiver
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

class TestAlertActivity :
    ComponentActivity() {

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var ringtone:
        Ringtone? =
        null

    private var toneGenerator:
        ToneGenerator? =
        null

    private var vibrator:
        Vibrator? =
        null

    private var stopped =
        false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        window.addFlags(
            WindowManager
                .LayoutParams
                .FLAG_KEEP_SCREEN_ON
        )

        startFeedback()

        handler.postDelayed(
            {
                stopAndFinish()
            },
            TEST_DURATION_MS
        )

        val audio =
            getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        val current =
            audio.getStreamVolume(
                AudioManager.STREAM_ALARM
            )

        val max =
            audio.getStreamMaxVolume(
                AudioManager.STREAM_ALARM
            )

        setContent {
            com.anotai.app.AnotaiTheme {
                Surface(
                    modifier =
                        Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        verticalArrangement =
                            Arrangement
                                .spacedBy(16.dp)
                    ) {
                        Text(
                            "Teste de alerta",
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineMedium
                        )

                        Text(
                            "O teste deve tocar e vibrar. Ele para automaticamente em até 15 segundos."
                        )

                        Text(
                            "Volume de alarme do aparelho: $current / $max"
                        )

                        Button(
                            onClick = {
                                stopAndFinish()
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Parar teste"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startFeedback() {
        val audioAttributes =
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

        val candidates =
            listOfNotNull(
                RingtoneManager
                    .getActualDefaultRingtoneUri(
                        this,
                        RingtoneManager
                            .TYPE_ALARM
                    ),
                RingtoneManager
                    .getDefaultUri(
                        RingtoneManager
                            .TYPE_ALARM
                    ),
                RingtoneManager
                    .getActualDefaultRingtoneUri(
                        this,
                        RingtoneManager
                            .TYPE_RINGTONE
                    ),
                RingtoneManager
                    .getDefaultUri(
                        RingtoneManager
                            .TYPE_RINGTONE
                    )
            )
                .distinct()

        for (uri in candidates) {
            val candidate =
                try {
                    RingtoneManager
                        .getRingtone(
                            this,
                            uri
                        )
                } catch (_: Throwable) {
                    null
                }

            if (candidate != null) {
                try {
                    candidate
                        .setAudioAttributes(
                            audioAttributes
                        )

                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.P
                    ) {
                        candidate.isLooping =
                            true

                        candidate.volume =
                            1.0f
                    }

                    candidate.play()

                    ringtone =
                        candidate

                    Log.i(
                        TAG,
                        "Som do teste iniciado: $uri"
                    )

                    break
                } catch (
                    error: Throwable
                ) {
                    Log.e(
                        TAG,
                        "Falha ao tocar $uri",
                        error
                    )
                }
            }
        }

        handler.postDelayed(
            {
                if (
                    !stopped &&
                    ringtone?.isPlaying !=
                    true
                ) {
                    startToneFallback()
                }
            },
            700L
        )

        vibrator =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {
                val manager =
                    getSystemService(
                        VibratorManager::class.java
                    )

                manager
                    ?.defaultVibrator
            } else {
                @Suppress(
                    "DEPRECATION"
                )
                getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as? Vibrator
            }

        val vibration =
            VibrationEffect.createWaveform(
                longArrayOf(
                    0L,
                    450L,
                    350L,
                    450L,
                    900L
                ),
                0
            )

        vibrator?.vibrate(
            vibration
        )
    }

    private fun startToneFallback() {
        try {
            toneGenerator =
                ToneGenerator(
                    AudioManager
                        .STREAM_ALARM,
                    100
                )

            repeatFallbackTone()

            Log.i(
                TAG,
                "Fallback sonoro ToneGenerator iniciado."
            )
        } catch (
            error: Throwable
        ) {
            Log.e(
                TAG,
                "Falha no fallback sonoro.",
                error
            )
        }
    }

    private fun repeatFallbackTone() {
        if (
            stopped
        ) {
            return
        }

        toneGenerator
            ?.startTone(
                ToneGenerator
                    .TONE_CDMA_ALERT_CALL_GUARD,
                900
            )

        handler.postDelayed(
            {
                repeatFallbackTone()
            },
            1_300L
        )
    }

    private fun stopAndFinish() {
        if (stopped) {
            return
        }

        stopped =
            true

        handler.removeCallbacksAndMessages(
            null
        )

        try {
            ringtone?.stop()
        } catch (_: Throwable) {
        }

        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (_: Throwable) {
        }

        try {
            vibrator?.cancel()
        } catch (_: Throwable) {
        }

        (
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
        ).cancel(
            TestAlarmReceiver
                .NOTIFICATION_ID
        )

        finish()
    }

    override fun onDestroy() {
        if (!stopped) {
            stopped =
                true

            handler.removeCallbacksAndMessages(
                null
            )

            try {
                ringtone?.stop()
            } catch (_: Throwable) {
            }

            try {
                toneGenerator?.stopTone()
                toneGenerator?.release()
            } catch (_: Throwable) {
            }

            try {
                vibrator?.cancel()
            } catch (_: Throwable) {
            }
        }

        super.onDestroy()
    }

    companion object {
        private const val TAG =
            "AnotaiTest"

        private const val
            TEST_DURATION_MS =
            15_000L
    }
}
