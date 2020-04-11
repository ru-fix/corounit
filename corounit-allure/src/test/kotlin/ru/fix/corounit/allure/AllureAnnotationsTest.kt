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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.fix.corounit.engine.CorounitPlugin
import ru.fix.corounit.engine.TestClassContextElement
import ru.fix.corounit.engine.TestMethodContextElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

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
    fun `annotations are working for success method invocation`() {

        val testResult = emulatePlugin(TestClass::class, TestClass::testMethod){plugin ->
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
                testResult.labels.any { it.name == expected.key && it.value == expected.value }.shouldBeTrue()
            }
        }

        testResult.name.shouldBe(TestClass::testMethod.name)
        testResult.description.shouldBe("description")
        testResult.status.shouldBe(Status.PASSED)
    }

    private fun emulatePlugin(testClass: KClass<*>,
                              testMethod: KFunction<*>,
                              pluginInvocation: suspend CoroutineScope.(plugin: CorounitPlugin)->Unit): TestResult{
        val writer = mockk<AllureResultsWriter>()
        val slot = slot<TestResult>()
        every { writer.write(capture(slot)) } returns Unit

        val plugin = AllureCorounitPlugin(writer = writer)

        runBlocking(TestClassContextElement(testClass) + TestMethodContextElement(testMethod)) {
            pluginInvocation(plugin)
        }
        return slot.captured
    }


    @Test
    fun `annotations are working for skipped method invocation`() {
        val testResult =emulatePlugin(TestClassWithDisabledMethod::class,
                TestClassWithDisabledMethod::skippedTestMethod){plugin ->
            plugin.skipTestMethod(coroutineContext, "reason")
        }

        mapOf(
                ResultsUtils.FEATURE_LABEL_NAME to "feature",
                ResultsUtils.EPIC_LABEL_NAME to "epic",
                ResultsUtils.STORY_LABEL_NAME to "story",
                ResultsUtils.PACKAGE_LABEL_NAME to "my.package"
        ).forEach { expected ->
            withClue("label $expected") {
                testResult.labels.any { it.name == expected.key && it.value == expected.value }.shouldBeTrue()
            }
        }

        testResult.name.shouldBe(TestClassWithDisabledMethod::skippedTestMethod.name)
        testResult.description.shouldBe("description")

        testResult.status.shouldBe(Status.SKIPPED)
        testResult.statusDetails.message.shouldBe("reason")
    }
}