package com.anotai.app.parser

import com.anotai.app.model.RecurrenceType
import com.anotai.app.model.TaskDraft
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class TaskTextParser {

    private val months = mapOf(
        "janeiro" to 1, "fevereiro" to 2, "marco" to 3, "abril" to 4,
        "maio" to 5, "junho" to 6, "julho" to 7, "agosto" to 8,
        "setembro" to 9, "outubro" to 10, "novembro" to 11, "dezembro" to 12
    )

    private val weekdays = linkedMapOf(
        "segunda" to DayOfWeek.MONDAY,
        "terca" to DayOfWeek.TUESDAY,
        "quarta" to DayOfWeek.WEDNESDAY,
        "quinta" to DayOfWeek.THURSDAY,
        "sexta" to DayOfWeek.FRIDAY,
        "sabado" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY
    )

    private val alertIntentRegex = Regex(
        """\b(alertar|alerta|avisar|avise|avisa|lembrar|lembre|lembra|notificar|notifique|notifica)\b"""
    )

    fun parse(text: String, now: LocalDateTime = LocalDateTime.now()): TaskDraft {
        val original = text.trim()
        val normalized = normalize(original)
        val warnings = mutableListOf<String>()

        val reminderResult = parseReminder(normalized)
        val reminder =
            if (reminderResult.second) {
                reminderResult.first
            } else {
                60
            }
        val reminderRecognized = reminderResult.second

        val mainPart = cutAtReminder(normalized)
        val time = parseTime(mainPart)

        val untilResult = parseUntil(normalized, now.toLocalDate())
        if (untilResult.second != null) warnings += untilResult.second!!

        val monthlyMatch = Regex("""\btodo\s+dia\s+(\d{1,2})\b""").find(normalized)
        val isDaily = Regex("""\btodos?\s+os?\s+dias\b""").containsMatchIn(normalized) ||
            Regex("""\bdiariamente\b""").containsMatchIn(normalized) ||
            (Regex("""\btodo\s+dia\b""").containsMatchIn(normalized) && monthlyMatch == null)

        val mentionedWeekdays = weekdays
            .filterKeys { Regex("""\b${it}(?:-feira)?s?\b""").containsMatchIn(normalized) }
            .values
            .toSet()

        val hasWeeklyKeyword =
            Regex("""\b(toda|todo|todas|todos)\b""").containsMatchIn(normalized)

        val isWeekly = mentionedWeekdays.size >= 2 ||
            (
                mentionedWeekdays.size == 1 &&
                    hasWeeklyKeyword &&
                    !isDaily &&
                    monthlyMatch == null
                )

        val recurrenceType = when {
            monthlyMatch != null -> RecurrenceType.MONTHLY
            isDaily -> RecurrenceType.DAILY
            isWeekly -> RecurrenceType.WEEKLY
            else -> RecurrenceType.NONE
        }

        val monthDay = monthlyMatch?.groupValues?.get(1)?.toIntOrNull()

        val dateWithoutUntil = normalized.substringBefore(" ate ")
        var date = parseExplicitDate(
            dateWithoutUntil,
            now.toLocalDate(),
            recurrenceType,
            warnings
        )

        val strictNextWeekday =
            Regex("""\b(proxima|proximo)\s+[a-z]+""").containsMatchIn(normalized) ||
                Regex("""\b[a-z]+(?:-feira)?\s+que\s+vem\b""").containsMatchIn(normalized)

        if (date == null &&
            recurrenceType == RecurrenceType.WEEKLY &&
            mentionedWeekdays.isNotEmpty()
        ) {
            date = mentionedWeekdays
                .map { nextWeekday(now.toLocalDate(), it, strict = false) }
                .minOrNull()
        }

        if (date == null &&
            recurrenceType == RecurrenceType.MONTHLY &&
            monthDay != null
        ) {
            date = nextMonthlyDate(now.toLocalDate(), monthDay)
        }

        if (date == null && recurrenceType == RecurrenceType.DAILY) {
            date = chooseDateFromTime(now, time)
        }

        if (date == null &&
            recurrenceType == RecurrenceType.NONE &&
            mentionedWeekdays.size == 1
        ) {
            date = nextWeekday(
                now.toLocalDate(),
                mentionedWeekdays.first(),
                strict = strictNextWeekday
            )
        }

        if (date == null && time != null) {
            date = chooseDateFromTime(now, time)
            warnings += if (date == now.toLocalDate()) {
                "Data não informada; interpretei como hoje (${formatDate(date)})."
            } else {
                "Data não informada e o horário de hoje já passou; interpretei como amanhã (${formatDate(date)})."
            }
        }

        if (time == null) {
            warnings += "Horário não identificado. Sem horário, nenhum alerta será agendado."
        }

        if (alertIntentRegex.containsMatchIn(normalized) && !reminderRecognized) {
            warnings += "Encontrei um pedido de alerta, mas não consegui entender quanto tempo antes devo avisar."
        }

        if (Regex("""\bate\b""").containsMatchIn(normalized) && untilResult.first == null) {
            warnings += "Encontrei uma data final indicada por 'até', mas não consegui interpretar essa data."
        }

        val recurrenceIntent =
            Regex("""\b(toda|todo|todas|todos|diariamente|semanalmente|mensalmente)\b""")
                .containsMatchIn(normalized)

        if (recurrenceIntent && recurrenceType == RecurrenceType.NONE) {
            warnings += "Há indicação de repetição no texto, mas não consegui definir a recorrência com segurança."
        }

        val title = extractTitle(original, normalized)
        if (title.isBlank()) {
            warnings += "Não consegui identificar claramente o nome da tarefa."
        }

        return TaskDraft(
            title = title.ifBlank { original },
            originalText = original,
            date = date,
            time = time,
            reminderMinutesBefore = reminder,
            recurrenceType = recurrenceType,
            weekdays = if (recurrenceType == RecurrenceType.WEEKLY) mentionedWeekdays else emptySet(),
            monthDay = if (recurrenceType == RecurrenceType.MONTHLY) monthDay else null,
            untilDate = untilResult.first,
            warnings = warnings.distinct()
        )
    }

    fun parseManualRecurrence(
        value: String,
        fallbackWeekdays: Set<DayOfWeek> = emptySet(),
        fallbackMonthDay: Int? = null
    ): Triple<RecurrenceType, Set<DayOfWeek>, Int?> {
        val n = normalize(value.trim())

        if (n.isBlank() || n.contains("nao se repete")) {
            return Triple(RecurrenceType.NONE, emptySet(), null)
        }

        if (
            n.contains("todos os dias") ||
            n == "diario" ||
            n == "diaria" ||
            n == "diariamente"
        ) {
            return Triple(RecurrenceType.DAILY, emptySet(), null)
        }

        val monthly = Regex("""todo\s+dia\s+(\d{1,2})""")
            .find(n)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

        if (monthly != null && monthly in 1..31) {
            return Triple(RecurrenceType.MONTHLY, emptySet(), monthly)
        }

        val days = weekdays.filterKeys { key ->
            Regex("""\b${key}(?:-feira)?s?\b""").containsMatchIn(n)
        }.values.toSet()

        if (days.isNotEmpty()) {
            return Triple(RecurrenceType.WEEKLY, days, null)
        }

        return when {
            n == "semanal" && fallbackWeekdays.isNotEmpty() ->
                Triple(RecurrenceType.WEEKLY, fallbackWeekdays, null)

            n == "mensal" && fallbackMonthDay != null ->
                Triple(RecurrenceType.MONTHLY, emptySet(), fallbackMonthDay)

            else ->
                Triple(RecurrenceType.NONE, emptySet(), null)
        }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(
            value.lowercase(),
            Normalizer.Form.NFD
        )
            .replace(Regex("""\p{M}+"""), "")
            .replace("–", "-")
            .replace("—", "-")
    }

    private fun cutAtReminder(n: String): String {
        val markers = listOf(
            " alertar",
            " alerta",
            " avisar",
            " avise",
            " avisa",
            " lembrar",
            " lembre",
            " lembra",
            " notificar",
            " notifique",
            " notifica"
        )

        val positions = markers
            .map { n.indexOf(it) }
            .filter { it >= 0 }

        return if (positions.isEmpty()) {
            n
        } else {
            n.substring(0, positions.minOrNull()!!)
        }
    }

    private fun parseTime(n: String): LocalTime? {
        Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""").find(n)?.let {
            return LocalTime.of(
                it.groupValues[1].toInt(),
                it.groupValues[2].toInt()
            )
        }

        Regex("""\b([01]?\d|2[0-3])h(?:([0-5]\d))?\b""").find(n)?.let {
            val minute = it.groupValues[2]
                .takeIf(String::isNotBlank)
                ?.toInt()
                ?: 0

            return LocalTime.of(
                it.groupValues[1].toInt(),
                minute
            )
        }

        Regex("""\bas\s+([01]?\d|2[0-3])\b""").find(n)?.let {
            return LocalTime.of(
                it.groupValues[1].toInt(),
                0
            )
        }

        Regex("""\b([01]?\d|2[0-3])\s*(?:h|horas?)?\s+e\s+meia\b""")
            .find(n)
            ?.let {
                return LocalTime.of(
                    it.groupValues[1].toInt(),
                    30
                )
            }

        return null
    }

    private fun parseReminder(n: String): Pair<Int, Boolean> {
        val standard = Regex(
            """(?:alertar|alerta|avisar|avise|avisa|lembrar|lembre|lembra|notificar|notifique|notifica)[^.!?]{0,45}?\b(meia|uma|um|\d+)\s*(h|hora|horas|min|minuto|minutos)\s+antes"""
        ).find(n)

        if (standard != null) {
            return reminderAmount(
                standard.groupValues[1],
                standard.groupValues[2]
            ) to true
        }

        val simple = Regex(
            """\b(meia|uma|um|\d+)\s*(h|hora|horas|min|minuto|minutos)\s+antes\b"""
        ).find(n)

        if (simple != null) {
            return reminderAmount(
                simple.groupValues[1],
                simple.groupValues[2]
            ) to true
        }

        val antecedence = Regex(
            """\b(?:com\s+)?(meia|uma|um|\d+)\s*(h|hora|horas|min|minuto|minutos)\s+de\s+antecedencia\b"""
        ).find(n)

        if (antecedence != null) {
            return reminderAmount(
                antecedence.groupValues[1],
                antecedence.groupValues[2]
            ) to true
        }

        return 0 to false
    }

    private fun reminderAmount(amountRaw: String, unit: String): Int {
        if (amountRaw == "meia") return 30

        val amount = when (amountRaw) {
            "uma", "um" -> 1
            else -> amountRaw.toIntOrNull() ?: 0
        }

        return if (unit == "h" || unit.startsWith("hora")) {
            amount * 60
        } else {
            amount
        }
    }

    private fun parseUntil(
        n: String,
        baseDate: LocalDate
    ): Pair<LocalDate?, String?> {
        val slash = Regex(
            """\bate\s+(?:o\s+)?(?:dia\s+)?(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?"""
        ).find(n)

        if (slash != null) {
            val day = slash.groupValues[1].toInt()
            val month = slash.groupValues[2].toInt()
            val yearRaw = slash.groupValues[3]

            val year = if (yearRaw.isBlank()) {
                chooseYear(baseDate, month, day)
            } else {
                normalizeYear(yearRaw.toInt())
            }

            val date = safeDate(year, month, day)

            return date to when {
                date == null ->
                    "A data final parece inválida."

                yearRaw.isBlank() ->
                    "Ano da data final não informado; interpretei como ${formatDate(date)}."

                else ->
                    null
            }
        }

        val named = Regex(
            """\bate\s+(?:o\s+)?(?:dia\s+)?(\d{1,2})(?:\s+de\s+([a-z]+))?(?:\s+de\s+(\d{4}))?"""
        ).find(n) ?: return null to null

        val day = named.groupValues[1].toInt()
        val monthName = named.groupValues[2]
        val yearRaw = named.groupValues[3]

        val month = if (monthName.isBlank()) {
            baseDate.monthValue
        } else {
            months[monthName]
        }

        if (month == null) {
            return null to "Não reconheci o mês informado na data final."
        }

        val year = if (yearRaw.isBlank()) {
            chooseYear(baseDate, month, day)
        } else {
            yearRaw.toInt()
        }

        val date = safeDate(year, month, day)

        val warning = when {
            date == null ->
                "A data final parece inválida."

            monthName.isBlank() ->
                "Mês da data final não informado; interpretei como ${formatDate(date)}."

            yearRaw.isBlank() ->
                "Ano da data final não informado; interpretei como ${formatDate(date)}."

            else ->
                null
        }

        return date to warning
    }

    private fun parseExplicitDate(
        n: String,
        baseDate: LocalDate,
        recurrenceType: RecurrenceType,
        warnings: MutableList<String>
    ): LocalDate? {
        if (Regex("""\bdepois\s+de\s+amanha\b""").containsMatchIn(n)) {
            return baseDate.plusDays(2)
        }

        if (Regex("""\bamanha\b""").containsMatchIn(n)) {
            return baseDate.plusDays(1)
        }

        if (Regex("""\bhoje\b""").containsMatchIn(n)) {
            return baseDate
        }

        val slash = Regex(
            """(?<!\d)(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?(?!\d)"""
        ).find(n)

        if (slash != null) {
            val day = slash.groupValues[1].toInt()
            val month = slash.groupValues[2].toInt()
            val yearRaw = slash.groupValues[3]

            val year = if (yearRaw.isBlank()) {
                chooseYear(baseDate, month, day)
            } else {
                normalizeYear(yearRaw.toInt())
            }

            val date = safeDate(year, month, day)

            if (yearRaw.isBlank() && date != null) {
                warnings += "Ano não informado; interpretei a data como ${formatDate(date)}."
            }

            if (date == null) {
                warnings += "A data informada parece inválida."
            }

            return date
        }

        val named = Regex(
            """\bdia\s+(\d{1,2})\s+de\s+([a-z]+)(?:\s+de\s+(\d{4}))?"""
        ).find(n)

        if (named != null) {
            val day = named.groupValues[1].toInt()
            val month = months[named.groupValues[2]]

            if (month == null) {
                warnings += "Não reconheci o mês informado."
                return null
            }

            val yearRaw = named.groupValues[3]

            val year = if (yearRaw.isBlank()) {
                chooseYear(baseDate, month, day)
            } else {
                yearRaw.toInt()
            }

            val date = safeDate(year, month, day)

            if (yearRaw.isBlank() && date != null) {
                warnings += "Ano não informado; interpretei a data como ${formatDate(date)}."
            }

            if (date == null) {
                warnings += "A data informada parece inválida."
            }

            return date
        }

        if (recurrenceType != RecurrenceType.MONTHLY) {
            val dayOnly = Regex("""\bdia\s+(\d{1,2})\b""").find(n)

            if (dayOnly != null) {
                val day = dayOnly.groupValues[1].toInt()
                val date = nextDayOfMonth(baseDate, day)

                if (date != null) {
                    warnings += "Mês não informado; interpretei 'dia $day' como ${formatDate(date)}."
                } else {
                    warnings += "O dia informado parece inválido."
                }

                return date
            }
        }

        return null
    }

    private fun chooseDateFromTime(
        now: LocalDateTime,
        time: LocalTime?
    ): LocalDate {
        if (time == null) return now.toLocalDate()

        return if (time.isAfter(now.toLocalTime())) {
            now.toLocalDate()
        } else {
            now.toLocalDate().plusDays(1)
        }
    }

    private fun nextWeekday(
        base: LocalDate,
        day: DayOfWeek,
        strict: Boolean
    ): LocalDate {
        var d = if (strict) base.plusDays(1) else base

        repeat(8) {
            if (d.dayOfWeek == day) return d
            d = d.plusDays(1)
        }

        return d
    }

    private fun nextMonthlyDate(
        base: LocalDate,
        day: Int
    ): LocalDate? {
        if (day !in 1..31) return null

        var ym = YearMonth.from(base)

        repeat(24) {
            if (day <= ym.lengthOfMonth()) {
                val candidate = ym.atDay(day)

                if (!candidate.isBefore(base)) {
                    return candidate
                }
            }

            ym = ym.plusMonths(1)
        }

        return null
    }

    private fun nextDayOfMonth(
        base: LocalDate,
        day: Int
    ): LocalDate? = nextMonthlyDate(base, day)

    private fun chooseYear(
        base: LocalDate,
        month: Int,
        day: Int
    ): Int {
        val thisYear = safeDate(base.year, month, day)

        return if (thisYear != null && !thisYear.isBefore(base)) {
            base.year
        } else {
            base.year + 1
        }
    }

    private fun normalizeYear(year: Int): Int =
        if (year < 100) 2000 + year else year

    private fun safeDate(
        year: Int,
        month: Int,
        day: Int
    ): LocalDate? =
        try {
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }

    private fun formatDate(date: LocalDate): String =
        "%02d/%02d/%04d".format(
            date.dayOfMonth,
            date.monthValue,
            date.year
        )

    private fun extractTitle(
        original: String,
        normalized: String
    ): String {
        val markers = mutableListOf<Int>()

        fun addIndex(index: Int) {
            if (index > 0) markers += index
        }

        listOf(
            " depois de amanha",
            " amanha",
            " hoje",
            " proxima ",
            " proximo ",
            " toda ",
            " todo ",
            " todos ",
            " todas ",
            " diariamente",
            " dia ",
            " alertar",
            " alerta",
            " avisar",
            " avise",
            " avisa",
            " lembrar",
            " lembre",
            " lembra",
            " notificar",
            " notifique",
            " notifica",
            " ate "
        ).forEach {
            addIndex(normalized.indexOf(it))
        }

        weekdays.keys.forEach {
            addIndex(normalized.indexOf(" $it"))
        }

        Regex("""\b([01]?\d|2[0-3])(?::[0-5]\d|h(?:[0-5]\d)?)\b""")
            .find(normalized)
            ?.range
            ?.first
            ?.let(::addIndex)

        Regex("""\bas\s+([01]?\d|2[0-3])\b""")
            .find(normalized)
            ?.range
            ?.first
            ?.let(::addIndex)

        val end = markers.minOrNull() ?: original.length

        return original
            .substring(0, end.coerceAtMost(original.length))
            .trim()
            .trimEnd('.', ',', ';', '-', ':')
            .replace(Regex("""\s+"""), " ")
            .replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase()
                } else {
                    it.toString()
                }
            }
    }
}
