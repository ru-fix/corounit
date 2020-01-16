package ru.fix.corounit.allure

import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Morph
import net.bytebuddy.matcher.ElementMatchers

object AllureAspect {
    fun newAspectedInstanceViaSubtyping(clazz: Class<*>): Any? {
        return ByteBuddy()
                .subclass(clazz)
                .method(ElementMatchers.not(ElementMatchers.isDeclaredBy(Any::class.java)))
                .intercept(MethodDelegation
                        .withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(MorphingInvocation::class.java))
                        .to(AllureStepInterceptor::class.java))
                .make()
                .load(clazz.classLoader)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
    }
}