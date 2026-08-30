package com.anotai.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anotai.app.alarm.AlarmPermissionHelper
import com.anotai.app.alarm.AlarmScheduler
import com.anotai.app.alarm.FullScreenAlertPermissionHelper
import com.anotai.app.alarm.TestAlarmScheduler
import com.anotai.app.alarm.XiaomiPermissionHelper
import com.anotai.app.calendar.TaskOccurrenceCalculator
import com.anotai.app.data.TaskRecord
import com.anotai.app.data.TaskRepository
import com.anotai.app.logging.ErrorLogManager
import com.anotai.app.model.RecurrenceType
import com.anotai.app.model.TaskDraft
import com.anotai.app.parser.TaskInterpreter
import com.anotai.app.permission.PermissionCoordinator
import com.anotai.app.util.FriendlyDateFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.itemsIndexed

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
        val errorLog = ErrorLogManager(this)
        errorLog.ensureExists()

        setContent {
            com.anotai.app.AnotaiTheme {
                Surface(Modifier.fillMaxSize()) {
                    AnotaiApp(
                        repository = repository,
                        scheduler = scheduler,
                        parser = parser,
                        errorLog = errorLog
                    )
                }
            }
        }
    }
}

private enum class Screen {
    INPUT,
    CALENDAR,
    CONFIRM_NEW,
    EDIT_NEW,
    DETAIL,
    EDIT_EXISTING,
    CONFIRM_EXISTING,
    CONFIRM_DELETE
}

