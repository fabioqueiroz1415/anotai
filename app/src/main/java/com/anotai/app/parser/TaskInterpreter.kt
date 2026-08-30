package com.anotai.app.parser

import com.anotai.app.model.RecurrenceType
import com.anotai.app.model.TaskDraft
import java.time.DayOfWeek
import java.time.LocalDateTime

class TaskInterpreter(
    private val base:
        TaskTextParser = TaskTextParser()
) {

    fun parse(
        text: String
    ): TaskDraft =
        parse(
            text,
            LocalDateTime.now()
        )

    fun parse(
        text: String,
        now: LocalDateTime
    ): TaskDraft {
        val normalized =
            normalizeStable(text)

        val onlyAlert =
            ONLY_ALERT_PATTERN
                .containsMatchIn(normalized)

        val onlyNotification =
            ONLY_NOTIFICATION_PATTERN
                .containsMatchIn(normalized)

        val disableAlert =
            DISABLE_ALERT_PATTERN
                .containsMatchIn(normalized)

        val disableNotification =
            DISABLE_NOTIFICATION_PATTERN
                .containsMatchIn(normalized)

        val notificationMatch =
            NOTIFICATION_PATTERN
                .find(normalized)

        val notificationIntent =
            NOTIFICATION_INTENT
                .containsMatchIn(normalized)

        val explicitNotification =
            notificationMatch
                ?.let {
                    parseMinutes(
                        amount =
                            it.groupValues[1],
                        unit =
                            it.groupValues[2]
                    )
                }

        var cleaned =
            text

        listOf(
            ONLY_ALERT_PATTERN,
            ONLY_NOTIFICATION_PATTERN,
            DISABLE_ALERT_PATTERN,
            DISABLE_NOTIFICATION_PATTERN,
            NOTIFICATION_PATTERN
        ).forEach { regex ->
            cleaned =
                replaceMatchesWithSpaces(
                    cleaned,
                    regex
                )
        }

        val parsed =
            base.parse(
                cleaned,
                now
            )

        val warnings =
            parsed.warnings
                .toMutableList()

        var notificationMinutes =
            explicitNotification
                ?: parsed.notificationMinutesBefore

        var alertMinutes =
            parsed.reminderMinutesBefore

        if (disableNotification) {
            notificationMinutes =
                DISABLED
        }

        if (disableAlert) {
            alertMinutes =
                DISABLED
        }

        if (onlyAlert) {
            notificationMinutes =
                DISABLED

            if (alertMinutes < 0) {
                alertMinutes =
                    DEFAULT_ALERT_MINUTES
            }
        }

        if (onlyNotification) {
            alertMinutes =
                DISABLED

            if (notificationMinutes < 0) {
                notificationMinutes =
                    DEFAULT_NOTIFICATION_MINUTES
            }
        }

        if (
            onlyAlert &&
            onlyNotification
        ) {
            warnings +=
                "Você pediu somente alerta e somente notificação ao mesmo tempo. Revise os avisos antes de confirmar."
        }

        if (
            notificationIntent &&
            explicitNotification == null &&
            !disableNotification &&
            !onlyAlert &&
            !onlyNotification
        ) {
            warnings +=
                "Você mencionou notificação, mas o tempo de antecedência não foi entendido. Mantive o padrão de 2 horas."
        }

        if (
            notificationMinutes >= 0 &&
            alertMinutes >= 0 &&
            notificationMinutes <
            alertMinutes
        ) {
            warnings +=
                "A notificação comum está programada depois do alerta forte. Revise os dois tempos antes de confirmar."
        }

        if (
            onlyAlert &&
            explicitNotification != null
        ) {
            warnings +=
                "A frase contém 'somente alerta' e também um tempo de notificação. 'Somente alerta' prevaleceu e a notificação foi desativada."
        }

        if (
            onlyNotification &&
            hasExplicitAlertIntent(normalized)
        ) {
            warnings +=
                "A frase contém 'somente notificação' e também um alerta. 'Somente notificação' prevaleceu e o alerta forte foi desativado."
        }

        return parsed.copy(
            originalText =
                text,
            notificationMinutesBefore =
                notificationMinutes,
            reminderMinutesBefore =
                alertMinutes,
            warnings =
                warnings.distinct()
        )
    }

    fun parseManualRecurrence(
        text: String,
        fallbackWeekdays:
            Set<DayOfWeek>,
        fallbackMonthDay:
            Int?
    ): Triple<
        RecurrenceType,
        Set<DayOfWeek>,
        Int?
    > =
        base.parseManualRecurrence(
            text,
            fallbackWeekdays,
            fallbackMonthDay
        )

    private fun hasExplicitAlertIntent(
        normalized: String
    ): Boolean =
        ALERT_INTENT
            .containsMatchIn(normalized) &&
            !DISABLE_ALERT_PATTERN
                .containsMatchIn(normalized)

    private fun parseMinutes(
        amount: String,
        unit: String
    ): Int? {
        val number =
            when (amount) {
                "meia" -> {
                    if (
                        unit.startsWith("hora") ||
                        unit == "h"
                    ) {
                        return 30
                    }

                    return null
                }

                "uma",
                "um" ->
                    1

                else ->
                    amount.toIntOrNull()
                        ?: return null
            }

        return when {
            unit == "h" ||
                unit.startsWith("hora") ->
                number * 60

            unit.startsWith("min") ->
                number

            else ->
                null
        }
    }

    private fun replaceMatchesWithSpaces(
        original: String,
        regex: Regex
    ): String {
        val normalized =
            normalizeStable(original)

        val chars =
            original.toCharArray()

        regex.findAll(normalized)
            .map { it.range }
            .forEach { range ->
                val start =
                    range.first
                        .coerceAtLeast(0)

                val end =
                    range.last
                        .coerceAtMost(
                            chars.lastIndex
                        )

                if (start <= end) {
                    for (
                        index in start..end
                    ) {
                        chars[index] = ' '
                    }
                }
            }

        return String(chars)
    }

    private fun normalizeStable(
        text: String
    ): String =
        buildString(text.length) {
            text.lowercase()
                .forEach { c ->
                    append(
                        when (c) {
                            'á', 'à', 'ã', 'â', 'ä' -> 'a'
                            'é', 'è', 'ê', 'ë' -> 'e'
                            'í', 'ì', 'î', 'ï' -> 'i'
                            'ó', 'ò', 'õ', 'ô', 'ö' -> 'o'
                            'ú', 'ù', 'û', 'ü' -> 'u'
                            'ç' -> 'c'
                            else -> c
                        }
                    )
                }
        }

    companion object {
        const val DISABLED =
            -1

        private const val
            DEFAULT_NOTIFICATION_MINUTES =
            120

        private const val
            DEFAULT_ALERT_MINUTES =
            60

        private val NOTIFICATION_INTENT =
            Regex(
                """\b(?:notificar|notifique|notificacao|notificacoes|notif|noti)\b"""
            )

        private val ALERT_INTENT =
            Regex(
                """\b(?:alertar|alerta|alertas|avisar|avise)\b"""
            )

        private val NOTIFICATION_PATTERN =
            Regex(
                """\b(?:notificar|notifique|notificacao|notif|noti)\s+(?:com\s+)?(meia|uma|um|\d+)\s*(h|hora|horas|min|minuto|minutos)\s*(?:antes|de\s+antecedencia)?\b"""
            )

        private val DISABLE_ALERT_PATTERN =
            Regex(
                """\b(?:(?:nao|n)\s+(?:alertar|alerta|alertas)|sem\s+(?:alerta|alertas|alertar)|nao\s+quero\s+(?:alerta|alertas)|desativar\s+(?:o\s+)?alerta|alerta\s+desativado)\b"""
            )

        private val DISABLE_NOTIFICATION_PATTERN =
            Regex(
                """\b(?:(?:nao|n)\s+(?:notificar|notificacao|notificacoes|notif|noti)|sem\s+(?:notificar|notificacao|notificacoes|notif|noti)|nao\s+quero\s+(?:notificacao|notificacoes)|desativar\s+(?:a\s+)?notificacao|notificacao\s+desativada)\b"""
            )

        private val ONLY_ALERT_PATTERN =
            Regex(
                """\b(?:so|somente|apenas)\s+(?:alerta|alertas|alertar)\b"""
            )

        private val ONLY_NOTIFICATION_PATTERN =
            Regex(
                """\b(?:so|somente|apenas)\s+(?:notificacao|notificacoes|notificar|notif|noti)\b"""
            )
    }
}
