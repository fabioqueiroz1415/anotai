package com.anotai.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object FriendlyDateFormatter {

    private val shortDate =
        DateTimeFormatter.ofPattern("dd.MM.yy")

    fun format(
        date: LocalDate?,
        today: LocalDate = LocalDate.now()
    ): String {
        if (date == null) {
            return "Não informada"
        }

        val weekday =
            weekdayLabel(date.dayOfWeek)

        val dateText =
            date.format(shortDate)

        val days =
            ChronoUnit.DAYS.between(
                today,
                date
            )

        return when (days) {
            0L ->
                "Hoje | $weekday | $dateText"

            1L ->
                "Amanhã | $weekday | $dateText"

            2L ->
                "Depois de amanhã | $weekday | $dateText"

            in 3L..7L ->
                "Próx. $weekday | $dateText"

            else ->
                "$weekday | $dateText"
        }
    }

    private fun weekdayLabel(
        day: DayOfWeek
    ): String =
        when (day) {
            DayOfWeek.MONDAY -> "seg"
            DayOfWeek.TUESDAY -> "ter"
            DayOfWeek.WEDNESDAY -> "qua"
            DayOfWeek.THURSDAY -> "qui"
            DayOfWeek.FRIDAY -> "sex"
            DayOfWeek.SATURDAY -> "sáb"
            DayOfWeek.SUNDAY -> "dom"
        }
}