@Composable
private fun AnotaiApp(
    repository: TaskRepository,
    scheduler: AlarmScheduler,
    parser: TaskInterpreter,
    errorLog: ErrorLogManager
) {
    val context = LocalContext.current

    var screen by remember {
        mutableStateOf(Screen.CALENDAR)
    }

    var input by rememberSaveable {
        mutableStateOf("")
    }

    var initialComposerPending by rememberSaveable {
        mutableStateOf(true)
    }

    var draft by remember {
        mutableStateOf<TaskDraft?>(null)
    }

    var tasks by remember {
        mutableStateOf(repository.all())
    }

    var selectedTaskId by remember {
        mutableStateOf<Long?>(null)
    }

    var status by remember {
        mutableStateOf<String?>(null)
    }

    var lastBackAt by remember {
        mutableStateOf(0L)
    }

    var detailReturnScreen by remember {
        mutableStateOf(Screen.CALENDAR)
    }

    fun refresh() {
        tasks = repository.all()
    }

    fun selectedTask(): TaskRecord? =
        selectedTaskId?.let(repository::get)

    BackHandler {
        when (screen) {
            Screen.CALENDAR -> {
                val now =
                    System.currentTimeMillis()

                if (
                    now - lastBackAt <=
                    2_000L
                ) {
                    (context as? android.app.Activity)
                        ?.finish()
                } else {
                    lastBackAt = now

                    Toast.makeText(
                        context,
                        "Pressione voltar novamente para sair",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            Screen.INPUT ->
                screen = Screen.CALENDAR

            Screen.CONFIRM_NEW ->
                screen = Screen.CALENDAR

            Screen.EDIT_NEW ->
                screen = Screen.CALENDAR

            Screen.DETAIL ->
                screen = detailReturnScreen

            Screen.EDIT_EXISTING ->
                screen = Screen.DETAIL

            Screen.CONFIRM_EXISTING ->
                screen = Screen.EDIT_EXISTING

            Screen.CONFIRM_DELETE ->
                screen = Screen.DETAIL
        }
    }

    GlobalAppShell(
        onOpenSavedTasks = {
            refresh()
            screen = Screen.INPUT
        },
        onOpenPermissions = {
            context.startActivity(
                Intent(
                    context,
                    PermissionsActivity::class.java
                )
            )
        },
        onOpenReadMe = {
            context.startActivity(
                Intent(
                    context,
                    ReadMeActivity::class.java
                )
            )
        },
        onTestAlert = {
            TestAlarmScheduler
                .scheduleInTenSeconds(
                    context
                )

            status =
                "Teste programado para 10 segundos."
        },
        onExportErrors = {
            errorLog.share()
        }
    ) {
    when (screen) {
        Screen.INPUT -> {
            SavedTasksScreen(
                tasks = tasks,
                status = status,
                onOpenTask = { id ->
                    selectedTaskId = id
                    detailReturnScreen =
                        Screen.INPUT
                    status = null
                    screen = Screen.DETAIL
                }
            )
        }

        Screen.CALENDAR -> {
            CalendarScreen(
                tasks = tasks,
                text = input,
                onTextChange = {
                    input = it
                },
                status = status,
                autoOpenComposer =
                    initialComposerPending,
                onAutoOpenConsumed = {
                    initialComposerPending =
                        false
                },
                onConfirmReminder = {
                    if (input.isNotBlank()) {
                        try {
                            val parsed =
                                parser.parse(input)

                            if (
                                parsed.date == null ||
                                parsed.time == null
                            ) {
                                status =
                                    "Informe data e horário para confirmar."
                            } else {
                                errorLog
                                    .recordInterpretationProblem(
                                        parsed
                                    )

                                val id =
                                    repository.save(
                                        parsed
                                    )

                                scheduler.scheduleNext(
                                    id
                                )

                                refresh()
                                input = ""
                                draft = null
                                status =
                                    "Tarefa salva. Notificação e alerta foram programados."
                            }
                        } catch (t: Throwable) {
                            errorLog
                                .recordException(
                                    input,
                                    t
                                )

                            status =
                                "Não consegui salvar esta tarefa. O erro foi registrado."
                        }
                    }
                },
                onOpenTask = { id ->
                    selectedTaskId = id
                    detailReturnScreen =
                        Screen.CALENDAR
                    status = null
                    screen =
                        Screen.DETAIL
                },
                onOpenSavedTasks = {
                    refresh()
                    screen =
                        Screen.INPUT
                },
                onOpenPermissions = {
                    context.startActivity(
                        Intent(
                            context,
                            PermissionsActivity::class.java
                        )
                    )
                },
                onOpenReadMe = {
                    context.startActivity(
                        Intent(
                            context,
                            ReadMeActivity::class.java
                        )
                    )
                },
                onTestAlert = {
                    TestAlarmScheduler
                        .scheduleInTenSeconds(
                            context
                        )

                    status =
                        "Teste programado para 10 segundos."
                },
                onExportErrors = {
                    errorLog.share()
                }
            )
        }

        Screen.CONFIRM_NEW -> {
            val current = draft

            if (current == null) {
                screen = Screen.INPUT
            } else {
                ConfirmationScreen(
                    title = "Confirme antes de salvar",
                    draft = current,
                    confirmText = "CONFIRMAR",
                    onEdit = {
                        screen = Screen.EDIT_NEW
                    },
                    onCancel = {
                        draft = null
                        status =
                            "Tarefa cancelada. Nada foi salvo e nenhum aviso foi criado."
                        screen = Screen.INPUT
                    },
                    onConfirm = {
                        val id = repository.save(current)
                        scheduler.scheduleNext(id)
                        refresh()
                        input = ""
                        draft = null
                        status =
                            "Tarefa salva. Notificação e alerta foram programados."
                        screen = Screen.INPUT
                    }
                )
            }
        }

        Screen.EDIT_NEW -> {
            val current = draft

            if (current == null) {
                screen = Screen.INPUT
            } else {
                EditScreen(
                    title = "Editar interpretação",
                    draft = current,
                    parser = parser,
                    onBack = {
                        screen = Screen.CONFIRM_NEW
                    },
                    onReview = {
                        draft = it
                        screen = Screen.CONFIRM_NEW
                    }
                )
            }
        }

        Screen.DETAIL -> {
            val task = selectedTask()

            if (task == null) {
                selectedTaskId = null
                refresh()
                screen = Screen.INPUT
            } else {
                TaskDetailScreen(
                    task = task,
                    onBack = {
                        selectedTaskId = null
                        refresh()
                        screen =
                            detailReturnScreen
                    },
                    onEdit = {
                        draft = task.toDraft()
                        screen = Screen.EDIT_EXISTING
                    },
                    onDelete = {
                        screen = Screen.CONFIRM_DELETE
                    }
                )
            }
        }

        Screen.EDIT_EXISTING -> {
            val current = draft

            if (
                current == null ||
                selectedTaskId == null
            ) {
                screen = Screen.INPUT
            } else {
                EditScreen(
                    title = "Editar tarefa salva",
                    draft = current,
                    parser = parser,
                    onBack = {
                        draft = null
                        screen = Screen.DETAIL
                    },
                    onReview = {
                        draft = it
                        screen = Screen.CONFIRM_EXISTING
                    }
                )
            }
        }

        Screen.CONFIRM_EXISTING -> {
            val current = draft
            val id = selectedTaskId

            if (
                current == null ||
                id == null
            ) {
                screen = Screen.INPUT
            } else {
                ConfirmationScreen(
                    title = "Confirme as alterações",
                    draft = current,
                    confirmText = "CONFIRMAR ALTERAÇÕES",
                    onEdit = {
                        screen = Screen.EDIT_EXISTING
                    },
                    onCancel = {
                        draft = null
                        screen = Screen.DETAIL
                    },
                    onConfirm = {
                        if (repository.update(id, current)) {
                            scheduler.replace(id)
                            refresh()
                            draft = null
                            status =
                                "Tarefa atualizada e avisos reagendados."
                            screen = Screen.DETAIL
                        } else {
                            status =
                                "Não consegui atualizar a tarefa."
                            screen = Screen.DETAIL
                        }
                    }
                )
            }
        }

        Screen.CONFIRM_DELETE -> {
            val task = selectedTask()

            if (task == null) {
                screen = Screen.INPUT
            } else {
                DeleteConfirmationScreen(
                    task = task,
                    onCancel = {
                        screen = Screen.DETAIL
                    },
                    onDelete = {
                        scheduler.cancel(task.id)
                        repository.delete(task.id)
                        selectedTaskId = null
                        draft = null
                        refresh()
                        status =
                            "Tarefa excluída e avisos cancelados."
                        screen = Screen.INPUT
                    }
                )
            }
        }
    }
    }
}

@Composable
private fun GlobalAppShell(
    onOpenSavedTasks: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenReadMe: () -> Unit,
    onTestAlert: () -> Unit,
    onExportErrors: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            ),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Text(
                        "Anotaí",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        modifier =
                            Modifier.padding(
                                top = 10.dp
                            )
                    )

                    GlobalOverflowMenu(
                        onOpenSavedTasks =
                            onOpenSavedTasks,
                        onOpenPermissions =
                            onOpenPermissions,
                        onOpenReadMe =
                            onOpenReadMe,
                        onTestAlert =
                            onTestAlert,
                        onExportErrors =
                            onExportErrors
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
        ) {
            content()
        }
    }
}


@Composable
private fun GlobalOverflowMenu(
    onOpenSavedTasks: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenReadMe: () -> Unit,
    onTestAlert: () -> Unit,
    onExportErrors: () -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    androidx.compose.foundation.layout.Box {
        TextButton(
            onClick = {
                expanded = true
            },
            modifier =
                Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                )
        ) {
            Text(
                "☰",
                style =
                    MaterialTheme
                        .typography
                        .displaySmall
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Tarefas salvas") },
                onClick = {
                    expanded = false
                    onOpenSavedTasks()
                }
            )

            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Permissões") },
                onClick = {
                    expanded = false
                    onOpenPermissions()
                }
            )

            androidx.compose.material3.DropdownMenuItem(
                text = { Text("LEIA-ME") },
                onClick = {
                    expanded = false
                    onOpenReadMe()
                }
            )

            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Text(
                        "Testar alerta em 10 segundos"
                    )
                },
                onClick = {
                    expanded = false
                    onTestAlert()
                }
            )

            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Exportar erros") },
                onClick = {
                    expanded = false
                    onExportErrors()
                }
            )
        }
    }
}


