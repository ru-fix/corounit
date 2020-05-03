package ru.fix.corounit.allure

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.collections.shouldBeSingleton
import io.kotlintest.matchers.string.shouldContain
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors


class MyStep {

    val pool = Executors.newFixedThreadPool(5).asCoroutineDispatcher()

    suspend fun makeRequest(){
        withContext(pool){
            Thread.sleep(100)
        }
    }

    @Step
    suspend fun `my step method`(number: Int) {
        makeRequest()
    }
}

class AspectjPostCompileWaveringTest {

    @Test
    fun test() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = MyStep()
            myStep.`my step method`(42)
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain("my step method")
            it.name.shouldContain("number")
            it.name.shouldContain("42")
        }
    }
}