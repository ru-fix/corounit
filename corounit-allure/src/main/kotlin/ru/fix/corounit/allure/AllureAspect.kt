package ru.fix.corounit.allure

import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Morph
import net.bytebuddy.matcher.ElementMatchers
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

object AllureAspect {
    fun <T : Any> newAspectedInstanceViaSubtyping(clazz: KClass<T>, vararg args: Any?): T =
            newAspectedInstanceViaSubtyping(clazz.java, *args)

    fun <T : Any> newAspectedInstanceViaSubtyping(clazz: Class<T>, vararg args: Any?): T {
        val stepClass = newAspectedClassViaSubtyping(clazz)
        if (args.isEmpty()) {
            return stepClass.getConstructor().newInstance()
        }
        var ctors = stepClass.constructors.filter { it.parameterCount == args.size }
        if (ctors.isEmpty()) throw IllegalArgumentException("" +
                "Can not find constructor for class $clazz" +
                " with ${args.size} parameters")

        if (ctors.size == 1) {
            return ctors.single().newInstance(*args) as T
        }

        for (argIndex in 0 until args.size) {
            val arg = args[argIndex] ?: continue
            ctors = ctors.filter { it.parameterTypes[argIndex].isAssignableFrom(arg.javaClass) }
            if (ctors.isEmpty()) throw IllegalArgumentException("" +
                    "Can not find constructor with ${args.size} arguments" +
                    "matching parameters $args")
            if (ctors.size == 1) {
                return ctors.single().newInstance(*args) as T
            }
        }

        throw IllegalArgumentException("" +
                "Too many constructor candidates with ${args.size} arguments" +
                "matching parameters $args: $ctors")
    }


    fun <T : Any> newAspectedClassViaSubtyping(clazz: Class<T>): Class<out T> {
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
    }
}