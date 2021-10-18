package ru.fix.corounit.example.concurrent

import java.nio.file.Files
import java.nio.file.Paths

private fun testClassName(suite: Int) = "ConcurrentTest" + suite.toString().uppercase().padStart(2, '0')
private fun template(suite: Int): String {
    fun testNum(suite: Int, test: Int) = (20 * (suite-1) + test).toString().uppercase().padStart(4, '0')


    return """
package ru.fix.corounit.example.concurrent

import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger { }

class ${testClassName(suite)} {
${
    (1..20).map { testNum(suite, it) }.map { testNum ->
        """
    @Test
    suspend fun `suspend test ${testNum}`() {
        log.info { "start $testNum" }
        delay(1_000)
        log.info { "done $testNum" }
    }"""
    }.joinToString(separator = "\n")
    }
}""".trimIndent()
}

fun main() {
    for (suite in 1..50) {
        Files.write(Paths.get("" +
                "${testClassName(suite)}.kt"),
                template(suite).toByteArray())

        template(suite)
    }
}