package com.anotai.app.typo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TypoSuggestionEngineTest {

    @Test
    fun aleryaSugereAlerta() {
        assertEquals(
            "alerta",
            TypoSuggestionEngine
                .localSuggestionFor(
                    "alerya"
                )
        )
    }

    @Test
    fun palavraCorretaNaoSugereElaMesma() {
        assertNull(
            TypoSuggestionEngine
                .localSuggestionFor(
                    "alerta"
                )
        )
    }

    @Test
    fun substituiSomentePalavraSelecionada() {
        val text =
            "sem alerya até dezembro"

        val suggestion =
            TypoSuggestion(
                original =
                    "alerya",
                replacement =
                    "alerta",
                start =
                    4,
                length =
                    6
            )

        assertEquals(
            "sem alerta até dezembro",
            TypoSuggestionEngine
                .replace(
                    text,
                    suggestion
                )
        )
    }
}
