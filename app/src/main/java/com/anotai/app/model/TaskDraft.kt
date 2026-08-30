package com.anotai.app.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY
}

data class TaskDraft(
    val title: String,
    val originalText: String,
    val date: LocalDate?,
    val time: LocalTime?,
    val reminderMinutesBefore: Int,
    val notificationMinutesBefore: Int = 120,
    val recurrenceType: RecurrenceType,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthDay: Int? = null,
    val untilDate: LocalDate? = null,
    val warnings: List<String> = emptyList()
) {
    fun dateLabel(): String =
        date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Não informada"

    fun timeLabel(): String =
        time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Não informado"
    fun reminderLabel(): String =
        when {
            reminderMinutesBefore < 0 ->
                "Desativado"

            time == null ->
                "Sem alerta (horário ausente)"

            reminderMinutesBefore == 0 ->
                "No horário da tarefa"

            reminderMinutesBefore % 60 == 0 -> {
                val hours =
                    reminderMinutesBefore / 60

                if (hours == 1) {
                    "1 hora antes"
                } else {
                    "$hours horas antes"
                }
            }

            else ->
                "$reminderMinutesBefore minutos antes"
        }


    fun recurrenceLabel(): String = when (recurrenceType) {
        RecurrenceType.NONE -> "Não se repete"
        RecurrenceType.DAILY -> "Todos os dias"
        RecurrenceType.MONTHLY -> "Todo dia ${monthDay ?: "?"}"
        RecurrenceType.WEEKLY -> {
            val labels = weekdays.sortedBy { it.value }.map {
                when (it) {
                    DayOfWeek.MONDAY -> "segunda-feira"
                    DayOfWeek.TUESDAY -> "terça-feira"
                    DayOfWeek.WEDNESDAY -> "quarta-feira"
                    DayOfWeek.THURSDAY -> "quinta-feira"
                    DayOfWeek.FRIDAY -> "sexta-feira"
                    DayOfWeek.SATURDAY -> "sábado"
                    DayOfWeek.SUNDAY -> "domingo"
                }
            }
            if (labels.size == 1) "Toda ${labels.first()}" else labels.joinToString(", ")
        }
    }

    fun untilLabel(): String =
        untilDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Sem data final"
}
