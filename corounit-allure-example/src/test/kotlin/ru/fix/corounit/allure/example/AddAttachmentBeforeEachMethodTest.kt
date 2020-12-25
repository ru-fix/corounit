package ru.fix.corounit.allure.example

import io.kotlintest.matchers.boolean.shouldBeTrue
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.fix.corounit.allure.AllureStep
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddAttachmentBeforeEachMethodTest {

    suspend fun beforeAll() {
        AllureStep.attachment("attach", "beforeAllContent")
    }

    suspend fun afterAll() {
        AllureStep.attachment("attach", "afterAllContent")
    }

    suspend fun beforeEach() {
        AllureStep.attachment("attach", "beforeEachContent")
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
