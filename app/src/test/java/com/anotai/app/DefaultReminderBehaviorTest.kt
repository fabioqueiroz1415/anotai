package com.anotai.app

import com.anotai.app.parser.TaskTextParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DefaultReminderBehaviorTest {

    private val parser =
        TaskTextParser()

    private val now =
        LocalDateTime.of(
            2026,
            8,
            29,
            19,
            0
        )

    @Test
    fun alertaPadraoEhUmaHora() {
        val result =
            parser.parse(
                "dentista amanhã 14h",
                now
            )

        assertEquals(
            60,
            result.reminderMinutesBefore
        )
    }

    @Test
    fun notificacaoPadraoEhDuasHoras() {
        val result =
            parser.parse(
                "dentista amanhã 14h",
                now
            )

        assertEquals(
            120,
            result.notificationMinutesBefore
        )
    }

    @Test
    fun alertaExplicitoSubstituiUmaHora() {
        val result =
            parser.parse(
                "dentista amanhã 14h alertar 30 minutos antes",
                now
            )

        assertEquals(
            30,
            result.reminderMinutesBefore
        )

        assertEquals(
            120,
            result.notificationMinutesBefore
        )
    }
}
