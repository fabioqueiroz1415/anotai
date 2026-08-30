package com.anotai.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReminderLeadPolicyTest {

    @Test
    fun defaultsHaveRequestedNotifications() {
        assertEquals(
            listOf(120, 30, 15, 5, 0),
            ReminderLeadPolicy.notificationLeads(
                "buscar filha amanhã 8h"
            )
        )
    }

    @Test
    fun oneMinuteIsNotDefaultAnymore() {
        assertFalse(
            ReminderLeadPolicy.notificationLeads(
                "buscar filha amanhã 8h"
            ).contains(1)
        )
    }

    @Test
    fun defaultsHaveOneHourAndEventAlerts() {
        assertEquals(
            listOf(60, 0),
            ReminderLeadPolicy.alertLeads(
                "buscar filha amanhã 8h"
            )
        )
    }

    @Test
    fun everyAlertGetsNotificationThirtyMinutesEarlier() {
        assertEquals(
            listOf(
                PreAlertSpec(90, 60),
                PreAlertSpec(30, 0)
            ),
            ReminderLeadPolicy.preAlertSpecs(
                "buscar filha amanhã 8h"
            )
        )
    }

    @Test
    fun alertWithoutSoAddsToDefaults() {
        assertEquals(
            listOf(120, 60, 5, 0),
            ReminderLeadPolicy.alertLeads(
                "buscar filha 20h alertar 2h antes, 1h antes e 5min antes"
            )
        )
    }

    @Test
    fun soAlertUsesOnlyExplicitAlerts() {
        assertEquals(
            listOf(120, 60, 5),
            ReminderLeadPolicy.alertLeads(
                "buscar filha 20h só alertar 2h antes, 1h antes e 5min antes"
            )
        )
    }

    @Test
    fun notificationWithoutSoAddsToDefaults() {
        assertEquals(
            listOf(180, 120, 30, 15, 5, 0),
            ReminderLeadPolicy.notificationLeads(
                "dentista 14h notificação 3h antes"
            )
        )
    }

    @Test
    fun soNotificationUsesOnlyExplicitNotifications() {
        assertEquals(
            listOf(45, 10),
            ReminderLeadPolicy.notificationLeads(
                "dentista 14h só notificar 45min antes e 10min antes"
            )
        )
    }
}
