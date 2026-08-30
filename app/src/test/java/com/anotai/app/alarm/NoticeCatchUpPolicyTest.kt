package com.anotai.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeCatchUpPolicyTest {

    @Test
    fun agendaNoHorarioNormalQuandoAindaNaoPassou() {
        val now =
            1_000_000L

        val event =
            now +
                3L * 60L * 60L * 1000L

        val plan =
            NoticeCatchUpPolicy.plan(
                eventAtMillis =
                    event,
                minutesBefore =
                    60,
                nowMillis =
                    now
            )

        requireNotNull(plan)

        assertFalse(
            plan.isCatchUp
        )

        assertEquals(
            event -
                60L * 60L * 1000L,
            plan.triggerAtMillis
        )
    }

    @Test
    fun fazCatchUpQuandoHorarioDoAvisoJaPassouMasEventoAindaNao() {
        val now =
            1_000_000L

        val event =
            now +
                30L * 60L * 1000L

        val plan =
            NoticeCatchUpPolicy.plan(
                eventAtMillis =
                    event,
                minutesBefore =
                    60,
                nowMillis =
                    now
            )

        requireNotNull(plan)

        assertTrue(
            plan.isCatchUp
        )

        assertEquals(
            now + 3_000L,
            plan.triggerAtMillis
        )
    }

    @Test
    fun naoAgendaQuandoEventoJaPassou() {
        val now =
            1_000_000L

        assertNull(
            NoticeCatchUpPolicy.plan(
                eventAtMillis =
                    now - 1L,
                minutesBefore =
                    60,
                nowMillis =
                    now
            )
        )
    }

    @Test
    fun naoAgendaQuandoAvisoEstaDesativado() {
        val now =
            1_000_000L

        assertNull(
            NoticeCatchUpPolicy.plan(
                eventAtMillis =
                    now + 60_000L,
                minutesBefore =
                    -1,
                nowMillis =
                    now
            )
        )
    }

    @Test
    fun zeroMinutosAgendaNoHorarioDoEvento() {
        val now =
            1_000_000L

        val event =
            now +
                60_000L

        val plan =
            NoticeCatchUpPolicy.plan(
                eventAtMillis =
                    event,
                minutesBefore =
                    0,
                nowMillis =
                    now
            )

        requireNotNull(plan)

        assertFalse(
            plan.isCatchUp
        )

        assertEquals(
            event,
            plan.triggerAtMillis
        )
    }

    @Test
    fun catchUpNuncaFicaDepoisDoEvento() {
        val now =
            1_000_000L

        val event =
            now +
                1_000L

        val plan =
            NoticeCatchUpPolicy.plan(
                eventAtMillis =
                    event,
                minutesBefore =
                    60,
                nowMillis =
                    now,
                catchUpDelayMillis =
                    3_000L
            )

        requireNotNull(plan)

        assertTrue(
            plan.isCatchUp
        )

        assertTrue(
            plan.triggerAtMillis <
                event
        )
    }
}
