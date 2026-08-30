package com.anotai.app.parser

import com.anotai.app.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskTextParserTest {

    private val parser = TaskTextParser()
    private val now = LocalDateTime.of(2026, 8, 29, 19, 16)

    @Test
    fun filhaAmanhaComAlerta() {
        val r = parser.parse("buscar filha amanhã 8h e alertar 1 hora antes", now)
        assertEquals("Buscar filha", r.title)
        assertEquals(LocalDate.of(2026, 8, 30), r.date)
        assertEquals(LocalTime.of(8, 0), r.time)
        assertEquals(60, r.reminderMinutesBefore)
        assertEquals(RecurrenceType.NONE, r.recurrenceType)
    }

    @Test
    fun clientesTodaTercaAteJunho() {
        val r = parser.parse(
            "buscar clientes toda terça 20h até dia 20 de junho. alertar 1 hora antes.",
            now
        )
        assertEquals("Buscar clientes", r.title)
        assertEquals(LocalTime.of(20, 0), r.time)
        assertEquals(RecurrenceType.WEEKLY, r.recurrenceType)
        assertTrue(DayOfWeek.TUESDAY in r.weekdays)
        assertEquals(LocalDate.of(2027, 6, 20), r.untilDate)
        assertEquals(60, r.reminderMinutesBefore)
    }

    @Test
    fun horarioPassadoSemDataVaiParaAmanha() {
        val r = parser.parse("fazer almoço 8h30", now)
        assertEquals(LocalDate.of(2026, 8, 30), r.date)
        assertEquals(LocalTime.of(8, 30), r.time)
        assertTrue(r.warnings.isNotEmpty())
    }

    @Test
    fun segundaQuartaSextaViraSemanal() {
        val r = parser.parse("academia segunda quarta e sexta 18h", now)
        assertEquals(RecurrenceType.WEEKLY, r.recurrenceType)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), r.weekdays)
        assertEquals(LocalDate.of(2026, 8, 31), r.date)
    }

    @Test
    fun todosOsDias() {
        val r = parser.parse("tomar remédio todos os dias às 8h", now)
        assertEquals(RecurrenceType.DAILY, r.recurrenceType)
        assertEquals(LocalTime.of(8, 0), r.time)
    }

    @Test
    fun todoDiaCincoViraMensal() {
        val r = parser.parse("pagar aluguel todo dia 5 às 9h", now)
        assertEquals(RecurrenceType.MONTHLY, r.recurrenceType)
        assertEquals(5, r.monthDay)
        assertEquals(LocalDate.of(2026, 9, 5), r.date)
    }

    @Test
    fun diaSemMesGeraAviso() {
        val r = parser.parse("reunião dia 10 às 8h", now)
        assertEquals(LocalDate.of(2026, 9, 10), r.date)
        assertTrue(r.warnings.any { it.contains("Mês não informado") })
    }

    @Test
    fun meiaHoraAntes() {
        val r = parser.parse("buscar filha amanhã 8h. alerta meia hora antes.", now)
        assertEquals(30, r.reminderMinutesBefore)
    }
}
