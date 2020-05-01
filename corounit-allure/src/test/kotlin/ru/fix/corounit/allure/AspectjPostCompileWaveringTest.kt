package ru.fix.corounit.allure

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.collections.shouldBeSingleton
import io.kotlintest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test


class MyStep {
    @Step
    suspend fun `my step method`() {
    }
}

class AspectjPostCompileWaveringTest {

    @Test
    fun test() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            println("start")
            val myStep = MyStep()
            myStep.`my step method`()
            println("stop")
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain("my step method")
        }
    }
}