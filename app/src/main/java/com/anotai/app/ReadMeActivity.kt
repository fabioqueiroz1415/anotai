package com.anotai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ReadMeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content =
            runCatching {
                assets
                    .open("LEIA-ME.md")
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrElse {
                "Não foi possível abrir o LEIA-ME."
            }

        setContent {
            com.anotai.app.AnotaiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ReadMeContent(
                        content = content,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadMeContent(
    content: String,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "LEIA-ME",
                style =
                    MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Text(
                content,
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }

        item {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("VOLTAR")
            }
        }
    }
}
