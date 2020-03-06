package ru.fix.corounit.allure

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.qameta.allure.*
import io.qameta.allure.model.Status
import io.qameta.allure.model.TestResult
import io.qameta.allure.util.ResultsUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.fix.corounit.engine.CorounitContext

class TestClass {

    @Epic("epic")
    @Feature("feature")
    @Story("story")
    @Package("my.package")
    @Description("description")
    @Test
    suspend fun testMethod() {
        (2 * 2).shouldBe(4)
    }
}

class TestClassWithDisabledMethod {

    @Disabled("reason")
    @Epic("epic")
    @Feature("feature")
    @Story("story")
    @Package("my.package")
    @Description("description")
    @Test
    suspend fun skippedTestMethod() {
        (2 * 2).shouldBe(4)
    }

}

class AllureAnnotationsTest {
    @Test
    fun `check annotations are working for success method invocation`() {
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

        slot.captured.name.shouldBe(TestClass::testMethod.name)
        slot.captured.description.shouldBe("description")

        slot.captured.status.shouldBe(Status.PASSED)
    }

    @Test
    fun `check annotations are working for skipped method invocation`() {
        val writer = mockk<AllureResultsWriter>()
        val slot = slot<TestResult>()
        every { writer.write(capture(slot)) } returns Unit

        val plugin = AllureCorounitPlugin(writer = writer)

        runBlocking(CorounitContext().apply {
            set(CorounitContext.TestClass, TestClassWithDisabledMethod::class)
            set(CorounitContext.TestMethod, TestClassWithDisabledMethod::skippedTestMethod)
        }) {

            plugin.skipTestMethod(coroutineContext, "reason")
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

        slot.captured.name.shouldBe(TestClassWithDisabledMethod::skippedTestMethod.name)
        slot.captured.description.shouldBe("description")

        slot.captured.status.shouldBe(Status.SKIPPED)
        slot.captured.statusDetails.message.shouldBe("reason")
    }
}