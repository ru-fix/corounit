package ru.fix.corounit.engine

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class TestClassContextElement(
        val testClass: KClass<*>
) : AbstractCoroutineContextElement(TestClassContextElement) {

    companion object : CoroutineContext.Key<TestClassContextElement>
}

class TestMethodContextElement(
        val testMethod: KFunction<*>

) : AbstractCoroutineContextElement(TestMethodContextElement) {

    companion object : CoroutineContext.Key<TestMethodContextElement>
}