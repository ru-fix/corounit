package ru.fix.corounit.allure

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.qameta.allure.AllureResultsWriter
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.model.TestResult
import io.qameta.allure.util.ResultsUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import ru.fix.corounit.engine.CorounitContext

class TestClass {

    @Epic("epic")
    @Feature("feature")
    @Story("story")
    @Package("my.package")
    suspend fun testMethod() {
        (2 * 2).shouldBe(4)
    }
}

class AllureAnnotationsTest {
    @Test
    fun `check annotations are working`() {
        val writer = mockk<AllureResultsWriter>()
        val slot = slot<TestResult>()
        every { writer.write(capture(slot)) } returns Unit

        val plugin = AllureCorounitPlugin(writer = writer)

        runBlocking(CorounitContext().apply {
            set(CorounitContext.TestClass, TestClass::class)
            set(CorounitContext.TestMethod, TestClass::testMethod)
        }) {
            val newContext = plugin.beforeTestMethod(coroutineContext)
            plugin.afterTestMethod(newContext, null)
        }

        mapOf(
                ResultsUtils.FEATURE_LABEL_NAME to "feature",
                ResultsUtils.EPIC_LABEL_NAME to "epic",
                ResultsUtils.STORY_LABEL_NAME to "story",
                ResultsUtils.PACKAGE_LABEL_NAME to "my.package"
        ).forEach { expected ->
            withClue("label $expected") {
                slot.captured.labels.any { it.name == expected.key && it.value == expected.value }.shouldBeTrue()
            }
        }
    }
}