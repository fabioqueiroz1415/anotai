package com.anotai.app.typo

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.text.Normalizer
import java.util.Locale

data class TypoSuggestion(
    val original: String,
    val replacement: String,
    val start: Int,
    val length: Int
)

object TypoSuggestionEngine {

    private val domainWords =
        setOf(
            "alerta",
            "alertar",
            "alertas",
            "avisar",
            "avise",
            "notificar",
            "notificação",
            "notificações",
            "hoje",
            "amanhã",
            "depois",
            "próximo",
            "próxima",
            "segunda",
            "terça",
            "quarta",
            "quinta",
            "sexta",
            "sábado",
            "domingo",
            "todos",
            "todas",
            "todo",
            "toda",
            "diariamente",
            "semanalmente",
            "mensalmente",
            "antes",
            "horas",
            "hora",
            "minutos",
            "minuto",
            "dezembro",
            "novembro",
            "outubro",
            "setembro",
            "agosto",
            "julho",
            "junho",
            "maio",
            "abril",
            "março",
            "fevereiro",
            "janeiro"
        )

    fun request(
        context: Context,
        text: String,
        precomputedLocal:
            List<TypoSuggestion>? = null,
        onResult:
            (List<TypoSuggestion>) -> Unit
    ): SpellCheckerSession? {
        val local =
            precomputedLocal
                ?: localSuggestions(text)

        val manager =
            context.getSystemService(
                TextServicesManager::class.java
            )

        if (manager == null) {
            onResult(local)
            return null
        }

        val listener =
            object :
                SpellCheckerSession
                    .SpellCheckerSessionListener {

                override fun onGetSuggestions(
                    results:
                        Array<out SuggestionsInfo>?
                ) = Unit

                override fun onGetSentenceSuggestions(
                    results:
                        Array<out SentenceSuggestionsInfo>?
                ) {
                    val system =
                        parseSystemSuggestions(
                            text,
                            results
                        )

                    val merged =
                        merge(
                            system,
                            local
                        )

                    Handler(
                        Looper.getMainLooper()
                    ).post {
                        onResult(merged)
                    }
                }
            }

        val session =
            try {
                manager.newSpellCheckerSession(
                    null,
                    Locale.forLanguageTag("pt-BR"),
                    listener,
                    true
                )
            } catch (_: Throwable) {
                null
            }

        if (session == null) {
            onResult(local)
            return null
        }

        return try {
            session.getSentenceSuggestions(
                arrayOf(
                    TextInfo(text)
                ),
                1
            )

            session
        } catch (_: Throwable) {
            session.close()
            onResult(local)
            null
        }
    }

    fun localSuggestionFor(
        word: String
    ): String? {
        val normalized =
            normalize(word)

        if (
            normalized.length < 4
        ) {
            return null
        }

        var best: String? =
            null

        var bestDistance =
            Int.MAX_VALUE

        for (candidate in domainWords) {
            val candidateNorm =
                normalize(candidate)

            val distance =
                levenshtein(
                    normalized,
                    candidateNorm
                )

            val maxAllowed =
                when {
                    normalized.length <= 5 ->
                        1

                    normalized.length <= 8 ->
                        2

                    else ->
                        3
                }

            if (
                distance <= maxAllowed &&
                distance < bestDistance
            ) {
                best =
                    candidate

                bestDistance =
                    distance
            }
        }

        if (
            best == null ||
            normalize(best) ==
            normalized
        ) {
            return null
        }

        return preserveCase(
            source = word,
            suggestion = best
        )
    }

    fun replace(
        text: String,
        suggestion: TypoSuggestion
    ): String {
        val start =
            suggestion.start
                .coerceIn(
                    0,
                    text.length
                )

        val end =
            (start + suggestion.length)
                .coerceIn(
                    start,
                    text.length
                )

        return buildString {
            append(
                text.substring(
                    0,
                    start
                )
            )

            append(
                suggestion.replacement
            )

            append(
                text.substring(end)
            )
        }
    }

    fun computeLocalSuggestions(
        text: String
    ): List<TypoSuggestion> =
        localSuggestions(text)

