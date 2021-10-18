package ru.fix.corounit.allure.example.concurrent

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
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }

@Feature("concurrency")
class ${testClassName(suite)} {
${
    (1..20).map { testNum(suite, it) }.map { testNum ->
        """
    @Test
    suspend fun `suspend test ${testNum}`() {
        "step $testNum"{
            delay(1_000)
        }
    }"""
    }.joinToString(separator = "\n")
    }
}""".trimIndent()
}

fun main() {
    for (suite in 1..10) {
        Files.write(Paths.get("" +
                "${testClassName(suite)}.kt"),
                template(suite).toByteArray())

        template(suite)
    }
}