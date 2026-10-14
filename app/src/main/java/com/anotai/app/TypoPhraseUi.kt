package com.anotai.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.anotai.app.typo.TypoSuggestion
import com.anotai.app.typo.TypoSuggestionEngine

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
            suggestion.start.coerceIn(0, text.length)

        val wordEnd =
            (suggestion.start + suggestion.length)
                .coerceIn(wordStart, text.length)

        val before = text.substring(0, wordStart)

        val matchedPrefix =
            knownPrefixes.firstOrNull { prefix ->
                before.lowercase().endsWith(prefix.lowercase())
            }

        val prefixStart =
            if (matchedPrefix != null) {
                (wordStart - matchedPrefix.length).coerceAtLeast(0)
            } else {
                wordStart
            }

        val remaining = text.substring(wordEnd)
        val suffix = timingSuffix.find(remaining)?.value.orEmpty()

        val replacementPhrase =
            if (matchedPrefix != null) {
                matchedPrefix + suggestion.replacement
            } else {
                suggestion.replacement + suffix
            }

        val highlightEnd =
            if (matchedPrefix != null) wordEnd else wordEnd + suffix.length

        return TypoPhraseSuggestion(
            originalWord = suggestion.original,
            replacementWord = suggestion.replacement,
            highlightStart = prefixStart,
            highlightLength = highlightEnd - prefixStart,
            replacementPhrase = replacementPhrase,
            sourceSuggestion = suggestion
        )
    }

    fun replace(
        text: String,
        phrase: TypoPhraseSuggestion
    ): String {
        val start = phrase.highlightStart.coerceIn(0, text.length)
        val end =
            (phrase.highlightStart + phrase.highlightLength)
                .coerceIn(start, text.length)

        val originalSegment = text.substring(start, end)
        val hasKnownPrefix =
            knownPrefixes.any { prefix ->
                originalSegment.lowercase().startsWith(prefix.lowercase())
            }

        if (hasKnownPrefix) {
            return buildString {
                append(text.substring(0, start))
                append(phrase.replacementPhrase)
                append(text.substring(end))
            }
        }

        return TypoSuggestionEngine.replace(text, phrase.sourceSuggestion)
    }
}

class TypoPhraseVisualTransformation(
    private val phrases: List<TypoPhraseSuggestion>
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)

        phrases.forEach { phrase ->
            val start = phrase.highlightStart.coerceIn(0, text.length)
            val end =
                (phrase.highlightStart + phrase.highlightLength)
                    .coerceIn(start, text.length)

            if (end > start) {
                builder.addStyle(
                    SpanStyle(
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.LineThrough
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
