package com.anotai.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AnotaiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}
