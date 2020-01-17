package ru.fix.corounit.allure

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class TestResultContext(
        val allureResult: io.qameta.allure.model.TestResult
) : AbstractCoroutineContextElement(Key) {
    companion object {
        val Key = object : CoroutineContext.Key<TestResultContext> {}
    }
}