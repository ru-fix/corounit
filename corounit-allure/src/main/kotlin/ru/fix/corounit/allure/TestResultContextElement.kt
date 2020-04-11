package ru.fix.corounit.allure

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class TestResultContextElement(
        val allureResult: io.qameta.allure.model.TestResult
) : AbstractCoroutineContextElement(TestResultContextElement) {
    companion object : CoroutineContext.Key<TestResultContextElement>
}