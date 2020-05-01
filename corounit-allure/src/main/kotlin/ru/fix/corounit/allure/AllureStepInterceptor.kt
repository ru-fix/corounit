package ru.fix.corounit.allure

import net.bytebuddy.implementation.bind.annotation.*
import java.lang.reflect.Method
import java.util.concurrent.Callable


class AllureStepInterceptor {
    companion object {
        @JvmStatic
        @BindingPriority(BindingPriority.DEFAULT * 100)
        @RuntimeType
        fun interceptStep(
                @Morph interceptedInvocation: MorphingInterceptedInvocation,
                @Origin method: Method,
                @AllArguments args: Array<Any?>): Any? {

            val wrapper = StepMethodWrapper(method, args)

            return wrapper.wrappedInvoke{ newArgs ->
                interceptedInvocation.invoke(newArgs)
            }
        }

        @JvmStatic
        @RuntimeType
        fun interceptNonStepToSuper(@SuperCall superCall: Callable<Any?>): Any? {
            return superCall.call()
        }

        @JvmStatic
        @RuntimeType
        fun interceptNonStepToDefault(@DefaultCall defaultCall: Callable<Any?>): Any? {
            return defaultCall.call()
        }

    }
}
