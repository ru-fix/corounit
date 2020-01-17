package ru.fix.corounit.engine

import kotlin.coroutines.CoroutineContext

interface CorounitPlugin {
    suspend fun beforeTestClass(testClassContext: CoroutineContext) = testClassContext
    suspend fun afterTestClass(testClassContext: CoroutineContext) {}

    suspend fun beforeTestMethod(testMethodContext: CoroutineContext) = testMethodContext
    suspend fun afterTestMethod(testMethodContext: CoroutineContext, thr: Throwable?){}

}