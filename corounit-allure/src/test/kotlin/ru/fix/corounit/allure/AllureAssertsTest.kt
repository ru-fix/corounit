package ru.fix.corounit.allure

import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.qameta.allure.model.Status
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class AllureAssertsTest {

    @Test
    fun `boolean assert`() {
        val step = AllureStep()
        runBlocking(step) {

            "simple assert for boolean expression"(true)

            try {
                "failed assert for boolean"(false)
                fail("boolean assertion should fail on false argument")
            } catch (exc: AssertionError) {
            }
        }
        val children = step.children.toList()
        children.size.shouldBe(2)
        children[0].step.apply {
            name.shouldContain("simple assert for boolean")
            status.shouldBe(Status.PASSED)
        }
        children[1].step.apply {
            name.shouldContain("failed assert for boolean")
            status.shouldBe(Status.FAILED)
        }
    }
}
