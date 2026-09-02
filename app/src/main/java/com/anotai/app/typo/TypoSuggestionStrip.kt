package com.anotai.app.typo

import android.view.textservice.SpellCheckerSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.anotai.app.AnotaiDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberTypoSuggestions(
    text: String
): State<List<TypoSuggestion>> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state =
        remember(text) {
            mutableStateOf(
                emptyList<TypoSuggestion>()
            )
        }

    DisposableEffect(text, context) {
        var active = true
        var session:
            SpellCheckerSession? = null

        val job =
            scope.launch {
                delay(300L)

                if (
                    !active ||
                    text.length < 4
                ) {
                    if (active) {
                        state.value =
                            emptyList()
                    }

                    return@launch
                }

                // O corretor também não usa a Main Thread nem a thread do
                // parser. Ele possui sua própria fila "AnotaiTypo".
                val local =
                    withContext(
                        AnotaiDispatchers.typo
                    ) {
                        TypoSuggestionEngine
                            .computeLocalSuggestions(
                                text
                            )
                    }

                if (!active) {
                    return@launch
                }

                session =
                    TypoSuggestionEngine
                        .request(
                            context = context,
                            text = text,
                            precomputedLocal =
                                local
                        ) { found ->
                            if (active) {
                                state.value =
                                    found
                            }
                        }
            }

        onDispose {
            active = false
            job.cancel()

            try {
                session?.cancel()
                session?.close()
            } catch (_: Throwable) {
            }
        }
    }

    return state
}
