package ru.fix.corounit.example

import ru.fix.corounit.engine.CorounitPlugin
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object CorounitConfig: CorounitPlugin {

    var beforeAllInvoked = AtomicBoolean(false)

    override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
        beforeAllInvoked.set(true)
        return super.beforeAllTestClasses(globalContext)
    }

    override fun <T: Any>createTestClassInstance(testClass: KClass<T>):T {
        val instance = testClass.createInstance()
        if(instance is UseDependencyInjectionByCorounitConfigTest) {
            UseDependencyInjectionByCorounitConfigTest::injectData.invoke(instance, "injected data")
        }
        return instance
    }
}