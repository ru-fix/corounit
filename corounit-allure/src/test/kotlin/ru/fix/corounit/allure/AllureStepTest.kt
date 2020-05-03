package ru.fix.corounit.allure

import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.qameta.allure.model.Status
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class AllureStepTest {
    @Test
    fun `failed assert`() {

        val step = AllureStep()
        var thrSlot: AssertionError? = null

        try {
            runBlocking(step) {
                "block with failed assert"{
                    (2 * 2).shouldBe(5)
                }
            }
        }catch (err: AssertionError){
            thrSlot = err
        }

        thrSlot.shouldNotBeNull()

        step.children.let {
            it.size.shouldBe(1)
            it.single().let { it ->
                it.step.name.shouldBe("block with failed assert")
                it.step.status.shouldBe(Status.FAILED)
            }
        }
    }

    @Test
    fun `child step context inherent all parent coroutine context elements`(){
        val parentStep = AllureStep()
        val parentContext = CoroutineName("the-name") + parentStep
        val childContext = parentStep.startChildStepWithCoroutineContext("child", parentContext)
        val nameInChildContext = childContext[CoroutineName]
        nameInChildContext.shouldNotBeNull()
        nameInChildContext.name.shouldBe("the-name")
    }
}