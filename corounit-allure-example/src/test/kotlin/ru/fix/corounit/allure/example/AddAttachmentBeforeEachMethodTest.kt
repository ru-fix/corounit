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
        /**
         * Вот тут при получении по ключу сваливаемся,
         * ru.fix.corounit.allure.AllureStep$Companion.fromCurrentCoroutineContext(AllureStep.kt:40)
         * т.к. элемента по кючу еще нет в coroutineContext
         */
        AllureStep.attachment("attach", "content")
    }

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
