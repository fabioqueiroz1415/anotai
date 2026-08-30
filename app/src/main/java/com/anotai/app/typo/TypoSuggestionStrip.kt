package com.anotai.app.typo

import android.os.Handler
import android.os.Looper
import android.view.textservice.SpellCheckerSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberTypoSuggestions(
    text: String
): State<List<TypoSuggestion>> {
    val context = LocalContext.current
    val state =
        remember(text) {
            mutableStateOf(
                emptyList<TypoSuggestion>()
            )
        }

    DisposableEffect(text) {
        val handler =
            Handler(
                Looper.getMainLooper()
            )

        var active = true
        var session:
            SpellCheckerSession? = null

        val runnable =
            Runnable {
                if (text.length < 4) {
                    state.value =
                        emptyList()
                    return@Runnable
                }

                session =
                    TypoSuggestionEngine
                        .request(
                            context,
                            text
                        ) { found ->
                            if (active) {
                                state.value =
                                    found
                            }
                        }
            }

        handler.postDelayed(
            runnable,
            300L
        )

        onDispose {
            active = false

            handler.removeCallbacks(
                runnable
            )

            try {
                session?.cancel()
                session?.close()
            } catch (_: Throwable) {
            }
        }
    }

    return state
}
