package com.anotai.app

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anotai.app.model.TaskDraft
import com.anotai.app.parser.TaskInterpreter
import com.anotai.app.typo.TypoSuggestion
import com.anotai.app.typo.TypoSuggestionEngine
import com.anotai.app.typo.rememberTypoSuggestions
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.width

data class TypoPhraseSuggestion(
    val originalWord: String,
    val replacementWord: String,
    val highlightStart: Int,
    val highlightLength: Int,
    val replacementPhrase: String,
    val sourceSuggestion: TypoSuggestion
)

object TypoPhraseSuggestionBuilder {

    private val timingSuffix =
        Regex(
            """^\s+(\d+\s*(?:h|hs|hora|horas|min|minuto|minutos)\s+antes)\b""",
            RegexOption.IGNORE_CASE
        )

    private val knownPrefixes =
        listOf(
            "depois de ",
            "dia depois de "
        )

    fun from(
        text: String,
        suggestion: TypoSuggestion
    ): TypoPhraseSuggestion {
        val wordStart =
            suggestion.start.coerceIn(
                0,
                text.length
            )

        val wordEnd =
            (
                suggestion.start +
                    suggestion.length
            ).coerceIn(
                wordStart,
                text.length
            )

        val before =
            text.substring(
                0,
                wordStart
            )

        val matchedPrefix =
            knownPrefixes.firstOrNull { prefix ->
                before
                    .lowercase()
                    .endsWith(
                        prefix.lowercase()
                    )
            }

        val prefixStart =
            if (matchedPrefix != null) {
                (
                    wordStart -
                        matchedPrefix.length
                ).coerceAtLeast(0)
            } else {
                wordStart
            }

        val remaining =
            text.substring(wordEnd)

        val suffix =
            timingSuffix
                .find(remaining)
                ?.value
                .orEmpty()

        val replacementPhrase =
            if (matchedPrefix != null) {
                matchedPrefix +
                    suggestion.replacement
            } else {
                suggestion.replacement +
                    suffix
            }

        val highlightEnd =
            if (matchedPrefix != null) {
                wordEnd
            } else {
                wordEnd +
                    suffix.length
            }

        return TypoPhraseSuggestion(
            originalWord =
                suggestion.original,
            replacementWord =
                suggestion.replacement,
            highlightStart =
                prefixStart,
            highlightLength =
                highlightEnd -
                    prefixStart,
            replacementPhrase =
                replacementPhrase,
            sourceSuggestion =
                suggestion
        )
    }

    fun replace(
        text: String,
        phrase: TypoPhraseSuggestion
    ): String {
        val start =
            phrase.highlightStart
                .coerceIn(
                    0,
                    text.length
                )

        val end =
            (
                phrase.highlightStart +
                    phrase.highlightLength
            ).coerceIn(
                start,
                text.length
            )

        val originalSegment =
            text.substring(
                start,
                end
            )

        val hasKnownPrefix =
            knownPrefixes.any { prefix ->
                originalSegment
                    .lowercase()
                    .startsWith(
                        prefix.lowercase()
                    )
            }

        if (hasKnownPrefix) {
            return buildString {
                append(
                    text.substring(
                        0,
                        start
                    )
                )

                append(
                    phrase.replacementPhrase
                )

                append(
                    text.substring(end)
                )
            }
        }

        return TypoSuggestionEngine
            .replace(
                text,
                phrase.sourceSuggestion
            )
    }
}

class TypoPhraseVisualTransformation(
    private val phrases:
        List<TypoPhraseSuggestion>
) : VisualTransformation {

    override fun filter(
        text: AnnotatedString
    ): TransformedText {
        val builder =
            AnnotatedString.Builder(text)

        phrases.forEach { phrase ->
            val start =
                phrase.highlightStart
                    .coerceIn(
                        0,
                        text.length
                    )

            val end =
                (
                    phrase.highlightStart +
                        phrase.highlightLength
                ).coerceIn(
                    start,
                    text.length
                )

            if (end > start) {
                builder.addStyle(
                    SpanStyle(
                        color =
                            Color.Red,
                        fontWeight =
                            FontWeight.Bold,
                        textDecoration =
                            TextDecoration.LineThrough
                    ),
                    start,
                    end
                )
            }
        }

        return TransformedText(
            builder.toAnnotatedString(),
            OffsetMapping.Identity
        )
    }
}

