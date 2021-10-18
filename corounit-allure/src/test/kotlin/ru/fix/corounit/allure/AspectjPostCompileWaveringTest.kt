package ru.fix.corounit.allure

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldBeSingleton
import io.kotlintest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class MyStep {
    suspend fun makeRequest(): String {
        return makeSubRequest()
    }

    suspend fun makeSubRequest(): String {
        delay(1)
        return "sub-request"
    }

    @Step
    suspend fun aspectedMethodWithReturnValue(number: Int): Boolean {
        makeRequest()
        return true
    }

    @Step
    suspend fun aspectedMethodWithoutReturnValue(number: Int) {
        makeRequest()
    }
}

@Step
class StepClassWithAnnotation{
    suspend fun stepMethodWithoutAnnotation(number: Int): Boolean {
        return true
    }
}

class StepClassWithoutAnnotation{
    suspend fun stepMethodWithoutAnnotation(number: Int): Boolean {
        return true
    }
}

@Disabled("""
    Kotlin 1.5 is not supported by aspectj.
    So compile time aspect injection via aspectj post compilation weaving
    methods marked with [ru.fix.corounit.allure.Step] annotation
    currently not supported
""")
class AspectjPostCompileWaveringTest {

    @Test
    fun `aspected method with return value`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = MyStep()
            myStep.aspectedMethodWithReturnValue(42)
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain(MyStep::aspectedMethodWithReturnValue.name)
            it.name.shouldContain("number")
            it.name.shouldContain("42")
        }
    }

    @Test
    fun `aspected method without return value`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = MyStep()
            myStep.aspectedMethodWithoutReturnValue(42)
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain(MyStep::aspectedMethodWithoutReturnValue.name)
            it.name.shouldContain("number")
            it.name.shouldContain("42")
        }
    }

    @Test
    fun `step class with annotation does not require annotation on each method`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = StepClassWithAnnotation()
            myStep.stepMethodWithoutAnnotation(42)
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain(StepClassWithAnnotation::stepMethodWithoutAnnotation.name)
        }
    }

    @Test
    fun `step class without annotations does nothing`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = StepClassWithoutAnnotation()
            myStep.stepMethodWithoutAnnotation(42)
        }
        allureStepContextElement.children.shouldBeEmpty()
    }
}