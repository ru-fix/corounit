package ru.fix.corounit.engine

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

interface CorounitPlugin {
    enum class Ability{
        CREATE_INSTANCE
    }
    fun abilities(): Set<Ability> = emptySet()

    suspend fun beforeAll(globalContext: CoroutineContext) = globalContext
    suspend fun afterAll(globalContext: CoroutineContext) {}

    suspend fun beforeTestClass(testClassContext: CoroutineContext) = testClassContext
    suspend fun afterTestClass(testClassContext: CoroutineContext) {}

    suspend fun beforeTestMethod(testMethodContext: CoroutineContext) = testMethodContext
    suspend fun afterTestMethod(testMethodContext: CoroutineContext, thr: Throwable?){}

    fun <T: Any> createTestClassInstance(testClass: KClass<T>):T = throw NotImplementedError()

}