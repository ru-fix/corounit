package ru.fix.corounit.allure

import io.qameta.allure.model.StepResult
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class TestResultContext(
        val allureResult: io.qameta.allure.model.TestResult
) : AbstractCoroutineContextElement(Key) {
    companion object {
        val Key = object : CoroutineContext.Key<TestResultContext> {}
    }
}

class AllureContext(
        val step: StepResult
) : AbstractCoroutineContextElement(Key) {

    val children: ConcurrentLinkedDeque<AllureContext> = ConcurrentLinkedDeque()

    companion object {
        val Key = object : CoroutineContext.Key<AllureContext> {}
    }
}