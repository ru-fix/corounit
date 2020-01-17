package ru.fix.corounit.allure

import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Morph
import net.bytebuddy.matcher.ElementMatchers
import kotlin.reflect.KClass

object AllureAspect {
    fun <T: Any> newAspectedInstanceViaSubtyping(clazz: KClass<T>): T = newAspectedInstanceViaSubtyping(clazz.java)
    fun <T: Any> newAspectedInstanceViaSubtyping(clazz: Class<T>): T {
        return ByteBuddy()
                .subclass(clazz)
                .method(ElementMatchers.not(ElementMatchers.isDeclaredBy(java.lang.Object::class.java)))
                .intercept(MethodDelegation
                        .withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(MorphingInterceptedInvocation::class.java))
                        .to(AllureStepInterceptor::class.java)
                )

                .make()
                .load(clazz.classLoader)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
    }
}