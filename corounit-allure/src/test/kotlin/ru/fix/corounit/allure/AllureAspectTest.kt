package ru.fix.corounit.allure

import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

open class MyStepCalss{
    open suspend fun myStepMethod(myArg: String): String{
        return myArg
    }
}

class AllureAspectTest {
    @Test
    fun intercept(){
        val myStep = AllureAspect.newAspectedInstanceViaSubtyping(MyStepCalss::class)
        val step = AllureStep()
        runBlocking (step){
            myStep.myStepMethod("foo").shouldBe("foo")
        }
        step.children.single().let {
            it.step.name.shouldBe("myStepMethod (myArg: foo)")
        }
    }
}