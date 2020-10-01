package ru.fix.corounit.allure.example

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.AllureStep

class MMMyStep(){



//    @Step
    suspend fun `my step`(){
        AllureStep.fromCurrentCoroutineContext().step("my step") {
            println("hello")
        }
    }

//    @Step
    suspend fun `my step empty`(){
            println("hello")
    }
 }

class CompilerAnnotationTest {
    @Test
    fun `do work`() = runBlocking{
        MMMyStep().`my step`()
    }
}