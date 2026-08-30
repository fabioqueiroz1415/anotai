package com.anotai.app.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TaskInterpreterNotificationTest {

    private val interpreter =
        TaskInterpreter()

    private val now =
        LocalDateTime.of(
            2026,
            8,
            29,
            19,
            0
        )

    @Test
    fun padroesContinuamDuasHorasEUmaHora() {
        val result =
            interpreter.parse(
                "dentista amanhã 14h",
                now
            )

        assertEquals(
            120,
            result.notificationMinutesBefore
        )

        assertEquals(
            60,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun notificarTresHorasAlteraSomenteNotificacao() {
        val result =
            interpreter.parse(
                "dentista amanhã 14h notificar 3 horas antes",
                now
            )

        assertEquals(
            180,
            result.notificationMinutesBefore
        )

        assertEquals(
            60,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun alertaExplicitoAlteraSomenteAlertaForte() {
        val result =
            interpreter.parse(
                "dentista amanhã 14h alertar 30 minutos antes",
                now
            )

        assertEquals(
            120,
            result.notificationMinutesBefore
        )

        assertEquals(
            30,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun doisTemposPodemVirNaMesmaFrase() {
        val result =
            interpreter.parse(
                "dentista amanhã 14h notificar 3 horas antes e alertar 45 minutos antes",
                now
            )

        assertEquals(
            180,
            result.notificationMinutesBefore
        )

        assertEquals(
            45,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun notificacaoMeiaHoraEhEntendida() {
        val result =
            interpreter.parse(
                "reunião amanhã 18h notificação meia hora antes",
                now
            )

        assertEquals(
            30,
            result.notificationMinutesBefore
        )
    }

    @Test
    fun ordemEstranhaGeraAviso() {
        val result =
            interpreter.parse(
                "reunião amanhã 18h notificar 30 minutos antes",
                now
            )

        assertTrue(
            result.warnings.any {
                it.contains(
                    "depois do alerta forte",
                    ignoreCase = true
                )
            }
        )
    }

    @Test
    fun textoOriginalContinuaExato() {
        val text =
            "Buscar filha amanhã 8h notificar 2h antes e alertar 30 minutos antes."

        val result =
            interpreter.parse(
                text,
                now
            )

        assertEquals(
            text,
            result.originalText
        )
    }
}
