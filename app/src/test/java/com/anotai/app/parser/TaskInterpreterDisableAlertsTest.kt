package com.anotai.app.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TaskInterpreterDisableAlertsTest {

    private val interpreter =
        TaskInterpreter()

    private val now =
        LocalDateTime.of(
            2026,
            8,
            30,
            0,
            0
        )

    private fun parse(
        suffix: String
    ) =
        interpreter.parse(
            "dentista amanhã 14h $suffix",
            now
        )

    @Test
    fun naoAlertarDesativaAlerta() {
        assertEquals(
            -1,
            parse("não alertar")
                .reminderMinutesBefore
        )
    }

    @Test
    fun nAlertarDesativaAlerta() {
        assertEquals(
            -1,
            parse("n alertar")
                .reminderMinutesBefore
        )
    }

    @Test
    fun semAlertaDesativaAlerta() {
        assertEquals(
            -1,
            parse("sem alerta")
                .reminderMinutesBefore
        )
    }

    @Test
    fun semAlertarDesativaAlerta() {
        assertEquals(
            -1,
            parse("sem alertar")
                .reminderMinutesBefore
        )
    }

    @Test
    fun soAlertaDesativaNotificacao() {
        val result =
            parse("só alerta")

        assertEquals(
            -1,
            result.notificationMinutesBefore
        )

        assertEquals(
            60,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun somenteAlertaPodeTerTempo() {
        val result =
            parse(
                "somente alerta e alertar 30 minutos antes"
            )

        assertEquals(
            -1,
            result.notificationMinutesBefore
        )

        assertEquals(
            30,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun naoNotificarDesativaNotificacao() {
        assertEquals(
            -1,
            parse("não notificar")
                .notificationMinutesBefore
        )
    }

    @Test
    fun nNotificarDesativaNotificacao() {
        assertEquals(
            -1,
            parse("n notificar")
                .notificationMinutesBefore
        )
    }

    @Test
    fun semNotificacaoDesativaNotificacao() {
        assertEquals(
            -1,
            parse("sem notificação")
                .notificationMinutesBefore
        )
    }

    @Test
    fun semNotifDesativaNotificacao() {
        assertEquals(
            -1,
            parse("sem notif")
                .notificationMinutesBefore
        )
    }

    @Test
    fun soNotificacaoDesativaAlerta() {
        val result =
            parse("só notificação")

        assertEquals(
            120,
            result.notificationMinutesBefore
        )

        assertEquals(
            -1,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun soNotificacaoPodeTerTempo() {
        val result =
            parse(
                "apenas notificação e notificar 3 horas antes"
            )

        assertEquals(
            180,
            result.notificationMinutesBefore
        )

        assertEquals(
            -1,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun conflitoGeraAviso() {
        val result =
            parse(
                "só alerta e notificar 2 horas antes"
            )

        assertEquals(
            -1,
            result.notificationMinutesBefore
        )

        assertTrue(
            result.warnings.any {
                it.contains(
                    "somente alerta",
                    ignoreCase = true
                )
            }
        )
    }

    @Test
    fun textoOriginalContinuaIntegral() {
        val text =
            "Buscar filha amanhã 8h sem notificação e alertar 30 minutos antes."

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
