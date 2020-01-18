package ru.fix.corounit.engine

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

interface CorounitPlugin {
    suspend fun beforeAll(globalContext: CoroutineContext) = globalContext
    suspend fun afterAll(globalContext: CoroutineContext) {}

    suspend fun beforeTestClass(testClassContext: CoroutineContext) = testClassContext
    suspend fun afterTestClass(testClassContext: CoroutineContext) {}

    suspend fun beforeTestMethod(testMethodContext: CoroutineContext) = testMethodContext
    suspend fun afterTestMethod(testMethodContext: CoroutineContext, thr: Throwable?){}

    fun <T: Any> createTestClassInstance(testClass: KClass<T>):T = throw NotImplementedError()

}