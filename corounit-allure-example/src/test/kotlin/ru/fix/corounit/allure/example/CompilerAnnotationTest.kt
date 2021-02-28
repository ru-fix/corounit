package ru.fix.corounit.allure.example

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

object CorounitInjector {
    @JvmStatic
    fun injectStep(stepName: String): AutoCloseable {
        return AutoCloseable { }
    }
}

class MMMyStep() {


    //    @Step
    suspend fun `my step`() {
        CorounitInjector.injectStep("my step name").use {
            println("hello step")
        }
    }

    //    @Step
    suspend fun `my step empty`() {
        println("hello empty")
    }
}

//TODO: https://bnorm.medium.com/writing-your-second-kotlin-compiler-plugin-part-1-project-setup-7b05c7d93f6c

class CompilerAnnotationTest {
    @Test
    fun `do work`() = runBlocking {
        MMMyStep().`my step`()
    }
}