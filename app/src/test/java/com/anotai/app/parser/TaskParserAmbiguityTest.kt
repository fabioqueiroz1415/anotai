package com.anotai.app.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TaskParserAmbiguityTest {

    private val parser = TaskTextParser()
    private val now = LocalDateTime.of(2026, 8, 29, 19, 16)

    @Test
    fun semHorarioGeraAvisoParaLog() {
        val result = parser.parse("buscar documentos amanhã", now)
        assertTrue(result.warnings.any { it.contains("Horário não identificado") })
    }

    @Test
    fun diaSemMesGeraAvisoParaLog() {
        val result = parser.parse("reunião dia 10 às 8h", now)
        assertTrue(result.warnings.any { it.contains("Mês não informado") })
    }

    @Test
    fun horarioSemDataGeraAvisoParaLog() {
        val result = parser.parse("buscar clientes 21h", now)
        assertTrue(result.warnings.any { it.contains("Data não informada") })
    }
}
