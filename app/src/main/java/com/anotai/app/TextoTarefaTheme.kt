package com.anotai.app

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AnotaiTheme(
    settings: ThemeSettings? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val resolvedSettings = settings ?: ThemePreferences(context).read()
    val darkTheme =
        when (resolvedSettings.mode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val colors =
        when {
            resolvedSettings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }

            darkTheme -> darkColorScheme()
            else -> lightColorScheme()
        }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
