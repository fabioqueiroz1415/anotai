package com.anotai.app.alarm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object XiaomiPermissionHelper {

    fun isXiaomi(): Boolean {
        return Build.MANUFACTURER.equals(
            "Xiaomi",
            ignoreCase = true
        )
    }

    fun openOtherPermissions(activity: Activity) {
        val miuiIntent =
            Intent(
                "miui.intent.action.APP_PERM_EDITOR"
            ).apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra(
                    "extra_pkgname",
                    activity.packageName
                )
            }

        runCatching {
            activity.startActivity(miuiIntent)
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