@Composable
private fun SavedTasksScreen(
    tasks: List<TaskRecord>,
    status: String?,
    onOpenTask: (Long) -> Unit
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Tarefas salvas",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )
        }

        status?.let { message ->
            item {
                Text(message)
            }
        }

        if (tasks.isEmpty()) {
            item {
                Text(
                    "Nenhuma tarefa salva."
                )
            }
        } else {
            items(
                items = tasks,
                key = { it.id }
            ) { task ->
                TaskCard(
                    task = task,
                    onClick = {
                        onOpenTask(task.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun InputScreen(
    text: String,
    onTextChange: (String) -> Unit,
    tasks: List<TaskRecord>,
    status: String?,
    fullScreenStatus: String,
    isXiaomi: Boolean,
    onInterpret: () -> Unit,
    onOpenReadMe: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenPermissions: () -> Unit,
    onConfigureAlerts: () -> Unit,
    onConfigureFullScreen: () -> Unit,
    onConfigureXiaomi: () -> Unit,
    onTestAlert: () -> Unit,
    onExportErrors: () -> Unit,
    onOpenTask: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Anotaí",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Text(
                "Escreva naturalmente. Ex.: buscar filha amanhã 8h e alertar 1 hora antes",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            Text(
                "Padrão: notificação 2h antes • alerta forte 1h antes",
                style = MaterialTheme.typography.labelMedium
            )
        }

        item {
            Text(
                fullScreenStatus,
                style = MaterialTheme.typography.labelLarge
            )
        }

        item {
        ReminderEntryLauncher(
            text = text,
            onTextChange = onTextChange,
            onContinue = onInterpret
        )


        }

        item {
            OutlinedButton(
                onClick = onOpenReadMe,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LEIA-ME")
            }
        }

        item {
            OutlinedButton(
                onClick = onOpenCalendar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CALENDÁRIO")
            }
        }

        item {
            }

        item {
            }

        if (isXiaomi) {
            item {
                    }
        }

        item {
            OutlinedButton(
                onClick = onTestAlert,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TESTAR ALERTA EM 10 SEGUNDOS")
            }
        }

        item {
            OutlinedButton(
            onClick =
                onOpenPermissions,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("PERMISSÕES")
        }

        Spacer(
            Modifier.height(8.dp)
        )

        OutlinedButton(
                onClick = onExportErrors,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("EXPORTAR ERROS")
            }
        }

        status?.let { message ->
            item {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Tarefas salvas",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (tasks.isEmpty()) {
            item {
                Text("Nenhuma tarefa salva.")
            }
        } else {
            items(
                items = tasks,
                key = { it.id }
            ) { task ->
                TaskCard(
                    task = task,
                    onClick = {
                        onOpenTask(task.id)
                    }
                )
            }
        }
    
        item {
            AppVersionFooter()
        }
}
}

@Composable
private fun CalendarScreen(
    tasks: List<TaskRecord>,
    text: String,
    onTextChange: (String) -> Unit,
    status: String?,
    autoOpenComposer: Boolean,
    onAutoOpenConsumed: () -> Unit,
    onConfirmReminder: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenSavedTasks: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenReadMe: () -> Unit,
    onTestAlert: () -> Unit,
    onExportErrors: () -> Unit
) {
    var month by remember {
        mutableStateOf(
            YearMonth.now()
        )
    }

    var selectedDate by remember {
        mutableStateOf(
            LocalDate.now()
        )
    }

    val monthTitle =
        month.format(
            DateTimeFormatter.ofPattern(
                "MMMM 'de' yyyy"
            )
        )

    val firstDay =
        month.atDay(1)

    val firstColumn =
        firstDay.dayOfWeek.value - 1

    val daysInMonth =
        month.lengthOfMonth()

    val cells =
        buildList<LocalDate?> {
            repeat(firstColumn) {
                add(null)
            }

            for (day in 1..daysInMonth) {
                add(month.atDay(day))
            }

            while (size % 7 != 0) {
                add(null)
            }
        }

    val now =
        java.time.LocalDateTime.now()

    val selectedTasks =
        tasks
            .filter {
                TaskOccurrenceCalculator
                    .occursOn(
                        it,
                        selectedDate
                    )
            }
            .sortedWith(
                Comparator { a, b ->
                    val aTime = a.time
                    val bTime = b.time

                    when {
                        aTime == null &&
                            bTime == null -> 0

                        aTime == null -> 1

                        bTime == null -> -1

                        else -> {
                            val aOccurrence =
                                selectedDate.atTime(
                                    aTime
                                )

                            val bOccurrence =
                                selectedDate.atTime(
                                    bTime
                                )

                            val aPassed =
                                aOccurrence.isBefore(
                                    now
                                )

                            val bPassed =
                                bOccurrence.isBefore(
                                    now
                                )

                            when {
                                aPassed != bPassed ->
                                    if (aPassed) 1 else -1

                                !aPassed ->
                                    aOccurrence.compareTo(
                                        bOccurrence
                                    )

                                else ->
                                    bOccurrence.compareTo(
                                        aOccurrence
                                    )
                            }
                        }
                    }
                }
            )

    androidx.compose.foundation.layout.Box(
        modifier =
            Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            month =
                                month.minusMonths(1)
                            selectedDate =
                                month.atDay(1)
                        }
                    ) {
                        Text("‹")
                    }

                    Text(
                        monthTitle.replaceFirstChar {
                            if (it.isLowerCase()) {
                                it.titlecase()
                            } else {
                                it.toString()
                            }
                        },
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    OutlinedButton(
                        onClick = {
                            month =
                                month.plusMonths(1)
                            selectedDate =
                                month.atDay(1)
                        }
                    ) {
                        Text("›")
                    }
                }
            }

            item {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "Seg",
                        "Ter",
                        "Qua",
                        "Qui",
                        "Sex",
                        "Sáb",
                        "Dom"
                    ).forEach { label ->
                        Text(
                            text = label,
                            modifier =
                                Modifier.weight(1f),
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium
                        )
                    }
                }
            }

            items(
                items =
                    cells.chunked(7)
            ) { week ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(58.dp)
                            )
                        } else {
                            val dayTasks =
                                tasks.filter {
                                    TaskOccurrenceCalculator
                                        .occursOn(
                                            it,
                                            date
                                        )
                                }

                            Card(
                                onClick = {
                                    selectedDate =
                                        date
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                border =
                                    if (
                                        date ==
                                            selectedDate
                                    ) {
                                        androidx.compose.foundation
                                            .BorderStroke(
                                                2.dp,
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                            )
                                    } else {
                                        null
                                    }
                            ) {
                                Column(
                                    modifier =
                                        Modifier.padding(6.dp)
                                ) {
                                    Text(
                                        date.dayOfMonth
                                            .toString()
                                    )

                                    if (
                                        dayTasks.isNotEmpty()
                                    ) {
                                        Text(
                                            "• ${dayTasks.size}",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val today =
                            LocalDate.now()

                        month =
                            YearMonth.from(
                                today
                            )

                        selectedDate =
                            today
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("HOJE")
                }
            }

            item {
                Text(
                    selectedDate.format(
                        DateTimeFormatter
                            .ofPattern(
                                "dd/MM/yyyy"
                            )
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
            }

            if (selectedTasks.isEmpty()) {
                item {
                    Text(
                        "Nenhuma tarefa neste dia."
                    )
                }
            } else {
                val firstExpiredIndex =
                selectedTasks.indexOfFirst {
                    task ->

                    isTaskExpiredOnSelectedDate(
                        task,
                        selectedDate
                    )
                }

            itemsIndexed(
                items =
                    selectedTasks,
                key = {
                    _,
                    task ->

                    task.id
                }
            ) {
                index,
                task ->

                if (
                    index ==
                        firstExpiredIndex
                ) {
                    Text(
                        "expiradas",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        modifier =
                            Modifier.padding(
                                top = 4.dp,
                                bottom = 2.dp
                            )
                    )
                }

                TaskCard(
                    task = task,
                    onClick = {
                        onOpenTask(task.id)
                    }
                )
            }
            }

            status?.let { message ->
                item {
                    Text(message)
                }
            }

            item {
                AppVersionFooter()
            }
        }

        ReminderEntryLauncher(
            text = text,
            onTextChange = onTextChange,
            onContinue = onConfirmReminder,
            showPlusButton = true,
            autoOpen = autoOpenComposer,
            onAutoOpenConsumed =
                onAutoOpenConsumed,
            modifier =
                Modifier
                    .align(
                        androidx.compose.ui
                            .Alignment
                            .BottomEnd
                    )
                    .padding(20.dp),
            onOpenSavedTasks =
                onOpenSavedTasks,
            onOpenPermissions =
                onOpenPermissions,
            onOpenReadMe =
                onOpenReadMe,
            onTestAlert =
                onTestAlert,
            onExportErrors =
                onExportErrors
        )
    }
}



@Composable
private fun TaskCardDateTime(
    task: TaskRecord
) {
    val formattedDate =
        FriendlyDateFormatter
            .format(
                task.date
            )

    val parts =
        formattedDate
            .split("|")
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }

    val textPart =
        parts
            .firstOrNull()
            ?: formattedDate

    val remainingDate =
        parts
            .drop(
                1
            )
            .joinToString(
                separator = " | "
            )

    Text(
        "$textPart • ${task.timeLabel()}",
        style =
            MaterialTheme
                .typography
                .titleMedium
    )

    if (
        remainingDate.isNotBlank()
    ) {
        Text(
            remainingDate,
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskRecord,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {
            Text(
                task.title,
                style =
                    MaterialTheme.typography.titleMedium
            )

            TaskCardDateTime(
                task = task
            )

            Text(
                task.recurrenceLabel(),
                style =
                    MaterialTheme.typography.bodySmall
            )

            Text(
                "Notificação ${com.anotai.app.alarm
                    .ReminderEffectiveLeadSummary
                    .forTask(
                        task,
                        com.anotai.app.alarm
                            .EffectiveLeadKind
                            .NOTIFICATION
                    )} • alerta ${com.anotai.app.alarm
                    .ReminderEffectiveLeadSummary
                    .forTask(
                        task,
                        com.anotai.app.alarm
                            .EffectiveLeadKind
                            .ALERT
                    )}",
                style =
                    MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun TaskDetailScreen(
    task: TaskRecord,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Detalhes da tarefa",
                style =
                    MaterialTheme.typography.headlineSmall
            )
        }

        item {
            Info("Tarefa", task.title)
        }

        item {
            Info("Data", FriendlyDateFormatter.format(task.date))
        }

        item {
            Info("Horário", task.timeLabel())
        }

        item {
            Info(
                "Notificação",
                com.anotai.app.alarm
                    .ReminderEffectiveLeadSummary
                    .forTask(
                        task,
                        com.anotai.app.alarm
                            .EffectiveLeadKind
                            .NOTIFICATION
                    )
            )
        }

        item {
            Info(
                "Alerta forte",
                task.reminderLabel()
            )
        }

        item {
            Info(
                "Aviso antes do alerta",
                com.anotai.app.alarm
                    .ReminderEffectiveLeadSummary
                    .forTask(
                        task,
                        com.anotai.app.alarm
                            .EffectiveLeadKind
                            .PRE_ALERT
                    )
            )
        }

        item {
            Info(
                "Repetição",
                task.recurrenceLabel()
            )
        }

        if (task.untilDate != null) {
            item {
                Info(
                    "Até",
                    task.untilLabel()
                )
            }
        }

        item {
            Info(
                "Texto original",
                task.originalText
            )
        }

        item {
            Button(
                onClick = onEdit,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("EDITAR")
            }
        }

        item {
            OutlinedButton(
                onClick = onDelete,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("EXCLUIR")
            }
        }

        item {
            TextButton(
                onClick = onBack,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("VOLTAR")
            }
        }
    }
}

@Composable
private fun ConfirmationScreen(
    title: String,
    draft: TaskDraft,
    confirmText: String,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                title,
                style =
                    MaterialTheme.typography.headlineSmall
            )
        }

        if (draft.warnings.isNotEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(14.dp)
                    ) {
                        Text(
                            "Atenção",
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        draft.warnings.forEach {
                            Text("• $it")
                        }
                    }
                }
            }
        }

        item {
            Info("Tarefa", draft.title)
        }

        item {
            Info("Data", FriendlyDateFormatter.format(draft.date))
        }

        item {
            Info("Horário", draft.timeLabel())
        }

        item {
            Info(
                    "Notificação",
                    com.anotai.app.alarm
                        .ReminderEffectiveLeadSummary
                        .forDraft(
                            draft,
                            com.anotai.app.alarm
                                .EffectiveLeadKind
                                .NOTIFICATION
                        )
                )
        }

        item {
            Info(
                    "Alerta forte",
                    com.anotai.app.alarm
                        .ReminderEffectiveLeadSummary
                        .forDraft(
                            draft,
                            com.anotai.app.alarm
                                .EffectiveLeadKind
                                .ALERT
                        )
                )
        }

        item {
            Info(
                "Repetição",
                draft.recurrenceLabel()
            )
        }


        item {
            Info(
                "Aviso antes do alerta",
                com.anotai.app.alarm
                    .ReminderEffectiveLeadSummary
                    .forDraft(
                        draft,
                        com.anotai.app.alarm
                            .EffectiveLeadKind
                            .PRE_ALERT
                    )
            )
        }

        if (draft.untilDate != null) {
            item {
                Info(
                    "Até",
                    draft.untilLabel()
                )
            }
        }

        item {
            Info(
                "Texto original",
                draft.originalText
            )
        }

        item {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("EDITAR")
                }

                TextButton(
                    onClick = onCancel,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("CANCELAR")
                }
            }
        }

        item {
            Button(
                onClick = onConfirm,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(confirmText)
            }
        }
    }
}

