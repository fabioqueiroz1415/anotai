package com.anotai.app.permission

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.anotai.app.alarm.FullScreenAlertPermissionHelper
import com.anotai.app.alarm.XiaomiPermissionHelper

enum class PermissionStep {
    NOTIFICATION_RUNTIME,
    NOTIFICATION_SETTINGS,
    EXACT_ALARM,
    FULL_SCREEN,
    XIAOMI,
    COMPLETE
}

data class PermissionSnapshot(
    val notificationRuntime: Boolean,
    val notificationsEnabled: Boolean,
    val exactAlarm: Boolean,
    val fullScreen: Boolean,
    val xiaomi: Boolean,
    val xiaomiReviewed: Boolean
)

object PermissionCoordinator {

    private const val PREFS = "permission_coordinator"
    private const val KEY_XIAOMI_REVIEWED = "xiaomi_reviewed"

    const val NOTIFICATION_REQUEST_CODE = 1601

    fun snapshot(
        context: Context
    ): PermissionSnapshot {
        val notificationRuntime =
            Build.VERSION.SDK_INT <
                Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        val notificationsEnabled =
            NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled()

        val exactAlarm =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {
                val manager =
                    context.getSystemService(
                        Context.ALARM_SERVICE
                    ) as AlarmManager

                manager.canScheduleExactAlarms()
            } else {
                true
            }

        val fullScreen =
            FullScreenAlertPermissionHelper
                .canUse(context)

        val xiaomi =
            XiaomiPermissionHelper.isXiaomi()

        val reviewed =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getBoolean(
                    KEY_XIAOMI_REVIEWED,
                    false
                )

        return PermissionSnapshot(
            notificationRuntime = notificationRuntime,
            notificationsEnabled = notificationsEnabled,
            exactAlarm = exactAlarm,
            fullScreen = fullScreen,
            xiaomi = xiaomi,
            xiaomiReviewed = reviewed
        )
    }

    fun nextStep(
        snapshot: PermissionSnapshot,
        includeXiaomi: Boolean = true
    ): PermissionStep =
        when {
            !snapshot.notificationRuntime ->
                PermissionStep.NOTIFICATION_RUNTIME

            !snapshot.notificationsEnabled ->
                PermissionStep.NOTIFICATION_SETTINGS

            !snapshot.exactAlarm ->
                PermissionStep.EXACT_ALARM

            !snapshot.fullScreen ->
                PermissionStep.FULL_SCREEN

            includeXiaomi &&
                snapshot.xiaomi &&
                !snapshot.xiaomiReviewed ->
                PermissionStep.XIAOMI

            else ->
                PermissionStep.COMPLETE
        }

    fun requestNextMissing(
        activity: Activity,
        includeXiaomi: Boolean = true
    ): PermissionStep {
        val step =
            nextStep(
                snapshot(activity),
                includeXiaomi
            )

        when (step) {
            PermissionStep.NOTIFICATION_RUNTIME -> {
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU
                ) {
                    activity.requestPermissions(
                        arrayOf(
                            Manifest.permission.POST_NOTIFICATIONS
                        ),
                        NOTIFICATION_REQUEST_CODE
                    )
                }
            }

            PermissionStep.NOTIFICATION_SETTINGS -> {
                activity.startActivity(
                    Intent(
                        Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    ).apply {
                        putExtra(
                            Settings.EXTRA_APP_PACKAGE,
                            activity.packageName
                        )
                    }
                )
            }

            PermissionStep.EXACT_ALARM -> {
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
                ) {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse(
                                "package:${activity.packageName}"
                            )
                        )
                    )
                }
            }

            PermissionStep.FULL_SCREEN -> {
                FullScreenAlertPermissionHelper
                    .openSettings(activity)
            }

            PermissionStep.XIAOMI -> {
                markXiaomiReviewed(activity)

                XiaomiPermissionHelper
                    .openOtherPermissions(activity)
            }

            PermissionStep.COMPLETE -> Unit
        }

        return step
    }

    fun markXiaomiReviewed(
        context: Context
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_XIAOMI_REVIEWED,
                true
            )
            .apply()
    }

    fun resetXiaomiReview(
        context: Context
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                KEY_XIAOMI_REVIEWED
            )
            .apply()
    }

    fun statusLines(
        context: Context
    ): List<String> {
        val s =
            snapshot(context)

        return buildList {
            add(
                "Notificações: " +
                    if (
                        s.notificationRuntime &&
                        s.notificationsEnabled
                    ) {
                        "OK"
                    } else {
                        "PRECISA CONFIGURAR"
                    }
            )

            add(
                "Alarmes exatos: " +
                    if (s.exactAlarm) {
                        "OK"
                    } else {
                        "PRECISA CONFIGURAR"
                    }
            )

            add(
                "Tela cheia: " +
                    if (s.fullScreen) {
                        "OK"
                    } else {
                        "PRECISA CONFIGURAR"
                    }
            )

            if (s.xiaomi) {
                add(
                    "Xiaomi/HyperOS: " +
                        if (
                            s.xiaomiReviewed
                        ) {
                            "REVISADO"
                        } else {
                            "REVISAR UMA VEZ"
                        }
                )
            }
        }
    }
}
