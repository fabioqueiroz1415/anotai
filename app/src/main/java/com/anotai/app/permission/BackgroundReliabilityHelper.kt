package com.anotai.app.permission

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class BackgroundReliabilityStatus(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
    val fullScreenAllowed: Boolean,
    val ignoringAndroidBatteryOptimizations: Boolean,
    val isXiaomiFamily: Boolean
) {
    val androidCriticalReady: Boolean
        get() =
            notificationsAllowed &&
                exactAlarmsAllowed &&
                fullScreenAllowed
}

object BackgroundReliabilityHelper {

    fun read(
        context: Context
    ): BackgroundReliabilityStatus {
        val notificationPermission =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        val notificationsEnabled =
            NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled()

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val exactAllowed =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

        val fullScreenAllowed =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                val manager =
                    context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                manager.canUseFullScreenIntent()
            } else {
                true
            }

        val powerManager =
            context.getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager

        val ignoringBattery =
            powerManager
                .isIgnoringBatteryOptimizations(
                    context.packageName
                )

        return BackgroundReliabilityStatus(
            notificationsAllowed =
                notificationPermission &&
                    notificationsEnabled,
            exactAlarmsAllowed =
                exactAllowed,
            fullScreenAllowed =
                fullScreenAllowed,
            ignoringAndroidBatteryOptimizations =
                ignoringBattery,
            isXiaomiFamily =
                isXiaomiFamily()
        )
    }

    fun openNotificationSettings(
        context: Context
    ) {
        val intent =
            Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).apply {
                putExtra(
                    Settings.EXTRA_APP_PACKAGE,
                    context.packageName
                )
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        safeStart(
            context,
            intent
        )
    }

    fun openExactAlarmSettings(
        context: Context
    ) {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            val intent =
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse(
                        "package:${context.packageName}"
                    )
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            safeStart(
                context,
                intent
            )
        }
    }

    fun openFullScreenSettings(
        context: Context
    ) {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse(
                        "package:${context.packageName}"
                    )
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            safeStart(
                context,
                intent
            )
        }
    }

    fun openAndroidBatteryOptimizationSettings(
        context: Context
    ) {
        val intent =
            Intent(
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        safeStart(
            context,
            intent
        )
    }

    fun openXiaomiAutostart(
        context: Context
    ): Boolean {
        val candidates =
            listOf(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ),
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
            )

        for (
            component in candidates
        ) {
            val intent =
                Intent().apply {
                    this.component =
                        component

                    putExtra(
                        "extra_pkgname",
                        context.packageName
                    )

                    putExtra(
                        "package_name",
                        context.packageName
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            if (
                canResolve(
                    context,
                    intent
                ) &&
                runCatching {
                    context.startActivity(
                        intent
                    )
                }.isSuccess
            ) {
                return true
            }
        }

        openAppDetails(
            context
        )

        return false
    }

    fun openXiaomiBattery(
        context: Context
    ): Boolean {
        val candidates =
            listOf(
                ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                ),
                ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"
                )
            )

        for (
            component in candidates
        ) {
            val intent =
                Intent().apply {
                    this.component =
                        component

                    putExtra(
                        "package_name",
                        context.packageName
                    )

                    putExtra(
                        "package_label",
                        "Anotaí"
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            if (
                canResolve(
                    context,
                    intent
                ) &&
                runCatching {
                    context.startActivity(
                        intent
                    )
                }.isSuccess
            ) {
                return true
            }
        }

        openAppDetails(
            context
        )

        return false
    }

    fun openAppDetails(
        context: Context
    ) {
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse(
                    "package:${context.packageName}"
                )
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        safeStart(
            context,
            intent
        )
    }

    private fun isXiaomiFamily(): Boolean {
        val value =
            listOf(
                Build.MANUFACTURER,
                Build.BRAND
            )
                .joinToString(
                    " "
                )
                .lowercase()

        return value.contains(
            "xiaomi"
        ) ||
            value.contains(
                "redmi"
            ) ||
            value.contains(
                "poco"
            )
    }

    private fun canResolve(
        context: Context,
        intent: Intent
    ): Boolean =
        intent.resolveActivity(
            context.packageManager
        ) != null

    private fun safeStart(
        context: Context,
        intent: Intent
    ) {
        runCatching {
            context.startActivity(
                intent
            )
        }.onFailure {
            openAppDetails(
                context
            )
        }
    }
}
