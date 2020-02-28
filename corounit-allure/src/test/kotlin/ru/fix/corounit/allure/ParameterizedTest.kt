package ru.fix.corounit.allure

import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.qameta.allure.model.Status
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ParameterizedTest {

    @Test
    fun `parameterized launches added as child steps to report`() {
        val step = AllureStep()
        var slot: AssertionError? = null
        try {
            runBlocking(step) {
                parameterized(
                        row(1, "one"),
                        row(2, null),
                        row(3, "oops")) { number, _ ->

                    number.shouldNotBe(3)
                }
            }
        } catch (exc: AssertionError) {
            slot = exc
        }

        slot.shouldNotBeNull()

        val children = step.children.toList()
        children.size.shouldBe(3)
        children[0].step.apply {
            name.shouldContain("parameterized(1, one)")
            status.shouldBe(Status.PASSED)
        }
        children[1].step.apply {
            name.shouldContain("parameterized(2, null)")
            status.shouldBe(Status.PASSED)
        }
        children[2].step.apply {
            name.shouldContain("parameterized(3, oops)")
            status.shouldBe(Status.FAILED)
        }
    }
}
