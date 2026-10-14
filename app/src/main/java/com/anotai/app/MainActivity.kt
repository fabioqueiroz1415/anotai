package com.anotai.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.anotai.app.alarm.AlarmScheduler
import com.anotai.app.data.TaskRepository
import com.anotai.app.logging.ErrorLogManager
import com.anotai.app.parser.TaskInterpreter

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        val repository = TaskRepository(this)
        val scheduler = AlarmScheduler(this)
        val parser = TaskInterpreter()
        val errorLog = ErrorLogManager(this).also { it.ensureExists() }
        val themePreferences = ThemePreferences(this)
        var themeSettings by mutableStateOf(themePreferences.read())

        setContent {
            AnotaiTheme(settings = themeSettings) {
                ProfessionalAnotaiApp(
                    repository = repository,
                    scheduler = scheduler,
                    parser = parser,
                    errorLog = errorLog,
                    themeSettings = themeSettings,
                    onThemeSettingsChange = { updated ->
                        themeSettings = updated
                        themePreferences.write(updated)
                    }
                )
            }
        }
    }
}
