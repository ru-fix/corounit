package ru.fix.corounit.example.concurrent

import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger { }

class ConcurrentTest01 {

    @Test
    suspend fun `suspend test 0001`() {
        log.info { "start 0001" }
        delay(1_000)
        log.info { "done 0001" }
    }

    @Test
    suspend fun `suspend test 0002`() {
        log.info { "start 0002" }
        delay(1_000)
        log.info { "done 0002" }
    }

    @Test
    suspend fun `suspend test 0003`() {
        log.info { "start 0003" }
        delay(1_000)
        log.info { "done 0003" }
    }

    @Test
    suspend fun `suspend test 0004`() {
        log.info { "start 0004" }
        delay(1_000)
        log.info { "done 0004" }
    }

    @Test
    suspend fun `suspend test 0005`() {
        log.info { "start 0005" }
        delay(1_000)
        log.info { "done 0005" }
    }

    @Test
    suspend fun `suspend test 0006`() {
        log.info { "start 0006" }
        delay(1_000)
        log.info { "done 0006" }
    }

    @Test
    suspend fun `suspend test 0007`() {
        log.info { "start 0007" }
        delay(1_000)
        log.info { "done 0007" }
    }

    @Test
    suspend fun `suspend test 0008`() {
        log.info { "start 0008" }
        delay(1_000)
        log.info { "done 0008" }
    }

    @Test
    suspend fun `suspend test 0009`() {
        log.info { "start 0009" }
        delay(1_000)
        log.info { "done 0009" }
    }

    @Test
    suspend fun `suspend test 0010`() {
        log.info { "start 0010" }
        delay(1_000)
        log.info { "done 0010" }
    }

    @Test
    suspend fun `suspend test 0011`() {
        log.info { "start 0011" }
        delay(1_000)
        log.info { "done 0011" }
    }

    @Test
    suspend fun `suspend test 0012`() {
        log.info { "start 0012" }
        delay(1_000)
        log.info { "done 0012" }
    }

    @Test
    suspend fun `suspend test 0013`() {
        log.info { "start 0013" }
        delay(1_000)
        log.info { "done 0013" }
    }

    @Test
    suspend fun `suspend test 0014`() {
        log.info { "start 0014" }
        delay(1_000)
        log.info { "done 0014" }
    }

    @Test
    suspend fun `suspend test 0015`() {
        log.info { "start 0015" }
        delay(1_000)
        log.info { "done 0015" }
    }

    @Test
    suspend fun `suspend test 0016`() {
        log.info { "start 0016" }
        delay(1_000)
        log.info { "done 0016" }
    }

    @Test
    suspend fun `suspend test 0017`() {
        log.info { "start 0017" }
        delay(1_000)
        log.info { "done 0017" }
    }

    @Test
    suspend fun `suspend test 0018`() {
        log.info { "start 0018" }
        delay(1_000)
        log.info { "done 0018" }
    }

    @Test
    suspend fun `suspend test 0019`() {
        log.info { "start 0019" }
        delay(1_000)
        log.info { "done 0019" }
    }

    @Test
    suspend fun `suspend test 0020`() {
        log.info { "start 0020" }
        delay(1_000)
        log.info { "done 0020" }
    }
}