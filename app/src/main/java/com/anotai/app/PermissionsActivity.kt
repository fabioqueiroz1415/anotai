package com.anotai.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anotai.app.permission.BackgroundReliabilityHelper
import com.anotai.app.permission.BackgroundReliabilityStatus

class PermissionsActivity :
    ComponentActivity() {

    private var refreshVersion
        by mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) {
            refreshVersion++
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContent {
            AnotaiTheme {
                PermissionReliabilityScreen(
                    refreshVersion =
                        refreshVersion,
                    onRequestNotification = {
                        requestNotification()
                    },
                    onOpenExactAlarm = {
                        BackgroundReliabilityHelper
                            .openExactAlarmSettings(
                                this
                            )
                    },
                    onOpenFullScreen = {
                        BackgroundReliabilityHelper
                            .openFullScreenSettings(
                                this
                            )
                    },
                    onOpenBattery = {
                        BackgroundReliabilityHelper
                            .openAndroidBatteryOptimizationSettings(
                                this
                            )
                    },
                    onOpenXiaomiAutostart = {
                        val direct =
                            BackgroundReliabilityHelper
                                .openXiaomiAutostart(
                                    this
                                )

                        if (!direct) {
                            toast(
                                "Abra Inicialização automática e ative Anotaí."
                            )
                        }
                    },
                    onOpenXiaomiBattery = {
                        val direct =
                            BackgroundReliabilityHelper
                                .openXiaomiBattery(
                                    this
                                )

                        if (!direct) {
                            toast(
                                "Abra Bateria e selecione Sem restrições."
                            )
                        }
                    },
                    onRefresh = {
                        refreshVersion++
                    },
                    onBack = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        refreshVersion++
    }

    private fun requestNotification() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermission.launch(
                Manifest.permission
                    .POST_NOTIFICATIONS
            )
        } else {
            BackgroundReliabilityHelper
                .openNotificationSettings(
                    this
                )
        }
    }

    private fun toast(
        text: String
    ) {
        Toast
            .makeText(
                this,
                text,
                Toast.LENGTH_LONG
            )
            .show()
    }

    @Composable
    private fun PermissionReliabilityScreen(
        refreshVersion: Int,
        onRequestNotification: () -> Unit,
        onOpenExactAlarm: () -> Unit,
        onOpenFullScreen: () -> Unit,
        onOpenBattery: () -> Unit,
        onOpenXiaomiAutostart: () -> Unit,
        onOpenXiaomiBattery: () -> Unit,
        onRefresh: () -> Unit,
        onBack: () -> Unit
    ) {
        val status =
            BackgroundReliabilityHelper
                .read(this)

        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                Text(
                    "Permissões e funcionamento em segundo plano",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                Text(
                    "Para a notificação e o alerta forte funcionarem mesmo com o app fechado, revise todos os itens abaixo."
                )

                OverallStatus(
                    status
                )

                PermissionItem(
                    title =
                        "Notificações",
                    ok =
                        status
                            .notificationsAllowed,
                    detailOk =
                        "Permitidas",
                    detailMissing =
                        "Falta permitir notificações",
                    button =
                        "Configurar",
                    onClick =
                        onRequestNotification
                )

                PermissionItem(
                    title =
                        "Alarmes exatos",
                    ok =
                        status
                            .exactAlarmsAllowed,
                    detailOk =
                        "Permitidos",
                    detailMissing =
                        "Necessário para tocar no horário correto",
                    button =
                        "Configurar",
                    onClick =
                        onOpenExactAlarm
                )

                PermissionItem(
                    title =
                        "Alerta em tela cheia",
                    ok =
                        status
                            .fullScreenAllowed,
                    detailOk =
                        "Permitido",
                    detailMissing =
                        "Necessário para o alerta forte",
                    button =
                        "Configurar",
                    onClick =
                        onOpenFullScreen
                )

                PermissionItem(
                    title =
                        "Otimização de bateria do Android",
                    ok =
                        status
                            .ignoringAndroidBatteryOptimizations,
                    detailOk =
                        "App fora da otimização de bateria",
                    detailMissing =
                        "Pode restringir funcionamento em segundo plano",
                    button =
                        "Abrir bateria",
                    onClick =
                        onOpenBattery
                )

                if (
                    status.isXiaomiFamily
                ) {
                    XiaomiManualItem(
                        title =
                            "Xiaomi: Inicialização automática em segundo plano",
                        instruction =
                            "Deixe Anotaí LIGADO.",
                        button =
                            "ABRIR INICIALIZAÇÃO AUTOMÁTICA",
                        onClick =
                            onOpenXiaomiAutostart
                    )

                    XiaomiManualItem(
                        title =
                            "Xiaomi: Bateria → Sem restrições",
                        instruction =
                            "Selecione Sem restrições para Anotaí.",
                        button =
                            "Abrir bateria do Xiaomi",
                        onClick =
                            onOpenXiaomiBattery
                    )

                    Text(
                        "Os dois controles acima são do HyperOS/MIUI. O Xiaomi não fornece uma API pública confiável para o app confirmar automaticamente esses dois estados; por isso eles precisam ser conferidos na tela do sistema."
                    )
                }

                Button(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    onClick =
                        onRefresh
                ) {
                    Text(
                        "Verificar novamente"
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    onClick =
                        onBack
                ) {
                    Text(
                        "Voltar"
                    )
                }

                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                Text(
                    "A verificação também é atualizada automaticamente quando você volta das configurações.",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                Text(
                    "Atualização interna: $refreshVersion",
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )
            }
        }
    }

    @Composable
    private fun OverallStatus(
        status: BackgroundReliabilityStatus
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(14.dp)
            ) {
                Text(
                    if (
                        status.androidCriticalReady
                    ) {
                        "Android: principais permissões OK"
                    } else {
                        "Android: ainda há itens para configurar"
                    },
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                if (
                    status.isXiaomiFamily
                ) {
                    Text(
                        "HyperOS: confira também Inicialização automática e Bateria sem restrições."
                    )
                }
            }
        }
    }

    @Composable
    private fun PermissionItem(
        title: String,
        ok: Boolean,
        detailOk: String,
        detailMissing: String,
        button: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(14.dp),
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                Text(
                    if (ok) {
                        "✅ $title"
                    } else {
                        "⚠️ $title"
                    },
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    if (ok) {
                        detailOk
                    } else {
                        detailMissing
                    }
                )

                if (!ok) {
                    OutlinedButton(
                        onClick =
                            onClick
                    ) {
                        Text(
                            button
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun XiaomiManualItem(
        title: String,
        instruction: String,
        button: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(14.dp),
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                Text(
                    "⚠️ $title",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    instruction
                )

                Text(
                    "Verificação manual do HyperOS"
                )

                OutlinedButton(
                    onClick =
                        onClick
                ) {
                    Text(
                        button
                    )
                }
            }
        }
    }
}
