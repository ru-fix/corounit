package ru.fix.corounit.allure

import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Morph
import net.bytebuddy.matcher.ElementMatchers
import kotlin.reflect.KClass

object AllureAspect {
    fun newAspectedInstanceViaSubtyping(clazz: KClass<*>): Any? = newAspectedInstanceViaSubtyping(clazz.java)
    fun newAspectedInstanceViaSubtyping(clazz: Class<*>): Any? {
        return ByteBuddy()
                .subclass(clazz)
                .method(ElementMatchers.not(ElementMatchers.isDeclaredBy(Any::class.java)))
                .intercept(MethodDelegation
                        .withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(MorphingInterceptedInvocation::class.java))
                        .to(AllureStepInterceptor::class.java))
                .make()
                .load(clazz.classLoader)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
    }
}