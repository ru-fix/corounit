package ru.fix.corounit.allure

import kotlinx.coroutines.CoroutineScope

object Allure{
    suspend fun step(name:String, stepBody: suspend CoroutineScope.()->Unit) = ContextStepper.step(name, stepBody)
    suspend fun attachment(name: String, body: String) = ContextStepper.attachment(name, body)
}

suspend operator fun String.invoke(stepBody: suspend CoroutineScope.()->Unit) {
    Allure.step(this, stepBody)
}
