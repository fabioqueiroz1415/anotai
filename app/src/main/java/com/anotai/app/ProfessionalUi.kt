package com.anotai.app

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.anotai.app.alarm.AlarmScheduler
import com.anotai.app.alarm.EffectiveLeadKind
import com.anotai.app.alarm.ReminderEditPolicy
import com.anotai.app.alarm.ReminderEffectiveLeadSummary
import com.anotai.app.alarm.TestAlarmScheduler
import com.anotai.app.calendar.TaskOccurrenceCalculator
import com.anotai.app.data.TaskRecord
import com.anotai.app.data.TaskRepository
import com.anotai.app.logging.ErrorLogManager
import com.anotai.app.model.RecurrenceType
import com.anotai.app.model.TaskDraft
import com.anotai.app.parser.TaskInterpreter
import com.anotai.app.permission.PermissionCoordinator
import com.anotai.app.typo.rememberTypoSuggestions
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PtBr = Locale("pt", "BR")

private enum class AppDestination {
    CALENDAR,
    SAVED,
    SETTINGS,
    DETAIL
}

private enum class VisualTaskState {
    UPCOMING,
    EXPIRED,
    COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalAnotaiApp(
    repository: TaskRepository,
    scheduler: AlarmScheduler,
    parser: TaskInterpreter,
    errorLog: ErrorLogManager,
    themeSettings: ThemeSettings,
    onThemeSettingsChange: (ThemeSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var destination by rememberSaveable { mutableStateOf(AppDestination.CALENDAR) }
    var tasks by remember { mutableStateOf<List<TaskRecord>>(emptyList()) }
    var selectedTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    var composerOpen by rememberSaveable { mutableStateOf(false) }
    var editTask by remember { mutableStateOf<TaskRecord?>(null) }
    var deleteConfirmation by remember { mutableStateOf<TaskRecord?>(null) }
    var busy by remember { mutableStateOf(false) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    suspend fun refreshNow() {
        tasks = withContext(Dispatchers.IO) { repository.all() }
    }

    fun refresh() {
        scope.launch { refreshNow() }
    }

    fun openTask(task: TaskRecord) {
        selectedTaskId = task.id
        destination = AppDestination.DETAIL
    }

    fun saveNewReminder(text: String) {
        if (text.isBlank() || busy) return
        busy = true
        scope.launch {
            try {
                val parsed = withContext(AnotaiDispatchers.parser) {
                    parser.parse(text)
                }

                if (parsed.date == null || parsed.time == null) {
                    snackbarHostState.showSnackbar(
                        "Informe data e horário para salvar a tarefa."
                    )
                    return@launch
                }

                val updated = withContext(Dispatchers.IO) {
                    errorLog.recordInterpretationProblem(parsed)
                    val id = repository.save(parsed)
                    scheduler.scheduleNext(id)
                    repository.all()
                }
                tasks = updated
                if (input == text) input = ""
                snackbarHostState.showSnackbar("Tarefa salva")
            } catch (t: Throwable) {
                withContext(Dispatchers.IO) { errorLog.recordException(text, t) }
                snackbarHostState.showSnackbar(
                    "Não foi possível salvar. O erro foi registrado."
                )
            } finally {
                busy = false
            }
        }
    }

    fun setCompleted(task: TaskRecord, completed: Boolean, allowUndo: Boolean = true) {
        if (busy) return
        scope.launch {
            val changed = withContext(Dispatchers.IO) {
                val ok = repository.setCompleted(task.id, completed)
                if (ok) {
                    if (completed) scheduler.cancel(task.id) else scheduler.scheduleNext(task.id)
                }
                ok
            }
            if (!changed) {
                snackbarHostState.showSnackbar("Não foi possível atualizar a tarefa.")
                return@launch
            }
            refreshNow()
            val message = if (completed) "Tarefa concluída" else "Tarefa reaberta"
            if (!allowUndo) {
                snackbarHostState.showSnackbar(message)
                return@launch
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                withContext(Dispatchers.IO) {
                    repository.setCompleted(task.id, !completed)
                    if (!completed) scheduler.cancel(task.id) else scheduler.scheduleNext(task.id)
                }
                refreshNow()
            }
        }
    }

    fun deleteTask(task: TaskRecord, allowUndo: Boolean = true) {
        if (busy) return
        scope.launch {
            val deleted = withContext(Dispatchers.IO) {
                scheduler.cancel(task.id)
                repository.delete(task.id)
            }
            if (!deleted) {
                snackbarHostState.showSnackbar("Não foi possível excluir a tarefa.")
                return@launch
            }

            if (selectedTaskId == task.id) {
                selectedTaskId = null
                destination = AppDestination.CALENDAR
            }
            refreshNow()

            if (!allowUndo) {
                snackbarHostState.showSnackbar("Tarefa excluída")
                return@launch
            }

            val result = snackbarHostState.showSnackbar(
                message = "Tarefa excluída",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                withContext(Dispatchers.IO) {
                    repository.restore(task)
                    if (!task.completed) scheduler.scheduleNext(task.id)
                }
                refreshNow()
            }
        }
    }

    fun updateTask(task: TaskRecord, draft: TaskDraft) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val updated = withContext(Dispatchers.IO) {
                    val ok = repository.update(task.id, draft)
                    if (ok && !task.completed) scheduler.replace(task.id)
                    ok to repository.all()
                }
                tasks = updated.second
                editTask = null
                snackbarHostState.showSnackbar(
                    if (updated.first) "Tarefa atualizada" else "Não foi possível atualizar a tarefa."
                )
            } catch (t: Throwable) {
                withContext(Dispatchers.IO) { errorLog.recordException(draft.originalText, t) }
                snackbarHostState.showSnackbar(
                    "Não foi possível atualizar. O erro foi registrado."
                )
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(repository) { refreshNow() }

    BackHandler(enabled = destination != AppDestination.CALENDAR) {
        destination = AppDestination.CALENDAR
        selectedTaskId = null
    }

    val selectedTask = selectedTaskId?.let { id -> tasks.firstOrNull { it.id == id } }

    Scaffold(
        topBar = {
            AppTopBar(
                destination = destination,
                selectedDate = selectedDate,
                onBack = {
                    destination = AppDestination.CALENDAR
                    selectedTaskId = null
                },
                onOpenSaved = {
                    refresh()
                    destination = AppDestination.SAVED
                },
                onOpenSettings = { destination = AppDestination.SETTINGS },
                onOpenHelp = {
                    context.startActivity(Intent(context, ReadMeActivity::class.java))
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = destination == AppDestination.CALENDAR,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(120))
            ) {
                ExtendedFloatingActionButton(
                    onClick = { composerOpen = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nova tarefa") },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (destination) {
                AppDestination.CALENDAR -> ProfessionalCalendarScreen(
                    tasks = tasks,
                    month = month,
                    selectedDate = selectedDate,
                    onMonthChange = { newMonth ->
                        month = newMonth
                        val maxDay = newMonth.lengthOfMonth()
                        selectedDate = newMonth.atDay(selectedDate.dayOfMonth.coerceAtMost(maxDay))
                    },
                    onSelectedDateChange = { selectedDate = it },
                    onOpenTask = ::openTask,
                    onToggleCompleted = { task -> setCompleted(task, !task.completed) },
                    onDelete = { task -> deleteTask(task) }
                )

                AppDestination.SAVED -> SavedTasksScreenProfessional(
                    tasks = tasks,
                    onOpenTask = ::openTask,
                    onToggleCompleted = { task -> setCompleted(task, !task.completed) },
                    onDelete = { task -> deleteTask(task) }
                )

                AppDestination.SETTINGS -> SettingsScreenProfessional(
                    settings = themeSettings,
                    onSettingsChange = onThemeSettingsChange,
                    onOpenPermissions = {
                        context.startActivity(Intent(context, PermissionsActivity::class.java))
                    },
                    onTestAlert = {
                        TestAlarmScheduler.scheduleInTenSeconds(context)
                        scope.launch { snackbarHostState.showSnackbar("Alerta de teste programado para 10 segundos") }
                    },
                    onExportErrors = { errorLog.share() },
                    onOpenHelp = {
                        context.startActivity(Intent(context, ReadMeActivity::class.java))
                    }
                )

                AppDestination.DETAIL -> {
                    if (selectedTask == null) {
                        EmptyDetail(onBack = { destination = AppDestination.CALENDAR })
                    } else {
                        TaskDetailScreenProfessional(
                            task = selectedTask,
                            onEdit = { editTask = selectedTask },
                            onToggleCompleted = {
                                setCompleted(selectedTask, !selectedTask.completed, allowUndo = false)
                            },
                            onDelete = { deleteConfirmation = selectedTask }
                        )
                    }
                }
            }
        }
    }

    if (composerOpen) {
        ReminderComposerSheetProfessional(
            text = input,
            onTextChange = { input = it },
            parser = parser,
            busy = busy,
            onDismiss = { composerOpen = false },
            onSave = { text ->
                composerOpen = false
                saveNewReminder(text)
            }
        )
    }

    editTask?.let { task ->
        EditTaskSheetProfessional(
            task = task,
            parser = parser,
            busy = busy,
            onDismiss = { editTask = null },
            onSave = { draft -> updateTask(task, draft) }
        )
    }

    deleteConfirmation?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteConfirmation = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Excluir tarefa?") },
            text = { Text("“${task.title}” será removida e os avisos programados serão cancelados.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmation = null
                        deleteTask(task, allowUndo = true)
                    }
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmation = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    destination: AppDestination,
    selectedDate: LocalDate,
    onBack: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val title = when (destination) {
        AppDestination.CALENDAR -> "Anotaí"
        AppDestination.SAVED -> "Tarefas"
        AppDestination.SETTINGS -> "Configurações"
        AppDestination.DETAIL -> "Detalhes"
    }

    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (destination == AppDestination.CALENDAR) {
                    Text(
                        friendlyFullDate(selectedDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (destination != AppDestination.CALENDAR) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tarefas salvas") },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpenSaved()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Configurações") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpenSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ajuda e sobre") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpenHelp()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ProfessionalCalendarScreen(
    tasks: List<TaskRecord>,
    month: YearMonth,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onSelectedDateChange: (LocalDate) -> Unit,
    onOpenTask: (TaskRecord) -> Unit,
    onToggleCompleted: (TaskRecord) -> Unit,
    onDelete: (TaskRecord) -> Unit
) {
    var monthPickerOpen by remember { mutableStateOf(false) }
    val currentMonth = YearMonth.now()
    val today = LocalDate.now()

    val tasksByDate by produceState<Map<LocalDate, List<TaskRecord>>>(
        initialValue = emptyMap(),
        tasks,
        month
    ) {
        value = withContext(Dispatchers.Default) {
            (1..month.lengthOfMonth())
                .map { month.atDay(it) }
                .associateWith { date ->
                    tasks.filter { TaskOccurrenceCalculator.occursOn(it, date) }
                }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 108.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MonthHeader(
                month = month,
                showToday = month != currentMonth || selectedDate != today,
                onOpenPicker = { monthPickerOpen = true },
                onPrevious = { onMonthChange(month.minusMonths(1)) },
                onNext = { onMonthChange(month.plusMonths(1)) },
                onToday = {
                    onMonthChange(currentMonth)
                    onSelectedDateChange(today)
                }
            )
        }

        item {
            WeekdayHeader()
        }

        item {
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    if (targetState.isAfter(initialState)) {
                        (slideInHorizontally(tween(220)) { it } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally(tween(220)) { -it } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(220)) { it } + fadeOut(tween(180)))
                    }
                },
                label = "month_transition"
            ) { animatedMonth ->
                CalendarGrid(
                    month = animatedMonth,
                    selectedDate = selectedDate,
                    tasksByDate = tasksByDate,
                    onSelectDate = onSelectedDateChange,
                    onSwipePrevious = { onMonthChange(month.minusMonths(1)) },
                    onSwipeNext = { onMonthChange(month.plusMonths(1)) }
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                friendlyFullDate(selectedDate),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            AnimatedContent(
                targetState = selectedDate,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "day_tasks"
            ) { animatedDate ->
                TaskDaySection(
                    date = animatedDate,
                    tasks = tasksByDate[animatedDate].orEmpty(),
                    onOpenTask = onOpenTask,
                    onToggleCompleted = onToggleCompleted,
                    onDelete = onDelete
                )
            }
        }

        item { AppVersionFooter() }
    }

    if (monthPickerOpen) {
        MonthYearPickerDialog(
            initialMonth = month,
            onDismiss = { monthPickerOpen = false },
            onSelect = { picked ->
                monthPickerOpen = false
                onMonthChange(picked)
                onSelectedDateChange(picked.atDay(1))
            }
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    showToday: Boolean,
    onOpenPicker: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
        }
        TextButton(
            onClick = onOpenPicker,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                monthTitle(month),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        AnimatedVisibility(visible = showToday) {
            AssistChip(onClick = onToday, label = { Text("Hoje") })
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskRecord>>,
    onSelectDate: (LocalDate) -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit
) {
    var totalDrag by remember(month) { mutableStateOf(0f) }
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    val firstDay = month.atDay(1)
    val firstColumn = firstDay.dayOfWeek.value - 1
    val cells = remember(month) {
        buildList<LocalDate?> {
            repeat(firstColumn) { add(null) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
            while (size % 7 != 0) add(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(month) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        totalDrag += amount
                    },
                    onDragEnd = {
                        when {
                            totalDrag > threshold -> onSwipePrevious()
                            totalDrag < -threshold -> onSwipeNext()
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        DayCell(
                            date = date,
                            selected = date == selectedDate,
                            today = date == LocalDate.now(),
                            tasks = tasksByDate[date].orEmpty(),
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    tasks: List<TaskRecord>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(160),
        label = "selected_day"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(160),
        label = "selected_day_text"
    )

    Column(
        modifier = modifier.aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(selectedColor)
                .then(
                    if (today && !selected) {
                        Modifier.border(
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            CircleShape
                        )
                    } else Modifier
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = textColor,
                fontWeight = if (today || selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        Row(
            modifier = Modifier.height(8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tasks.take(3).forEach { task ->
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.completed) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun MonthYearPickerDialog(
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit
) {
    var year by remember { mutableStateOf(initialMonth.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar mês") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Ano anterior")
                    }
                    Text(year.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { year++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Próximo ano")
                    }
                }
                (1..12).chunked(3).forEach { rowMonths ->
                    Row(Modifier.fillMaxWidth()) {
                        rowMonths.forEach { monthNumber ->
                            val value = YearMonth.of(year, monthNumber)
                            FilterChip(
                                selected = value == initialMonth,
                                onClick = { onSelect(value) },
                                label = {
                                    Text(
                                        value.month.getDisplayName(TextStyle.SHORT, PtBr)
                                            .replaceFirstChar { it.titlecase(PtBr) }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun TaskDaySection(
    date: LocalDate,
    tasks: List<TaskRecord>,
    onOpenTask: (TaskRecord) -> Unit,
    onToggleCompleted: (TaskRecord) -> Unit,
    onDelete: (TaskRecord) -> Unit
) {
    val sorted = remember(tasks, date) {
        tasks.sortedBy { it.time ?: LocalTime.MAX }
    }
    val upcoming = sorted.filter { taskVisualState(it, date) == VisualTaskState.UPCOMING }
    val completed = sorted.filter { taskVisualState(it, date) == VisualTaskState.COMPLETED }
    val expired = sorted.filter { taskVisualState(it, date) == VisualTaskState.EXPIRED }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (sorted.isEmpty()) {
            EmptyDayCard(date)
        } else {
            upcoming.forEach { task ->
                SwipeableTaskCard(
                    task = task,
                    state = VisualTaskState.UPCOMING,
                    onOpen = { onOpenTask(task) },
                    onToggleCompleted = { onToggleCompleted(task) },
                    onDelete = { onDelete(task) }
                )
            }

            if (completed.isNotEmpty()) {
                SectionDivider("Concluídas")
                completed.forEach { task ->
                    SwipeableTaskCard(
                        task = task,
                        state = VisualTaskState.COMPLETED,
                        onOpen = { onOpenTask(task) },
                        onToggleCompleted = { onToggleCompleted(task) },
                        onDelete = { onDelete(task) }
                    )
                }
            }

            if (expired.isNotEmpty()) {
                SectionDivider("Tarefas anteriores")
                expired.forEach { task ->
                    SwipeableTaskCard(
                        task = task,
                        state = VisualTaskState.EXPIRED,
                        onOpen = { onOpenTask(task) },
                        onToggleCompleted = { onToggleCompleted(task) },
                        onDelete = { onDelete(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDayCard(date: LocalDate) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (date == LocalDate.now()) "Nada planejado para hoje" else "Nenhuma tarefa neste dia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Toque em “Nova tarefa” e escreva algo como “Dentista amanhã às 14h”.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun SwipeableTaskCard(
    task: TaskRecord,
    state: VisualTaskState,
    onOpen: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offset = remember(task.id, task.completed) { Animatable(0f) }
    val density = LocalDensity.current
    val maxOffset = with(density) { 132.dp.toPx() }
    val threshold = with(density) { 88.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (offset.value < 0f) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            )
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (task.completed) "Reabrir" else "Concluir")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Excluir")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }

        TaskCardProfessional(
            task = task,
            state = state,
            onClick = onOpen,
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(task.id, task.completed) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(-maxOffset, maxOffset))
                            }
                        },
                        onDragEnd = {
                            when {
                                offset.value >= threshold -> onToggleCompleted()
                                offset.value <= -threshold -> onDelete()
                            }
                            scope.launch { offset.animateTo(0f, tween(160)) }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f, tween(160)) }
                        }
                    )
                }
        )
    }
}

@Composable
private fun TaskCardProfessional(
    task: TaskRecord,
    state: VisualTaskState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = when (state) {
        VisualTaskState.UPCOMING -> 1f
        VisualTaskState.EXPIRED -> 0.62f
        VisualTaskState.COMPLETED -> 0.68f
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                taskTimeLabel(task),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.recurrenceType != RecurrenceType.NONE) {
                        CompactMeta(Icons.Default.Repeat, taskRecurrenceLabel(task))
                    }
                    CompactMeta(
                        Icons.Default.Notifications,
                        ReminderEffectiveLeadSummary.forTask(task, EffectiveLeadKind.NOTIFICATION)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, label) = when (state) {
                        VisualTaskState.UPCOMING -> Icons.Default.Schedule to "Próxima"
                        VisualTaskState.EXPIRED -> Icons.Default.ErrorOutline to "Passou do horário"
                        VisualTaskState.COMPLETED -> Icons.Default.Check to "Concluída"
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun CompactMeta(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun SavedTasksScreenProfessional(
    tasks: List<TaskRecord>,
    onOpenTask: (TaskRecord) -> Unit,
    onToggleCompleted: (TaskRecord) -> Unit,
    onDelete: (TaskRecord) -> Unit
) {
    val upcoming = tasks.filter { savedTaskState(it) == VisualTaskState.UPCOMING }
    val completed = tasks.filter { savedTaskState(it) == VisualTaskState.COMPLETED }
    val expired = tasks.filter { savedTaskState(it) == VisualTaskState.EXPIRED }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (tasks.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Seu calendário está livre", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "As tarefas que você adicionar aparecerão aqui.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            if (upcoming.isNotEmpty()) {
                item { Text("Próximas", style = MaterialTheme.typography.titleMedium) }
                items(upcoming, key = { "saved-up-${it.id}" }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        state = VisualTaskState.UPCOMING,
                        onOpen = { onOpenTask(task) },
                        onToggleCompleted = { onToggleCompleted(task) },
                        onDelete = { onDelete(task) }
                    )
                }
            }
            if (completed.isNotEmpty()) {
                item { SectionDivider("Concluídas") }
                items(completed, key = { "saved-done-${it.id}" }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        state = VisualTaskState.COMPLETED,
                        onOpen = { onOpenTask(task) },
                        onToggleCompleted = { onToggleCompleted(task) },
                        onDelete = { onDelete(task) }
                    )
                }
            }
            if (expired.isNotEmpty()) {
                item { SectionDivider("Tarefas anteriores") }
                items(expired, key = { "saved-old-${it.id}" }) { task ->
                    SwipeableTaskCard(
                        task = task,
                        state = VisualTaskState.EXPIRED,
                        onOpen = { onOpenTask(task) },
                        onToggleCompleted = { onToggleCompleted(task) },
                        onDelete = { onDelete(task) }
                    )
                }
            }
        }
        item { AppVersionFooter() }
    }
}

@Composable
private fun TaskDetailScreenProfessional(
    task: TaskRecord,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val state = savedTaskState(task)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                task.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null
            )
        }
        item {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        when (state) {
                            VisualTaskState.UPCOMING -> "Próxima"
                            VisualTaskState.EXPIRED -> "Passou do horário"
                            VisualTaskState.COMPLETED -> "Concluída"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        when (state) {
                            VisualTaskState.UPCOMING -> Icons.Default.Schedule
                            VisualTaskState.EXPIRED -> Icons.Default.ErrorOutline
                            VisualTaskState.COMPLETED -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        item { DetailLine(Icons.Default.CalendarMonth, "Data", task.date?.let(::friendlyFullDate) ?: "Não informada") }
        item { DetailLine(Icons.Default.Schedule, "Horário", taskTimeLabel(task)) }
        item { DetailLine(Icons.Default.Notifications, "Notificação", ReminderEffectiveLeadSummary.forTask(task, EffectiveLeadKind.NOTIFICATION)) }
        item { DetailLine(Icons.Default.Notifications, "Alerta forte", ReminderEffectiveLeadSummary.forTask(task, EffectiveLeadKind.ALERT)) }
        item { DetailLine(Icons.Default.Repeat, "Repetição", taskRecurrenceLabel(task)) }
        task.untilDate?.let { until ->
            item { DetailLine(Icons.Default.CalendarMonth, "Até", friendlyFullDate(until)) }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Texto original", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(task.originalText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            Button(onClick = onToggleCompleted, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    if (task.completed) Icons.Default.Refresh else Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (task.completed) "Reabrir tarefa" else "Marcar como concluída")
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Editar")
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Excluir") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp).size(20.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun EmptyDetail(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Esta tarefa não está mais disponível.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Voltar ao calendário") }
    }
}

@Composable
private fun SettingsScreenProfessional(
    settings: ThemeSettings,
    onSettingsChange: (ThemeSettings) -> Unit,
    onOpenPermissions: () -> Unit,
    onTestAlert: () -> Unit,
    onExportErrors: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    var diagnosticRefresh by remember { mutableStateOf(0) }
    val diagnostic = remember(diagnosticRefresh) {
        PermissionCoordinator.statusLines(context)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsSectionTitle(Icons.Default.Palette, "Aparência")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Tema", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> "Sistema"
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Escuro"
                            }
                            FilterChip(
                                selected = settings.mode == mode,
                                onClick = { onSettingsChange(settings.copy(mode = mode)) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Cores do sistema", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Usa Material You no Android 12 ou superior.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = { onSettingsChange(settings.copy(dynamicColor = it)) },
                            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        )
                    }
                }
            }
        }

        item { SettingsSectionTitle(Icons.Default.Tune, "Confiabilidade e diagnóstico") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Diagnóstico", style = MaterialTheme.typography.titleSmall)
                    diagnostic.forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { diagnosticRefresh++ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Atualizar")
                        }
                        Button(
                            onClick = onOpenPermissions,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Revisar permissões")
                        }
                    }
                }
            }
        }
        item {
            SettingsActionCard(
                icon = Icons.Default.Notifications,
                title = "Testar alerta",
                description = "Programa um alerta de teste para daqui a 10 segundos.",
                button = "Executar teste",
                onClick = onTestAlert
            )
        }
        item {
            SettingsActionCard(
                icon = Icons.Default.ErrorOutline,
                title = "Exportar diagnóstico",
                description = "Compartilha o arquivo de erros para análise técnica.",
                button = "Exportar erros",
                onClick = onExportErrors
            )
        }
        item {
            SettingsActionCard(
                icon = Icons.Default.Info,
                title = "Ajuda e sobre o Anotaí",
                description = "Veja exemplos de uso, comportamento dos alertas e informações do app.",
                button = "Abrir ajuda",
                onClick = onOpenHelp
            )
        }
        item { AppVersionFooter() }
    }
}

@Composable
private fun SettingsSectionTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    button: String,
    onClick: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(button) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderComposerSheetProfessional(
    text: String,
    onTextChange: (String) -> Unit,
    parser: TaskInterpreter,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var draft by remember { mutableStateOf<TaskDraft?>(null) }
    var parsedText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val rawSuggestions by rememberTypoSuggestions(text)
    val phraseSuggestions = remember(text, rawSuggestions) {
        rawSuggestions.map { TypoPhraseSuggestionBuilder.from(text, it) }
    }

    LaunchedEffect(text) {
        if (text.isBlank()) {
            draft = null
            parsedText = ""
        } else {
            delay(250L)
            val snapshot = text
            val parsed = withContext(AnotaiDispatchers.parser) {
                parser.parse(snapshot, LocalDateTime.now())
            }
            draft = parsed
            parsedText = snapshot
        }
    }

    LaunchedEffect(Unit) {
        delay(180L)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val currentDraft = draft.takeIf { parsedText == text }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Nova tarefa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            item {
                Text(
                    "Escreva naturalmente. Ex.: “Buscar filha amanhã às 8h e alertar 1 hora antes”.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text("O que você precisa lembrar?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    visualTransformation = TypoPhraseVisualTransformation(phraseSuggestions)
                )
            }
            phraseSuggestions.forEach { phrase ->
                item(key = "typo-${phrase.highlightStart}-${phrase.replacementPhrase}") {
                    TextButton(
                        onClick = {
                            onTextChange(TypoPhraseSuggestionBuilder.replace(text, phrase))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Você quis dizer “${phrase.replacementPhrase}”?")
                    }
                }
            }
            currentDraft?.let { parsed ->
                item {
                    DraftPreviewCard(parsed)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { onSave(text) },
                        enabled = !busy && currentDraft?.date != null && currentDraft.time != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (busy) "Salvando…" else "Salvar tarefa")
                    }
                }
            }
            if (currentDraft != null && (currentDraft.date == null || currentDraft.time == null)) {
                item {
                    Text(
                        "Informe data e horário para confirmar.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftPreviewCard(draft: TaskDraft) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Entendi assim", style = MaterialTheme.typography.labelLarge)
            Text(draft.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            DraftPreviewLine(
                Icons.Default.CalendarMonth,
                "${draft.date?.let(::friendlyFullDate) ?: "Data não identificada"} · ${draft.time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "horário não identificado"}"
            )
            DraftPreviewLine(
                Icons.Default.Notifications,
                "Notificação ${leadLabel(draft.notificationMinutesBefore)} · alerta ${leadLabel(draft.reminderMinutesBefore)}"
            )
            if (draft.recurrenceType != RecurrenceType.NONE) {
                DraftPreviewLine(Icons.Default.Repeat, draft.recurrenceLabel())
            }
            draft.warnings.forEach { warning ->
                Text(
                    "• $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DraftPreviewLine(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskSheetProfessional(
    task: TaskRecord,
    parser: TaskInterpreter,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit
) {
    val initial = remember(task.id) { task.toDraftProfessional() }
    var title by rememberSaveable(task.id) { mutableStateOf(initial.title) }
    var originalText by rememberSaveable(task.id) { mutableStateOf(initial.originalText) }
    var date by rememberSaveable(task.id) { mutableStateOf(initial.date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).orEmpty()) }
    var time by rememberSaveable(task.id) { mutableStateOf(initial.time?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()) }
    var recurrence by rememberSaveable(task.id) { mutableStateOf(initial.recurrenceLabel()) }
    var until by rememberSaveable(task.id) { mutableStateOf(initial.untilDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).orEmpty()) }
    var validation by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Editar tarefa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tarefa") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = originalText,
                    onValueChange = { originalText = it },
                    label = { Text("Texto original") },
                    supportingText = { Text("Notificações e alertas são recalculados a partir deste texto.") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Data") },
                        placeholder = { Text("dd/MM/aaaa") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Horário") },
                        placeholder = { Text("HH:mm") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = recurrence,
                    onValueChange = { recurrence = it },
                    label = { Text("Repetição") },
                    supportingText = { Text("Ex.: Não se repete, Todos os dias, Toda terça-feira, Todo dia 5") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = until,
                    onValueChange = { until = it },
                    label = { Text("Até (opcional)") },
                    placeholder = { Text("dd/MM/aaaa") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Avisos calculados", style = MaterialTheme.typography.labelLarge)
                        Text("Notificação: ${ReminderEditPolicy.notificationSummary(originalText)}")
                        Text("Alerta forte: ${ReminderEditPolicy.alertSummary(originalText)}")
                    }
                }
            }
            validation?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val parsedDate = parseDateProfessional(date)
                            val parsedTime = parseTimeProfessional(time)
                            val parsedUntil = if (until.isBlank()) null else parseDateProfessional(until)
                            when {
                                title.isBlank() -> validation = "Informe o nome da tarefa."
                                originalText.isBlank() -> validation = "Informe o texto original da tarefa."
                                parsedDate == null -> validation = "Data inválida."
                                parsedTime == null -> validation = "Horário inválido."
                                until.isNotBlank() && parsedUntil == null -> validation = "Data final inválida."
                                else -> {
                                    val recurrenceParsed = parser.parseManualRecurrence(
                                        recurrence,
                                        initial.weekdays,
                                        initial.monthDay
                                    )
                                    validation = null
                                    onSave(
                                        initial.copy(
                                            title = title.trim(),
                                            originalText = originalText.trim(),
                                            date = parsedDate,
                                            time = parsedTime,
                                            notificationMinutesBefore = ReminderEditPolicy.notificationPrimary(originalText),
                                            reminderMinutesBefore = ReminderEditPolicy.alertPrimary(originalText),
                                            recurrenceType = recurrenceParsed.first,
                                            weekdays = recurrenceParsed.second,
                                            monthDay = recurrenceParsed.third,
                                            untilDate = parsedUntil,
                                            warnings = emptyList()
                                        )
                                    )
                                }
                            }
                        }
                    ) { Text("Salvar alterações") }
                }
            }
        }
    }
}

private fun taskVisualState(task: TaskRecord, occurrenceDate: LocalDate): VisualTaskState {
    if (task.completed) return VisualTaskState.COMPLETED
    val now = LocalDateTime.now()
    val occurrenceTime = task.time ?: return if (occurrenceDate.isBefore(now.toLocalDate())) VisualTaskState.EXPIRED else VisualTaskState.UPCOMING
    return if (occurrenceDate.atTime(occurrenceTime).isBefore(now)) VisualTaskState.EXPIRED else VisualTaskState.UPCOMING
}

private fun savedTaskState(task: TaskRecord): VisualTaskState {
    if (task.completed) return VisualTaskState.COMPLETED
    val today = LocalDate.now()
    if (task.untilDate != null && task.untilDate.isBefore(today)) return VisualTaskState.EXPIRED
    if (task.recurrenceType != RecurrenceType.NONE) return VisualTaskState.UPCOMING
    val date = task.date ?: return VisualTaskState.UPCOMING
    val time = task.time
    val now = LocalDateTime.now()
    return if (time != null) {
        if (date.atTime(time).isBefore(now)) VisualTaskState.EXPIRED else VisualTaskState.UPCOMING
    } else {
        if (date.isBefore(today)) VisualTaskState.EXPIRED else VisualTaskState.UPCOMING
    }
}

private fun taskTimeLabel(task: TaskRecord): String =
    task.time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"

private fun monthTitle(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.FULL, PtBr)
        .replaceFirstChar { it.titlecase(PtBr) } + " de ${month.year}"

private fun friendlyFullDate(date: LocalDate): String {
    val today = LocalDate.now()
    val month = date.month.getDisplayName(TextStyle.FULL, PtBr)
    return when (date) {
        today -> "Hoje, ${date.dayOfMonth} de $month"
        today.plusDays(1) -> "Amanhã, ${date.dayOfMonth} de $month"
        else -> {
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, PtBr)
                .replaceFirstChar { it.titlecase(PtBr) }
            "$weekday, ${date.dayOfMonth} de $month"
        }
    }
}

private fun leadLabel(minutes: Int): String = when {
    minutes < 0 -> "desativada"
    minutes == 0 -> "no horário"
    minutes % 60 == 0 -> {
        val hours = minutes / 60
        if (hours == 1) "1h antes" else "${hours}h antes"
    }
    else -> "${minutes}min antes"
}

private fun taskRecurrenceLabel(task: TaskRecord): String = when (task.recurrenceType) {
    RecurrenceType.NONE -> "Não se repete"
    RecurrenceType.DAILY -> "Todos os dias"
    RecurrenceType.MONTHLY -> "Todo dia ${task.monthDay ?: "?"}"
    RecurrenceType.WEEKLY -> {
        val labels = task.weekdaysCsv.split(",")
            .mapNotNull { it.toIntOrNull() }
            .mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }
            .sortedBy { it.value }
            .map { it.getDisplayName(TextStyle.FULL, PtBr) }
        if (labels.size == 1) "Toda ${labels.first()}" else labels.joinToString(", ")
    }
}

private fun TaskRecord.toDraftProfessional(): TaskDraft =
    TaskDraft(
        title = title,
        originalText = originalText,
        date = date,
        time = time,
        reminderMinutesBefore = reminderMinutesBefore,
        notificationMinutesBefore = notificationMinutesBefore,
        recurrenceType = recurrenceType,
        weekdays = weekdaysCsv.split(",")
            .mapNotNull { it.toIntOrNull() }
            .mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }
            .toSet(),
        monthDay = monthDay,
        untilDate = untilDate,
        warnings = emptyList()
    )

private fun parseDateProfessional(value: String): LocalDate? =
    runCatching {
        LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrNull()

private fun parseTimeProfessional(value: String): LocalTime? =
    runCatching {
        LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern("H:mm"))
    }.recoverCatching {
        LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrNull()
