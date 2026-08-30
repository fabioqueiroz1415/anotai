package com.anotai.app.alarm

import java.text.Normalizer

data class ReminderLeadSet(
    val notificationMinutes: List<Int>,
    val alertMinutes: List<Int>
)

data class PreAlertSpec(
    val notificationLeadMinutes: Int,
    val alertLeadMinutes: Int
)

object ReminderLeadPolicy {

    val DEFAULT_NOTIFICATION_MINUTES =
        listOf(
            120,
            30,
            15,
            5,
            0
        )

    val DEFAULT_ALERT_MINUTES =
        listOf(
            60,
            0
        )

    private enum class CommandKind {
        NOTIFICATION,
        ALERT
    }

    private data class CommandOccurrence(
        val kind: CommandKind,
        val start: Int,
        val endExclusive: Int
    )

    private val commandRegex =
        Regex(
            """\b(alertar|alerta|alertas|notificar|notificacao|notificacoes|notif|noti)\b"""
        )

    private val durationRegex =
        Regex(
            """\b(\d+|um|uma|dois|duas|cinco|quinze|trinta)\s*(h|hora|horas|min|mins|minuto|minutos)\s*antes\b"""
        )

    private val halfHourRegex =
        Regex(
            """\bmeia\s+hora\s+antes\b"""
        )

    private val momentRegex =
        Regex(
            """\b(na\s+hora|no\s+momento|naquele\s+momento|na\s+hora\s+da\s+tarefa)\b"""
        )

    fun resolve(originalText: String): ReminderLeadSet {
        val normalized = normalize(originalText)

        return ReminderLeadSet(
            notificationMinutes =
                resolveForKind(
                    normalized,
                    CommandKind.NOTIFICATION,
                    DEFAULT_NOTIFICATION_MINUTES
                ),
            alertMinutes =
                resolveForKind(
                    normalized,
                    CommandKind.ALERT,
                    DEFAULT_ALERT_MINUTES
                )
        )
    }

    fun notificationLeads(originalText: String): List<Int> =
        resolve(originalText).notificationMinutes

    fun alertLeads(originalText: String): List<Int> =
        resolve(originalText).alertMinutes

    fun preAlertSpecs(originalText: String): List<PreAlertSpec> =
        alertLeads(originalText)
            .map { alertLead ->
                PreAlertSpec(
                    notificationLeadMinutes = alertLead + 30,
                    alertLeadMinutes = alertLead
                )
            }
            .distinctBy {
                it.notificationLeadMinutes to it.alertLeadMinutes
            }
            .sortedByDescending {
                it.notificationLeadMinutes
            }

    private fun resolveForKind(
        text: String,
        kind: CommandKind,
        defaults: List<Int>
    ): List<Int> {
        if (
            isDisabled(text, kind) ||
            isOtherKindExclusive(text, kind)
        ) {
            return emptyList()
        }

        val explicit = explicitMinutes(text, kind)

        val result =
            if (isExclusive(text, kind)) {
                if (explicit.isEmpty()) {
                    defaults
                } else {
                    explicit
                }
            } else {
                defaults + explicit
            }

        return result
            .filter { it >= 0 }
            .distinct()
            .sortedDescending()
    }

    private fun explicitMinutes(
        text: String,
        wantedKind: CommandKind
    ): List<Int> {
        val commands =
            commandRegex
                .findAll(text)
                .map { match ->
                    CommandOccurrence(
                        kind = commandKind(match.value),
                        start = match.range.first,
                        endExclusive = match.range.last + 1
                    )
                }
                .toList()

        val result = mutableListOf<Int>()

        commands.forEachIndexed { index, command ->
            if (command.kind != wantedKind) {
                return@forEachIndexed
            }

            val segmentEnd =
                commands.getOrNull(index + 1)?.start ?: text.length

            val segment =
                text.substring(command.endExclusive, segmentEnd)

            durationRegex.findAll(segment).forEach { match ->
                val amount = parseAmount(match.groupValues[1])
                val unit = match.groupValues[2]

                result +=
                    if (unit.startsWith("h")) {
                        amount * 60
                    } else {
                        amount
                    }
            }

            if (halfHourRegex.containsMatchIn(segment)) {
                result += 30
            }

            if (momentRegex.containsMatchIn(segment)) {
                result += 0
            }
        }

        return result
    }

    private fun isDisabled(text: String, kind: CommandKind): Boolean {
        val target =
            if (kind == CommandKind.ALERT) {
                """(?:alerta|alertar|alertas)"""
            } else {
                """(?:notificar|notificacao|notificacoes|notif|noti)"""
            }

        return Regex(
            """\b(?:nao|n|sem)\s+$target\b"""
        ).containsMatchIn(text)
    }

    private fun isOtherKindExclusive(
        text: String,
        kind: CommandKind
    ): Boolean {
        val other =
            when (kind) {
                CommandKind.NOTIFICATION ->
                    CommandKind.ALERT

                CommandKind.ALERT ->
                    CommandKind.NOTIFICATION
            }

        return isExclusive(
            text,
            other
        )
    }

    private fun isExclusive(text: String, kind: CommandKind): Boolean {
        val target =
            if (kind == CommandKind.ALERT) {
                """(?:alerta|alertar|alertas)"""
            } else {
                """(?:notificar|notificacao|notificacoes|notif|noti)"""
            }

        return Regex(
            """\b(?:so|somente|apenas)\s+(?:\w+\s+){0,2}$target\b"""
        ).containsMatchIn(text)
    }

    private fun commandKind(value: String): CommandKind =
        if (value.startsWith("alert")) {
            CommandKind.ALERT
        } else {
            CommandKind.NOTIFICATION
        }

    private fun parseAmount(value: String): Int =
        value.toIntOrNull()
            ?: when (value) {
                "um", "uma" -> 1
                "dois", "duas" -> 2
                "cinco" -> 5
                "quinze" -> 15
                "trinta" -> 30
                else -> 0
            }

    private fun normalize(value: String): String =
        Normalizer
            .normalize(
                value.lowercase(),
                Normalizer.Form.NFD
            )
            .replace(Regex("""\p{M}+"""), "")
            .replace('’', '\'')
            .replace(Regex("""\s+"""), " ")
            .trim()
}
