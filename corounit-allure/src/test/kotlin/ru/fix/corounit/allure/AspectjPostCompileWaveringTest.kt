package ru.fix.corounit.allure

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldBeSingleton
import io.kotlintest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

    suspend fun stepWithDefaultValues(number: Int = 42, answer: Boolean = true): Boolean{
        return answer
    }

    suspend fun stepWithDefaultValuesAndNoReturn(number: Int = 42, answer: Boolean = true){
        delay(0)
    }
}

class StepClassWithoutAnnotation{
    suspend fun stepMethodWithoutAnnotation(number: Int): Boolean {
        return true
    }
}

@Step
class IntToJointPointCast{

    suspend fun stepWithDefaultValues(number: Int = 42, answer: Boolean = true): Boolean{
        return answer
    }
}

@Step
open class OpenClassWithAnnotation{

    suspend fun stepWithDefaultValues(number: Int = 42, answer: Boolean = true): Boolean{
        return answer
    }
}


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

    @Test
    fun `aspected method with default args and return value`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = StepClassWithAnnotation()
            myStep.stepWithDefaultValues()
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain(StepClassWithAnnotation::stepWithDefaultValues.name)
        }
    }


    @Test
    fun `aspected method with default args and without return value`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = StepClassWithAnnotation()
            myStep.stepWithDefaultValuesAndNoReturn()
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain(StepClassWithAnnotation::stepWithDefaultValues.name)
        }
    }

    @Test
    fun `aspected method with default args and without return value with partially provided args`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = StepClassWithAnnotation()
            myStep.stepWithDefaultValuesAndNoReturn(55)
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain(StepClassWithAnnotation::stepWithDefaultValues.name)
        }
    }

    @Test
    fun `int to jpinPoint cast`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = IntToJointPointCast()
            myStep.stepWithDefaultValues(55)
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain(StepClassWithAnnotation::stepWithDefaultValues.name)
        }
    }

    @Test
    fun `open class with annotation`() {
        val allureStepContextElement = AllureStep()
        runBlocking(allureStepContextElement) {
            val myStep = OpenClassWithAnnotation()
            myStep.stepWithDefaultValues(55)
        }
        allureStepContextElement.children.shouldBeSingleton()
        allureStepContextElement.children.single().step.asClue {
            it.name.shouldContain(StepClassWithAnnotation::stepWithDefaultValues.name)
        }
    }
}