@Composable
private fun DeleteConfirmationScreen(
    task: TaskRecord,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Excluir tarefa?",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Text(
            "A tarefa \"${task.title}\" será excluída e seus avisos serão cancelados."
        )

        Info(
            "Texto original",
            task.originalText
        )

        Button(
            onClick = onDelete,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "CONFIRMAR EXCLUSÃO"
            )
        }

        OutlinedButton(
            onClick = onCancel,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("CANCELAR")
        }
    }
}

@Composable
private fun Info(
    label: String,
    value: String
) {
    Column {
        Text(
            label,
            style =
                MaterialTheme.typography.labelLarge
        )

        Text(
            value,
            style =
                MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun EditScreen(
    title: String,
    draft: TaskDraft,
    parser: TaskInterpreter,
    onBack: () -> Unit,
    onReview: (TaskDraft) -> Unit
) {
    val dateFormat =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy"
        )

    val timeFormat =
        DateTimeFormatter.ofPattern(
            "HH:mm"
        )

    var taskTitle by remember(draft) {
        mutableStateOf(draft.title)
    }

    var originalText by remember(draft) {
        mutableStateOf(
            draft.originalText
        )
    }

    var date by remember(draft) {
        mutableStateOf(
            draft.date
                ?.format(dateFormat)
                .orEmpty()
        )
    }

    var time by remember(draft) {
        mutableStateOf(
            draft.time
                ?.format(timeFormat)
                .orEmpty()
        )
    }

            var recurrence by remember(draft) {
        mutableStateOf(
            draft.recurrenceLabel()
        )
    }

    var until by remember(draft) {
        mutableStateOf(
            draft.untilDate
                ?.format(dateFormat)
                .orEmpty()
        )
    }

    var validation by remember {
        mutableStateOf<String?>(null)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                title,
                style =
                    MaterialTheme.typography.headlineSmall
            )
        }

        item {
            OutlinedTextField(
                value = originalText,
                onValueChange = {
                    originalText = it
                },
                label = {
                    Text("Texto original")
                },
                supportingText = {
                    Text(
                        "Os comandos de notificação e alerta são lidos deste texto."
                    )
                },
                minLines = 2,
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                Modifier.height(8.dp)
            )

        OutlinedTextField(
                value = taskTitle,
                onValueChange = {
                    taskTitle = it
                },
                label = {
                    Text("Tarefa")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = date,
                onValueChange = {
                    date = it
                },
                label = {
                    Text(
                        "Data (dd/MM/aaaa)"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = time,
                onValueChange = {
                    time = it
                },
                label = {
                    Text(
                        "Horário (HH:mm)"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        item {
            Info(
                "Notificações reais",
                com.anotai.app.alarm
                    .ReminderEditPolicy
                    .notificationSummary(
                        originalText
                    )
            )
        }

        item {
            Info(
                "Alertas fortes reais",
                com.anotai.app.alarm
                    .ReminderEditPolicy
                    .alertSummary(
                        originalText
                    )
            )
        }

        item {
            OutlinedTextField(
                value = recurrence,
                onValueChange = {
                    recurrence = it
                },
                label = {
                    Text("Repetição")
                },
                supportingText = {
                    Text(
                        "Ex.: Não se repete, Todos os dias, Toda terça-feira, Todo dia 5"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = until,
                onValueChange = {
                    until = it
                },
                label = {
                    Text(
                        "Até (dd/MM/aaaa), opcional"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        validation?.let { message ->
            item {
                Text(
                    message,
                    color =
                        MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onBack,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "VOLTAR SEM ALTERAR"
                )
            }
        }

        item {
            Button(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = {
                    val parsedDate =
                        parseDate(date)

                    val parsedTime =
                        parseTime(time)

                    val parsedUntil =
                        if (until.isBlank()) {
                            null
                        } else {
                            parseDate(until)
                        }





                    when {
                        taskTitle.isBlank() ->
                            validation =
                                "Informe o nome da tarefa."
                        originalText.isBlank() ->
                            validation = "Informe o texto original da tarefa."

                        date.isNotBlank() &&
                            parsedDate == null ->
                            validation =
                                "Data inválida."

                        time.isNotBlank() &&
                            parsedTime == null ->
                            validation =
                                "Horário inválido."

                        until.isNotBlank() &&
                            parsedUntil == null ->
                            validation =
                                "Data final inválida."





                        else -> {
                            val (
                                type,
                                days,
                                monthDay
                            ) =
                                parser.parseManualRecurrence(
                                    recurrence,
                                    draft.weekdays,
                                    draft.monthDay
                                )

                            validation = null

                            onReview(
                                draft.copy(
                                    title =
                                        taskTitle.trim(),
                                    date =
                                        parsedDate,
                                    time =
                                        parsedTime,
                                    originalText =
                                        originalText.trim(),
                                    notificationMinutesBefore =
                                        com.anotai.app.alarm
                                            .ReminderEditPolicy
                                            .notificationPrimary(
                                                originalText
                                            ),
                                    reminderMinutesBefore =
                                        com.anotai.app.alarm
                                            .ReminderEditPolicy
                                            .alertPrimary(
                                                originalText
                                            ),
                                    recurrenceType =
                                        type,
                                    weekdays =
                                        days,
                                    monthDay =
                                        monthDay,
                                    untilDate =
                                        parsedUntil,
                                    warnings =
                                        emptyList()
                                )
                            )
                        }
                    }
                }
            ) {
                Text(
                    "REVISAR DADOS"
                )
            }
        }
    }
}

private fun TaskRecord.toDraft(): TaskDraft =
    TaskDraft(
        title = title,
        originalText = originalText,
        date = date,
        time = time,
        reminderMinutesBefore =
            reminderMinutesBefore,
        notificationMinutesBefore =
            notificationMinutesBefore,
        recurrenceType =
            recurrenceType,
        weekdays =
            weekdaysCsv
                .split(",")
                .mapNotNull {
                    it.toIntOrNull()
                }
                .mapNotNull {
                    runCatching {
                        DayOfWeek.of(it)
                    }.getOrNull()
                }
                .toSet(),
        monthDay = monthDay,
        untilDate = untilDate,
        warnings = emptyList()
    )

private fun TaskRecord.dateLabel(): String =
    date?.format(
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy"
        )
    ) ?: "Não informada"

private fun TaskRecord.timeLabel(): String =
    time?.format(
        DateTimeFormatter.ofPattern(
            "HH:mm"
        )
    ) ?: "Não informado"

private fun TaskRecord.untilLabel(): String =
    untilDate?.format(
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy"
        )
    ) ?: "Sem data final"

private fun isTaskExpiredOnSelectedDate(
    task: TaskRecord,
    selectedDate: LocalDate
): Boolean {
    val now =
        java.time.LocalDateTime.now()

    val today =
        now.toLocalDate()

    return when {
        selectedDate.isBefore(
            today
        ) ->
            true

        selectedDate.isAfter(
            today
        ) ->
            false

        task.time ==
            null ->
            false

        else ->
            task.time.isBefore(
                now.toLocalTime()
            )
    }
}

private fun storedCompactLeadSummary(
    leads: List<Int>
): String {
    if (
        leads.isEmpty()
    ) {
        return "Nenhum horário futuro"
    }

    return leads
        .distinct()
        .sortedDescending()
        .joinToString(
            separator = " "
        ) {
            minutes ->

            when {
                minutes == 0 ->
                    "0min"

                minutes % 60 == 0 ->
                    "${minutes / 60}h"

                else ->
                    "${minutes}min"
            }
        } +
        " antes"
}

private fun TaskRecord.notificationLabel(): String =
    minutesBeforeLabel(
        notificationMinutesBefore,
        time == null,
        "notificação"
    )

private fun TaskDraft.notificationLabel(): String =
    minutesBeforeLabel(
        notificationMinutesBefore,
        time == null,
        "notificação"
    )

private fun TaskRecord.reminderLabel(): String =
    minutesBeforeLabel(
        reminderMinutesBefore,
        time == null,
        "alerta"
    )

private fun minutesBeforeLabel(
    minutes: Int,
    missingTime: Boolean,
    kind: String
): String =
    when {
        minutes < 0 ->
            "Desativado"

        missingTime ->
            "Sem $kind (horário ausente)"

        minutes == 0 ->
            "No horário da tarefa"

        minutes % 60 == 0 -> {
            val hours =
                minutes / 60

            if (hours == 1) {
                "1 hora antes"
            } else {
                "$hours horas antes"
            }
        }

        else ->
            "$minutes minutos antes"
    }

private fun TaskRecord.recurrenceLabel(): String =
    recurrenceLabel(
        recurrenceType,
        weekdaysCsv
            .split(",")
            .mapNotNull {
                it.toIntOrNull()
            }
            .mapNotNull {
                runCatching {
                    DayOfWeek.of(it)
                }.getOrNull()
            }
            .toSet(),
        monthDay
    )

private fun recurrenceLabel(
    type: RecurrenceType,
    weekdays: Set<DayOfWeek>,
    monthDay: Int?
): String =
    when (type) {
        RecurrenceType.NONE ->
            "Não se repete"

        RecurrenceType.DAILY ->
            "Todos os dias"

        RecurrenceType.MONTHLY ->
            "Todo dia ${monthDay ?: "?"}"

        RecurrenceType.WEEKLY -> {
            val labels =
                weekdays
                    .sortedBy {
                        it.value
                    }
                    .map {
                        when (it) {
                            DayOfWeek.MONDAY ->
                                "segunda-feira"
                            DayOfWeek.TUESDAY ->
                                "terça-feira"
                            DayOfWeek.WEDNESDAY ->
                                "quarta-feira"
                            DayOfWeek.THURSDAY ->
                                "quinta-feira"
                            DayOfWeek.FRIDAY ->
                                "sexta-feira"
                            DayOfWeek.SATURDAY ->
                                "sábado"
                            DayOfWeek.SUNDAY ->
                                "domingo"
                        }
                    }

            if (labels.size == 1) {
                "Toda ${labels.first()}"
            } else {
                labels.joinToString(", ")
            }
        }
    }

private fun parseNoticeMinutes(
    value: String
): Int? {
    val normalized =
        value
            .trim()
            .lowercase()

    if (
        normalized in
        setOf(
            "desativado",
            "desativada",
            "sem",
            "nao",
            "não",
            "off"
        )
    ) {
        return -1
    }

    val number =
        normalized.toIntOrNull()
            ?: return null

    return if (number >= 0) {
        number
    } else {
        null
    }
}

private fun parseDate(
    value: String
): LocalDate? =
    if (value.isBlank()) {
        null
    } else {
        runCatching {
            LocalDate.parse(
                value.trim(),
                DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy"
                )
            )
        }.getOrNull()
    }

private fun parseTime(
    value: String
): LocalTime? =
    if (value.isBlank()) {
        null
    } else {
        runCatching {
            LocalTime.parse(
                value.trim(),
                DateTimeFormatter.ofPattern(
                    "HH:mm"
                )
            )
        }.getOrNull()
    }
