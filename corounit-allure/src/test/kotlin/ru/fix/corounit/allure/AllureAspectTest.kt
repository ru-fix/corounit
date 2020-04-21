package ru.fix.corounit.allure

import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

open class StepClassWithNoArgs{
    open suspend fun myStepMethod(param: String): String{
        return param
    }
}

open class StepClassWithArgs(
        val arg1: String,
        val arg2: Int
){
    open suspend fun myStepMethod(param: String): String{
        return "$param-$arg1-$arg2"
    }
}

class AllureAspectTest {
    @Test
    fun `spep class with no args intercepted`(){
        val stepInstance = AllureAspect.newAspectedInstanceViaSubtyping(StepClassWithNoArgs::class)
        val allureStep = AllureStep()
        runBlocking (allureStep){
            stepInstance.myStepMethod("foo").shouldBe("foo")
        }
        allureStep.children.single().step.name.shouldBe("myStepMethod (param: foo)")
    }

    @Test
    fun `spep class with args intercepted`(){
        val stepInstance = AllureAspect.newAspectedInstanceViaSubtyping(StepClassWithArgs::class, "answer", 42)

        val allureStep = AllureStep()
        runBlocking (allureStep){
            stepInstance.myStepMethod("foo").shouldBe("foo-answer-42")
        }
        allureStep.children.single().step.name.shouldBe("myStepMethod (param: foo)")
    }
}