package ru.fix.corounit.allure

import kotlinx.coroutines.CoroutineScope


suspend operator fun String.invoke(stepBody: suspend CoroutineScope.()->Unit) {
    AllureStep.fromCurrentCoroutineContext().step(this, stepBody)
}
