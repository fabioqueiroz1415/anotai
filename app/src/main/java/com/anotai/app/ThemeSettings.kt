package com.anotai.app

import android.content.Context

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true
)

class ThemePreferences(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            "anotai_ui_preferences",
            Context.MODE_PRIVATE
        )

    fun read(): ThemeSettings =
        ThemeSettings(
            mode = ThemeMode.fromStorage(prefs.getString("theme_mode", null)),
            dynamicColor = prefs.getBoolean("dynamic_color", true)
        )

    fun write(settings: ThemeSettings) {
        prefs.edit()
            .putString("theme_mode", settings.mode.storageValue)
            .putBoolean("dynamic_color", settings.dynamicColor)
            .apply()
    }
}
