package com.anotai.app

import com.anotai.app.typo.TypoSuggestion
import org.junit.Assert.assertEquals
import org.junit.Test

class TypoPhraseExpressionTest {

    @Test
    fun depoisDeAmanhsSugereExpressaoInteira() {
        val text =
            "correr depois de amanhs 6h"

        val start =
            text.indexOf("amanhs")

        val phrase =
            TypoPhraseSuggestionBuilder
                .from(
                    text,
                    TypoSuggestion(
                        original =
                            "amanhs",
                        replacement =
                            "amanhã",
                        start =
                            start,
                        length =
                            "amanhs".length
                    )
                )

        assertEquals(
            "depois de amanhã",
            phrase.replacementPhrase
        )

        assertEquals(
            text.indexOf(
                "depois de amanhs"
            ),
            phrase.highlightStart
        )

        assertEquals(
            "depois de amanhs".length,
            phrase.highlightLength
        )
    }

    @Test
    fun substituiExpressaoInteira() {
        val text =
            "correr depois de amanhs 6h"

        val start =
            text.indexOf("amanhs")

        val phrase =
            TypoPhraseSuggestionBuilder
                .from(
                    text,
                    TypoSuggestion(
                        original =
                            "amanhs",
                        replacement =
                            "amanhã",
                        start =
                            start,
                        length =
                            "amanhs".length
                    )
                )

        assertEquals(
            "correr depois de amanhã 6h",
            TypoPhraseSuggestionBuilder
                .replace(
                    text,
                    phrase
                )
        )
    }
}
