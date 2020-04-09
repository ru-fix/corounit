package ru.fix.corounit.allure.example

import io.kotlintest.matchers.boolean.shouldBeTrue
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.AllureStep
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }


class AttachmentInEachMethodTest {

    suspend fun beforeEach() {
        AllureStep.attachment("attach", "content")
    }

    @Test
    suspend fun `first method`() {
        "my step" {
            true.shouldBeTrue()
        }
    }

    @Test
    suspend fun `second method`() {
        "my step" {
            true.shouldBeTrue()
        }
    }


}
