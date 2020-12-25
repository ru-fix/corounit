package ru.fix.corounit.engine

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

object CorounitConfig : CorounitPlugin, CorounitPluginsProvider {
    val beforeAllTestClassesInvocationCount = AtomicInteger()
    val skipMethodsInvocationCount = AtomicInteger()

    val skipTestMethodLog = ConcurrentLinkedDeque<TestMethodContextElement>()

    fun reset(){
        beforeAllTestClassesInvocationCount.set(0)
        skipMethodsInvocationCount.set(0)
    }

    override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
        beforeAllTestClassesInvocationCount.incrementAndGet()
        return super.beforeAllTestClasses(globalContext)
    }

    override suspend fun skipTestMethod(testMethodContext: CoroutineContext, reason: String) {
        skipMethodsInvocationCount.incrementAndGet()
        skipTestMethodLog.addLast(testMethodContext[TestMethodContextElement])
    }

    override fun plugins() = listOf(PluginForTestInvocation)
}