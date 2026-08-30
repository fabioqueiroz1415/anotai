package com.anotai.app.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val exactChanged =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                intent.action ==
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED

        val shouldReschedule =
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                exactChanged

        if (!shouldReschedule) return

        if (
            exactChanged &&
            !AlarmPermissionHelper.canScheduleExact(context)
        ) {
            return
        }

        AlarmScheduler(context).rescheduleAll()
    }
}
