package com.anotai.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FriendlyDateFormatterTest {

    private val today =
        LocalDate.of(
            2026,
            8,
            30
        )

    @Test
    fun hoje() {
        assertEquals(
            "Hoje | dom | 30.08.26",
            FriendlyDateFormatter.format(
                today,
                today
            )
        )
    }

    @Test
    fun amanha() {
        assertEquals(
            "Amanhã | seg | 31.08.26",
            FriendlyDateFormatter.format(
                today.plusDays(1),
                today
            )
        )
    }

    @Test
    fun depoisDeAmanha() {
        assertEquals(
            "Depois de amanhã | ter | 01.09.26",
            FriendlyDateFormatter.format(
                today.plusDays(2),
                today
            )
        )
    }

    @Test
    fun emTresDiasUsaProx() {
        assertEquals(
            "Próx. qua | 02.09.26",
            FriendlyDateFormatter.format(
                today.plusDays(3),
                today
            )
        )
    }

    @Test
    fun emSeteDiasUsaProx() {
        assertEquals(
            "Próx. dom | 06.09.26",
            FriendlyDateFormatter.format(
                today.plusDays(7),
                today
            )
        )
    }

    @Test
    fun emOitoDiasNaoUsaProx() {
        assertEquals(
            "seg | 07.09.26",
            FriendlyDateFormatter.format(
                today.plusDays(8),
                today
            )
        )
    }

    @Test
    fun dataPassada() {
        assertEquals(
            "sáb | 29.08.26",
            FriendlyDateFormatter.format(
                today.minusDays(1),
                today
            )
        )
    }

    @Test
    fun dataNula() {
        assertEquals(
            "Não informada",
            FriendlyDateFormatter.format(
                null,
                today
            )
        )
    }
}
