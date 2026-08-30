package com.anotai.app.alarm

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object FullScreenAlertPermissionHelper {

    fun canUse(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) {
            return true
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        return manager.canUseFullScreenIntent()
    }

    fun statusLabel(context: Context): String =
        if (canUse(context)) {
            "Tela cheia Android: PERMITIDA"
        } else {
            "Tela cheia Android: NÃO PERMITIDA"
        }

    fun openSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < 34) {
            return
        }

        val intent = Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.parse(
                "package:${activity.packageName}"
            )
        )

        runCatching {
            activity.startActivity(intent)
        }.recoverCatching {
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse(
                        "package:${activity.packageName}"
                    )
                )
            )
        }
    }
}
