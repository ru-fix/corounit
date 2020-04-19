package ru.fix.corounit.example

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger { }

class UsageTest {
    @Test
    suspend fun `suspend test`() {
        delay(1_000)
        log.info { "simple suspend test" }
    }

    @Test
    suspend fun `suspend test with launch`() = coroutineScope {
        launch {
            delay(1_000)
            println("suspend test with launch")
        }
    }

    @Test
    @Disabled("for test purpose")
    suspend fun `disabled test`() {
        delay(1000)
        log.info { "suspend disabled test" }
    }

    @Test
    @Tag("slow")
    suspend fun `exclude slow test by tag`(){
        log.info { "slow method with tag" }
    }

}