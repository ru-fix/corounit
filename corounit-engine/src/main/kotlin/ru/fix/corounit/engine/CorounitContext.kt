package ru.fix.corounit.engine

import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class CorounitContext(
        private val parent: CorounitContext? = null

) : AbstractCoroutineContextElement(CoroutineContextKey) {

    companion object {
        private val CoroutineContextKey = object : CoroutineContext.Key<CorounitContext> {}

        fun fromContext(coroutineContext: CoroutineContext): CorounitContext = coroutineContext[CoroutineContextKey]!!

        val TestMethod = object : Key<KFunction<*>> {}
        val TestClass = object : Key<KClass<*>> {}
    }

    interface Key<T>

    private val data = ConcurrentHashMap<Key<*>, Any?>()

    operator fun <T> get(key: Key<T>): T? {
        var context = this
        while (true) {
            val result = context.data[key]
            val parent = context.parent

            if (result != null || parent == null) {
                return result as T?
            } else {
                context = parent
            }
        }
    }

    operator fun <T> set(key: Key<T>, value: T) {
        data[key] = value
    }

    var testMethod: KFunction<*>
        get() = this[TestMethod]!!
        set(value) {
            this[TestMethod] = value
        }

    var testClass: KClass<*>
        get() = this[TestClass]!!
        set(value) {
            this[TestClass] = value
        }
}