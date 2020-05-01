package ru.fix.corounit.allure.example

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.collections.shouldBeSingleton
import io.kotlintest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.AllureStep
import ru.fix.corounit.allure.Step


class MyStep {
    @Step
    suspend fun `my step method`() {

    }
}

class AspectWaveringTest {

    @Test
    fun test() {
        val step = AllureStep()
        runBlocking(step) {
            println("start")
            val step = MyStep()
            step.`my step method`()
            println("stop")
        }
        step.children.shouldBeSingleton()
        step.children.single().step.asClue {
            it.name.shouldContain("my step method")
        }
    }
}