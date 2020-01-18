package ru.fix.corounit.example

import ru.fix.corounit.engine.CorounitPlugin
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object CorounitConfig: ru.fix.corounit.engine.CorounitConfig {

    override fun abilities() = setOf(CorounitPlugin.Ability.CREATE_INSTANCE)

    var beforeAllInvoked = AtomicBoolean(false)

    override suspend fun beforeAll(globalContext: CoroutineContext): CoroutineContext {
        beforeAllInvoked.set(true)
        return super.beforeAll(globalContext)
    }

    override fun <T: Any>createTestClassInstance(testClass: KClass<T>):T {
        val instance = testClass.createInstance()
        instance.javaClass.getMethod("injectData").invoke("injected data")
        return instance
    }
}