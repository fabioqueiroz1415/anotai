package com.anotai.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TestAlarmScheduler {
    fun scheduleInTenSeconds(context: Context) {
        val manager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val pi =
            PendingIntent.getBroadcast(
                context,
                991001,
                Intent(
                    context,
                    TestAlarmReceiver::class.java
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val trigger =
            System.currentTimeMillis() + 10_000L

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !manager.canScheduleExactAlarms()
        ) {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger,
                pi
            )
        } else {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger,
                pi
            )
        }
    }
}