    private fun localSuggestions(
        text: String
    ): List<TypoSuggestion> {
        val wordRegex =
            Regex(
                """[\p{L}]{4,}"""
            )

        return wordRegex
            .findAll(text)
            .mapNotNull { match ->
                val replacement =
                    localSuggestionFor(
                        match.value
                    )
                        ?: return@mapNotNull null

                TypoSuggestion(
                    original =
                        match.value,
                    replacement =
                        replacement,
                    start =
                        match.range.first,
                    length =
                        match.value.length
                )
            }
            .distinctBy {
                it.start
            }
            .take(4)
            .toList()
    }

    private fun parseSystemSuggestions(
        text: String,
        results:
            Array<out SentenceSuggestionsInfo>?
    ): List<TypoSuggestion> {
        if (results.isNullOrEmpty()) {
            return emptyList()
        }

        val collected =
            mutableListOf<TypoSuggestion>()

        for (sentence in results) {
            for (
                index in
                0 until
                sentence.suggestionsCount
            ) {
                val info =
                    sentence
                        .getSuggestionsInfoAt(
                            index
                        )

                val attributes =
                    info.suggestionsAttributes

                val looksLikeTypo =
                    attributes and
                        SuggestionsInfo
                            .RESULT_ATTR_LOOKS_LIKE_TYPO !=
                        0

                val recommended =
                    attributes and
                        SuggestionsInfo
                            .RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS !=
                        0

                if (
                    !looksLikeTypo &&
                    !recommended
                ) {
                    continue
                }

                if (
                    info.suggestionsCount <= 0
                ) {
                    continue
                }

                val start =
                    sentence
                        .getOffsetAt(
                            index
                        )

                val length =
                    sentence
                        .getLengthAt(
                            index
                        )

                if (
                    start < 0 ||
                    length <= 0 ||
                    start + length >
                    text.length
                ) {
                    continue
                }

                val original =
                    text.substring(
                        start,
                        start + length
                    )

                val raw =
                    info.getSuggestionAt(0)

                if (
                    raw.isBlank() ||
                    normalize(raw) ==
                    normalize(original)
                ) {
                    continue
                }

                val distance =
                    levenshtein(
                        normalize(original),
                        normalize(raw)
                    )

                val allowed =
                    when {
                        original.length <= 5 ->
                            1

                        original.length <= 9 ->
                            2

                        else ->
                            3
                    }

                if (distance > allowed) {
                    continue
                }

                collected +=
                    TypoSuggestion(
                        original =
                            original,
                        replacement =
                            preserveCase(
                                original,
                                raw
                            ),
                        start =
                            start,
                        length =
                            length
                    )
            }
        }

        return collected
            .distinctBy {
                it.start
            }
            .take(4)
    }

    private fun merge(
        primary:
            List<TypoSuggestion>,
        fallback:
            List<TypoSuggestion>
    ): List<TypoSuggestion> =
        (primary + fallback)
            .sortedBy {
                it.start
            }
            .distinctBy {
                it.start
            }
            .take(4)

    private fun normalize(
        value: String
    ): String =
        Normalizer.normalize(
            value.lowercase(),
            Normalizer.Form.NFD
        )
            .replace(
                Regex(
                    "\\p{Mn}+"
                ),
                ""
            )

    private fun preserveCase(
        source: String,
        suggestion: String
    ): String {
        if (
            source.isNotEmpty() &&
            source.first().isUpperCase()
        ) {
            return suggestion.replaceFirstChar {
                it.titlecase()
            }
        }

        return suggestion
    }

    private fun levenshtein(
        first: String,
        second: String
    ): Int {
        if (first == second) {
            return 0
        }

        if (first.isEmpty()) {
            return second.length
        }

        if (second.isEmpty()) {
            return first.length
        }

        var previous =
            IntArray(
                second.length + 1
            ) {
                it
            }

        var current =
            IntArray(
                second.length + 1
            )

        for (
            i in
            1..first.length
        ) {
            current[0] =
                i

            for (
                j in
                1..second.length
            ) {
                val cost =
                    if (
                        first[i - 1] ==
                        second[j - 1]
                    ) {
                        0
                    } else {
                        1
                    }

                current[j] =
                    minOf(
                        current[j - 1] + 1,
                        previous[j] + 1,
                        previous[j - 1] +
                            cost
                    )
            }

            val temp =
                previous

            previous =
                current

            current =
                temp
        }

        return previous[
            second.length
        ]
    }
}
