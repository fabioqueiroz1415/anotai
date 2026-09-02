package com.anotai.app

import android.os.Process
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * Filas de trabalho dedicadas para processamento de texto do Anotaí.
 *
 * A UI/teclado continuam na Main Thread. O parser e o corretor têm cada um
 * sua própria thread de baixa prioridade, evitando que o processamento da
 * mensagem entre na fila da interface.
 *
 * Estes executores duram durante todo o processo do app de propósito. Não
 * devem ser fechados por uma Activity, pois uma recriação da tela poderia
 * inutilizá-los para a próxima composição.
 */
object AnotaiDispatchers {

    private fun backgroundThreadFactory(
        threadName: String
    ): ThreadFactory =
        ThreadFactory { task ->
            Thread(
                {
                    Process.setThreadPriority(
                        Process.THREAD_PRIORITY_BACKGROUND
                    )
                    task.run()
                },
                threadName
            ).apply {
                isDaemon = true
            }
        }

    private val parserExecutor =
        Executors.newSingleThreadExecutor(
            backgroundThreadFactory("AnotaiParser")
        )

    private val typoExecutor =
        Executors.newSingleThreadExecutor(
            backgroundThreadFactory("AnotaiTypo")
        )

    val parser: CoroutineDispatcher =
        parserExecutor.asCoroutineDispatcher()

    val typo: CoroutineDispatcher =
        typoExecutor.asCoroutineDispatcher()
}
