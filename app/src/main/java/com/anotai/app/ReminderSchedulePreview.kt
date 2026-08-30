package com.anotai.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.anotai.app.alarm.PreviewNotice
import com.anotai.app.alarm.PreviewNoticeKind
import com.anotai.app.alarm.ReminderSchedulePreviewCalculator
import com.anotai.app.model.TaskDraft
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ReminderSchedulePreview(
    draft: TaskDraft
) {
    val date = draft.date ?: return
    val time = draft.time ?: return

    val eventAt = date.atTime(time)
    val now = LocalDateTime.now()

    val items =
        ReminderSchedulePreviewCalculator.calculate(
            originalText = draft.originalText,
            eventAt = eventAt,
            now = now
        )

    val notifications =
        items.filter {
            it.kind != PreviewNoticeKind.ALERT
        }

    val alerts =
        items.filter {
            it.kind == PreviewNoticeKind.ALERT
        }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "HORÁRIOS QUE SERÃO AGENDADOS",
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            "Horários que já passaram são ignorados.",
            style = MaterialTheme.typography.labelMedium
        )

        Text(
            "Notificações",
            style = MaterialTheme.typography.titleSmall
        )

        if (notifications.isEmpty()) {
            Text(
                "Nenhuma notificação ainda pode ser agendada.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            notifications.forEach { notice ->
                Text(
                    notificationLine(notice),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            "Alertas",
            style = MaterialTheme.typography.titleSmall
        )

        if (alerts.isEmpty()) {
            Text(
                "Nenhum alerta ainda pode ser agendado.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            alerts.forEach { notice ->
                Text(
                    "${clock(notice.triggerAt)} — ${leadLabel(notice.leadMinutes)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun notificationLine(notice: PreviewNotice): String =
    when (notice.kind) {
        PreviewNoticeKind.NOTIFICATION ->
            "${clock(notice.triggerAt)} — ${leadLabel(notice.leadMinutes)}"

        PreviewNoticeKind.PRE_ALERT -> {
            val alertLead = notice.targetAlertLeadMinutes ?: 0
            val alertTime = notice.triggerAt.plusMinutes(30)

            "${clock(notice.triggerAt)} — aviso: alerta às ${clock(alertTime)} (${leadLabel(alertLead)})"
        }

        PreviewNoticeKind.ALERT -> ""
    }

private fun leadLabel(minutes: Int): String =
    when {
        minutes == 0 -> "no momento da tarefa"
        minutes % 60 == 0 -> "${minutes / 60}h antes"
        else -> "${minutes}min antes"
    }

private fun clock(value: LocalDateTime): String =
    value.format(
        DateTimeFormatter.ofPattern("HH:mm")
    )