@Composable
fun ReminderEntryLauncher(
    text: String,
    onTextChange: (String) -> Unit,
    onContinue: () -> Unit,
    showPlusButton: Boolean = false,
    autoOpen: Boolean = false,
    onAutoOpenConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenSavedTasks: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenReadMe: () -> Unit = {},
    onTestAlert: () -> Unit = {},
    onExportErrors: () -> Unit = {}
) {
    var open by remember {
        mutableStateOf(false)
    }

    androidx.compose.runtime.LaunchedEffect(autoOpen) {
        if (autoOpen) {
            open = true
            onAutoOpenConsumed()
        }
    }

    if (showPlusButton) {
        androidx.compose.material3.FloatingActionButton(
            onClick = {
                open = true
            },
            modifier = modifier
        ) {
            Text(
                "+",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )
        }
    } else {
        Button(
            onClick = {
                open = true
            },
            modifier =
                modifier.fillMaxWidth()
        ) {
            Text("ADICIONAR LEMBRETE")
        }
    }

    if (!open) {
        return
    }

    Dialog(
        onDismissRequest = {
            open = false
        },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false
            )
    ) {
        AnotaiTheme {
            Surface(
                modifier =
                    Modifier.fillMaxSize()
            ) {
                ReminderComposerScreen(
                    text = text,
                    onTextChange = onTextChange,
                    onCancel = {
                        open = false
                    },
                    onContinue = {
                        open = false
                        onContinue()
                    },
                    onOpenSavedTasks = {
                        open = false
                        onOpenSavedTasks()
                    },
                    onOpenPermissions = {
                        open = false
                        onOpenPermissions()
                    },
                    onOpenReadMe = {
                        open = false
                        onOpenReadMe()
                    },
                    onTestAlert = {
                        open = false
                        onTestAlert()
                    },
                    onExportErrors = {
                        open = false
                        onExportErrors()
                    }
                )
            }
        }
    }
}



@OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class
)
@Composable
private fun ReminderComposerScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
    onOpenSavedTasks: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenReadMe: () -> Unit,
    onTestAlert: () -> Unit,
    onExportErrors: () -> Unit
) {
    BackHandler {
        onCancel()
    }

    val interpreter =
        remember {
            TaskInterpreter()
        }

    val draft =
        remember(text) {
            if (text.isBlank()) {
                null
            } else {
                interpreter.parse(
                    text,
                    LocalDateTime.now()
                )
            }
        }

    val focusRequester =
        remember {
            androidx.compose.ui.focus.FocusRequester()
        }

    val keyboardController =
        androidx.compose.ui.platform
            .LocalSoftwareKeyboardController
            .current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200L)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val rawSuggestions by
        rememberTypoSuggestions(text)

    val phraseSuggestions =
        remember(
            text,
            rawSuggestions
        ) {
            rawSuggestions.map {
                TypoPhraseSuggestionBuilder.from(
                    text,
                    it
                )
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                "Adicionar lembrete",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )

            ComposerOverflowMenu(
                onOpenSavedTasks = onOpenSavedTasks,
                onOpenPermissions = onOpenPermissions,
                onOpenReadMe = onOpenReadMe,
                onTestAlert = onTestAlert,
                onExportErrors = onExportErrors
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(
                        focusRequester
                    ),
            label = {
                Text(
                    "Escreva o lembrete para ser interpretado..."
                )
            },
            minLines = 3,
            visualTransformation =
                TypoPhraseVisualTransformation(
                    phraseSuggestions
                )
        )

        phraseSuggestions.forEach { phrase ->
            TextButton(
                onClick = {
                    onTextChange(
                        TypoPhraseSuggestionBuilder.replace(
                            text,
                            phrase
                        )
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "Você quis dizer ${phrase.replacementPhrase}?",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                )
            }
        }

        HorizontalDivider()

        if (draft != null) {
            LiveDraftSummary(draft)

            ReminderSchedulePreview(
                draft = draft
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text("VOLTAR")
            }

            Button(
                onClick = onContinue,
                enabled =
                    draft?.date != null &&
                        draft.time != null,
                modifier =
                    Modifier.weight(1f)
            ) {
                Text("CONFIRMAR")
            }
        }

        if (
            draft != null &&
            (
                draft.date == null ||
                draft.time == null
            )
        ) {
            Text(
                "Informe data e horário para confirmar.",
                style =
                    MaterialTheme
                        .typography
                        .labelMedium
            )
        }
    }
}


