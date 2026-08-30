package com.anotai.app.alarm

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object AlarmPermissionHelper {
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return manager.canScheduleExactAlarms()
    }

    fun openExactAlarmSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${activity.packageName}")
        )

        runCatching {
            activity.startActivity(intent)
        }.recoverCatching {
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${activity.packageName}")
                )
            )
        }
    }
}
