package com.anotai.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderEditPolicyTest {

    @Test
    fun defaultListsMatchRealSchedulerPolicy() {
        val text =
            "buscar filha 8h"

        assertEquals(
            "2h 30min 15min 5min 0min antes",
            ReminderEditPolicy
                .notificationSummary(text)
        )

        assertEquals(
            "1h 0min antes",
            ReminderEditPolicy
                .alertSummary(text)
        )

        assertEquals(
            120,
            ReminderEditPolicy
                .notificationPrimary(text)
        )

        assertEquals(
            60,
            ReminderEditPolicy
                .alertPrimary(text)
        )
    }

    @Test
    fun customCommandsAreDerivedFromOriginalText() {
        val text =
            "dentista amanhã 14h notificar 3h antes e alertar 2h antes"

        assertEquals(
            180,
            ReminderEditPolicy
                .notificationPrimary(text)
        )

        assertEquals(
            120,
            ReminderEditPolicy
                .alertPrimary(text)
        )
    }

    @Test
    fun disabledCommandsStayDisabled() {
        val text =
            "buscar filha 8h sem notificar e sem alertar"

        assertEquals(
            "Desativado",
            ReminderEditPolicy
                .notificationSummary(text)
        )

        assertEquals(
            "Desativado",
            ReminderEditPolicy
                .alertSummary(text)
        )

        assertEquals(
            -1,
            ReminderEditPolicy
                .notificationPrimary(text)
        )

        assertEquals(
            -1,
            ReminderEditPolicy
                .alertPrimary(text)
        )
    }

    @Test
    fun exclusiveAlertKeepsNotificationDisabled() {
        val text =
            "buscar filha 8h só alerta"

        assertEquals(
            -1,
            ReminderEditPolicy
                .notificationPrimary(text)
        )

        assertEquals(
            60,
            ReminderEditPolicy
                .alertPrimary(text)
        )
    }

    @Test
    fun exclusiveNotificationKeepsAlertDisabled() {
        val text =
            "buscar filha 8h só notificação"

        assertEquals(
            120,
            ReminderEditPolicy
                .notificationPrimary(text)
        )

        assertEquals(
            -1,
            ReminderEditPolicy
                .alertPrimary(text)
        )
    }



    @Test
    fun exclusiveAlertWithExplicitLeadUsesOnlyWrittenLead() {
        val text =
            "buscar filha 8h só alertar 2h antes"

        assertEquals(
            -1,
            ReminderEditPolicy
                .notificationPrimary(text)
        )

        assertEquals(
            120,
            ReminderEditPolicy
                .alertPrimary(text)
        )

        assertEquals(
            "2h antes",
            ReminderEditPolicy
                .alertSummary(text)
        )
    }

    @Test
    fun exclusiveNotificationWithExplicitLeadUsesOnlyWrittenLead() {
        val text =
            "buscar filha 8h só notificar 3h antes"

        assertEquals(
            180,
            ReminderEditPolicy
                .notificationPrimary(text)
        )

        assertEquals(
            -1,
            ReminderEditPolicy
                .alertPrimary(text)
        )

        assertEquals(
            "3h antes",
            ReminderEditPolicy
                .notificationSummary(text)
        )
    }


}
