package com.anotai.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AppVersionFooter() {
    Text(
        text =
            "versão ${BuildConfig.VERSION_NAME}",
        style =
            MaterialTheme
                .typography
                .labelSmall,
        color =
            MaterialTheme
                .colorScheme
                .onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 18.dp,
                    bottom = 4.dp
                ),
        textAlign =
            TextAlign.Center
    )
}