@Composable
private fun ComposerOverflowMenu(
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
private fun LiveDraftSummary(
    draft: TaskDraft
) {
    SummaryLine(
        "Título",
        draft.title
    )

    SummaryLine(
        "Data",
        liveFriendlyDateLabel(
            draft.date
        )
    )

    SummaryLine(
        "Horário",
        draft.timeLabel()
    )

    SummaryLine(
        "Notificação",
        effectiveLeadSummary(
            draft = draft,
            kind = LiveLeadKind.NOTIFICATION
        )
    )

    SummaryLine(
        "Alerta",
        effectiveLeadSummary(
            draft = draft,
            kind = LiveLeadKind.ALERT
        )
    )

    SummaryLine(
        "Repetição",
        draft.recurrenceLabel()
    )

    SummaryLine(
        "Até",
        draft.untilLabel()
    )

    if (draft.warnings.isNotEmpty()) {
        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            "Atenção",
            style =
                MaterialTheme
                    .typography
                    .titleSmall
        )

        draft.warnings.forEach {
            Text(
                "• $it",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}


private enum class LiveLeadKind {
    NOTIFICATION,
    ALERT
}

private fun effectiveLeadSummary(
    draft: TaskDraft,
    kind: LiveLeadKind
): String {
    val allLeads =
        when (kind) {
            LiveLeadKind.NOTIFICATION ->
                com.anotai.app.alarm.ReminderLeadPolicy
                    .notificationLeads(
                        draft.originalText
                    )

            LiveLeadKind.ALERT ->
                com.anotai.app.alarm.ReminderLeadPolicy
                    .alertLeads(
                        draft.originalText
                    )
        }

    val date =
        draft.date

    val time =
        draft.time

    val possible =
        if (
            date != null &&
            time != null
        ) {
            val now =
                java.time.LocalDateTime.now()

            val eventAt =
                date.atTime(
                    time
                )

            if (
                !eventAt.isAfter(
                    now
                )
            ) {
                emptyList()
            } else {
                allLeads.filter {
                    lead ->

                    eventAt
                        .minusMinutes(
                            lead.toLong()
                        )
                        .isAfter(
                            now
                        )
                }
            }
        } else {
            allLeads
        }

    return compactLeadSummary(
        possible
    )
}

private fun compactLeadSummary(
    leads: List<Int>
): String {
    if (
        leads.isEmpty()
    ) {
        return "Nenhum horário futuro"
    }

    val parts =
        leads
            .distinct()
            .sortedDescending()
            .map {
                minutes ->

                when {
                    minutes == 0 ->
                        "0min"

                    minutes % 60 == 0 ->
                        "${minutes / 60}h"

                    else ->
                        "${minutes}min"
                }
            }

    return parts.joinToString(
        separator = " "
    ) + " antes"
}


@Composable
private fun SummaryLine(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            androidx.compose.ui.Alignment.Top
    ) {
        Text(
            label,
            fontWeight =
                FontWeight.SemiBold,
            modifier =
                Modifier.width(
                    112.dp
                )
        )

        androidx.compose.foundation.layout.Spacer(
            modifier =
                Modifier.width(
                    16.dp
                )
        )

        Text(
            value,
            modifier =
                Modifier.weight(
                    1f
                )
        )
    }
}


private fun liveFriendlyDateLabel(
    date: LocalDate?
): String {
    if (date == null) {
        return "Não informada"
    }

    val today =
        LocalDate.now()

    val days =
        ChronoUnit.DAYS.between(
            today,
            date
        )

    val weekday =
        when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY ->
                "seg"

            java.time.DayOfWeek.TUESDAY ->
                "ter"

            java.time.DayOfWeek.WEDNESDAY ->
                "qua"

            java.time.DayOfWeek.THURSDAY ->
                "qui"

            java.time.DayOfWeek.FRIDAY ->
                "sex"

            java.time.DayOfWeek.SATURDAY ->
                "sáb"

            java.time.DayOfWeek.SUNDAY ->
                "dom"
        }

    val numeric =
        date.format(
            DateTimeFormatter.ofPattern(
                "dd.MM.yy"
            )
        )

    return when (days) {
        0L ->
            "Hoje | $weekday | $numeric"

        1L ->
            "Amanhã | $weekday | $numeric"

        2L ->
            "Depois de amanhã | $weekday | $numeric"

        in 3L..7L ->
            "Próx. $weekday | $numeric"

        else ->
            "$weekday | $numeric"
    }
}

private fun noticeLabel(
    minutes: Int
): String =
    when {
        minutes < 0 ->
            "Desativada"

        minutes == 0 ->
            "No horário"

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
