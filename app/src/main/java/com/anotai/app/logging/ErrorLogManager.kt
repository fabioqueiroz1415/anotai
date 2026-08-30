package com.anotai.app.logging

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.anotai.app.BuildConfig
import com.anotai.app.model.TaskDraft
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ErrorLogManager(private val context: Context) {

    private val logFile: File
        get() = File(context.filesDir, FILE_NAME)

    fun ensureExists() {
        if (!logFile.exists()) {
            logFile.writeText(
                buildString {
                    appendLine("ERROS DE INTERPRETAÇÃO - ANOTAÍ")
                    appendLine("Este arquivo registra somente interpretações incompletas, ambíguas ou com falha.")
                    appendLine("Criado em: ${timestamp()}")
                    appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Android: ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
                    appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("============================================================")
                    appendLine()
                }
            )
        }
    }

    fun recordInterpretationProblem(draft: TaskDraft) {
        if (draft.warnings.isEmpty()) return
        ensureExists()

        logFile.appendText(
            buildString {
                appendLine("DATA/HORA: ${timestamp()}")
                appendLine("TIPO: INTERPRETAÇÃO INCOMPLETA OU AMBÍGUA")
                appendLine("TEXTO ORIGINAL:")
                appendLine(draft.originalText)
                appendLine()
                appendLine("O APP INTERPRETOU:")
                appendLine("Tarefa: ${draft.title}")
                appendLine("Data: ${draft.dateLabel()}")
                appendLine("Horário: ${draft.timeLabel()}")
                appendLine("Alerta: ${draft.reminderLabel()}")
                appendLine("Repetição: ${draft.recurrenceLabel()}")
                appendLine("Até: ${draft.untilLabel()}")
                appendLine()
                appendLine("PROBLEMAS / INCERTEZAS:")
                draft.warnings.forEach { appendLine("- $it") }
                appendLine()
                appendLine("AMBIENTE:")
                appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Android: ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
                appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("============================================================")
                appendLine()
            }
        )
    }

    fun recordException(originalText: String, throwable: Throwable) {
        ensureExists()
        logFile.appendText(
            buildString {
                appendLine("DATA/HORA: ${timestamp()}")
                appendLine("TIPO: ERRO DURANTE INTERPRETAÇÃO")
                appendLine("TEXTO ORIGINAL:")
                appendLine(originalText)
                appendLine()
                appendLine("ERRO:")
                appendLine("${throwable::class.java.name}: ${throwable.message ?: "sem mensagem"}")
                appendLine()
                appendLine("STACK TRACE:")
                appendLine(throwable.stackTraceToString())
                appendLine("============================================================")
                appendLine()
            }
        )
    }

    fun share() {
        ensureExists()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            logFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, FILE_NAME)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Exportar erros")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun timestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))

    companion object {
        const val FILE_NAME = "erros-log.txt"
    }
}
