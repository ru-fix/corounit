package ru.fix.corounit.allure.example

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger { }

class UsageTest {
    @Test
    suspend fun `suspend test`() {
        delay(1_000)
        log.info { "simple suspend test" }
    }

    @Feature("feature 1")
    @Epic("epic 1")
    @Story("story 1")
    @Test
    suspend fun `suspend test with launch`() = coroutineScope {
        launch {
            delay(1_000)
            println("suspend test with launch")
        }
    }

    @Test
    @Disabled("for test purpose")
    suspend fun `disabled`() {
        delay(1000)
        log.info { "suspend disabled test" }
    }

}