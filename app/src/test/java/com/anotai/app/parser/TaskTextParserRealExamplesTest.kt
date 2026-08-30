package com.anotai.app.parser

import com.anotai.app.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskTextParserRealExamplesTest {

    private val parser = TaskTextParser()
    private val now = LocalDateTime.of(2026, 8, 29, 19, 16)

    @Test
    fun buscarFilhaAmanhaComAlerta() {
        val r = parser.parse(
            "buscar filha amanhã 8h e alertar 1 hora antes",
            now
        )

        assertEquals("Buscar filha", r.title)
        assertEquals(LocalDate.of(2026, 8, 30), r.date)
        assertEquals(LocalTime.of(8, 0), r.time)
        assertEquals(60, r.reminderMinutesBefore)
        assertEquals(RecurrenceType.NONE, r.recurrenceType)
    }

    @Test
    fun buscarFilhaComMeAvise() {
        val r = parser.parse(
            "buscar filha amanhã às 8 e me avise 1h antes",
            now
        )

        assertEquals(LocalDate.of(2026, 8, 30), r.date)
        assertEquals(LocalTime.of(8, 0), r.time)
        assertEquals(60, r.reminderMinutesBefore)
    }

    @Test
    fun buscarFilhaComUmaHoraAntes() {
        val r = parser.parse(
            "buscar filha 08:00 avisar uma hora antes",
            now
        )

        assertEquals(LocalTime.of(8, 0), r.time)
        assertEquals(60, r.reminderMinutesBefore)
    }

    @Test
    fun buscarFilhaComMeiaHoraAntes() {
        val r = parser.parse(
            "buscar filha amanhã 8h alerta meia hora antes",
            now
        )

        assertEquals(30, r.reminderMinutesBefore)
    }

    @Test
    fun dentistaAmanha() {
        val r = parser.parse(
            "dentista amanhã 14h",
            now
        )

        assertEquals("Dentista", r.title)
        assertEquals(LocalDate.of(2026, 8, 30), r.date)
        assertEquals(LocalTime.of(14, 0), r.time)
    }

    @Test
    fun clientesTodaTercaAteJunho() {
        val r = parser.parse(
            "buscar clientes toda terça 20h até dia 20 de junho. alertar 1 hora antes.",
            now
        )

        assertEquals(RecurrenceType.WEEKLY, r.recurrenceType)
        assertTrue(DayOfWeek.TUESDAY in r.weekdays)
        assertEquals(LocalTime.of(20, 0), r.time)
        assertEquals(LocalDate.of(2027, 6, 20), r.untilDate)
        assertEquals(60, r.reminderMinutesBefore)
    }

    @Test
    fun academiaTresDias() {
        val r = parser.parse(
            "academia segunda quarta e sexta 18h",
            now
        )

        assertEquals(RecurrenceType.WEEKLY, r.recurrenceType)
        assertEquals(
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.FRIDAY
            ),
            r.weekdays
        )
    }

    @Test
    fun remedioTodosOsDias() {
        val r = parser.parse(
            "tomar remédio todos os dias às 8h",
            now
        )

        assertEquals(RecurrenceType.DAILY, r.recurrenceType)
        assertEquals(LocalTime.of(8, 0), r.time)
    }

    @Test
    fun aluguelTodoDiaCinco() {
        val r = parser.parse(
            "pagar aluguel todo dia 5 às 9h",
            now
        )

        assertEquals(RecurrenceType.MONTHLY, r.recurrenceType)
        assertEquals(5, r.monthDay)
        assertEquals(LocalDate.of(2026, 9, 5), r.date)
    }

    @Test
    fun diaSemMesGeraAmbiguidade() {
        val r = parser.parse(
            "reunião dia 10 às 8",
            now
        )

        assertEquals(LocalDate.of(2026, 9, 10), r.date)
        assertEquals(LocalTime.of(8, 0), r.time)
        assertTrue(
            r.warnings.any { it.contains("Mês não informado") }
        )
    }

    @Test
    fun depoisDeAmanha() {
        val r = parser.parse(
            "consulta depois de amanhã 10h",
            now
        )

        assertEquals(LocalDate.of(2026, 8, 31), r.date)
        assertEquals(LocalTime.of(10, 0), r.time)
    }

    @Test
    fun tercaQueVem() {
        val r = parser.parse(
            "dentista terça que vem 14h",
            now
        )

        assertEquals(LocalDate.of(2026, 9, 1), r.date)
        assertEquals(LocalTime.of(14, 0), r.time)
        assertEquals(RecurrenceType.NONE, r.recurrenceType)
    }

    @Test
    fun horarioComEmeia() {
        val r = parser.parse(
            "lanche amanhã 8 e meia",
            now
        )

        assertEquals(LocalTime.of(8, 30), r.time)
    }

    @Test
    fun pedidoDeAlertaIncompletoGeraAviso() {
        val r = parser.parse(
            "buscar filha amanhã 8h e me avise antes",
            now
        )

        assertTrue(
            r.warnings.any {
                it.contains("pedido de alerta")
            }
        )
    }

    @Test
    fun ateSemDataCompletaGeraAviso() {
        val r = parser.parse(
            "buscar clientes toda terça 20h até junho",
            now
        )

        assertTrue(
            r.warnings.any {
                it.contains("data final")
            }
        )
    }

    @Test
    fun antecedenciaTambemFunciona() {
        val r = parser.parse(
            "consulta amanhã 10h com 30 minutos de antecedência",
            now
        )

        assertEquals(30, r.reminderMinutesBefore)
    }
}
