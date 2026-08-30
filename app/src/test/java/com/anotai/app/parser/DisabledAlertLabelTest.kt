package com.anotai.app.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DisabledAlertLabelTest {

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

    @Test
    fun semAlertaMostraDesativado() {
        val result =
            interpreter.parse(
                "buscar clientes toda terça 20h sem alerta até 30 de dezembro",
                now
            )

        assertEquals(
            -1,
            result.reminderMinutesBefore
        )

        assertEquals(
            "Desativado",
            result.reminderLabel()
        )
    }

    @Test
    fun semAlertaNaoPerdeDataFinal() {
        val result =
            interpreter.parse(
                "buscar clientes toda terça 20h sem alerta até 30 de dezembro",
                now
            )

        assertEquals(
            12,
            result.untilDate?.monthValue
        )

        assertEquals(
            30,
            result.untilDate?.dayOfMonth
        )
    }
}
