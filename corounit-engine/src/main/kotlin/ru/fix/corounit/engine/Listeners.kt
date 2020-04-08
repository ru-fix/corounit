package ru.fix.corounit.engine

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Listeners(val classes: Array<KClass<out CorounitListener>>)