package ru.fix.corounit.allure

import mu.KotlinLogging
import net.bytebuddy.implementation.bind.annotation.*
import java.lang.reflect.Method
import java.util.concurrent.Callable
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

class AllureStepInterceptor {
    companion object {
        @JvmStatic
        @BindingPriority(BindingPriority.DEFAULT * 100)
        @RuntimeType
        fun interceptStep(
                @Morph interceptedInvocation: MorphingInterceptedInvocation,
                @Origin method: Method,
                @AllArguments args: Array<Any?>): Any? {

            val continuation = args.findLast { it is Continuation<*> } as? Continuation<Any?>
                    ?: return interceptedInvocation.invoke(args)

            val parentStep = AllureStep.fromCoroutineContext(continuation.context)
            val childCoroutineContext = parentStep.startChildStepWithCoroutineContext(method.name, continuation.context)
            val childStep = AllureStep.fromCoroutineContext(childCoroutineContext)

            val newArgs = args.copyOf()
            newArgs[newArgs.lastIndex] = object : Continuation<Any?> {
                override val context: CoroutineContext
                    get() = childCoroutineContext

                override fun resumeWith(result: Result<Any?>) {
                    continuation.resumeWith(result)
                }
            }

            try {
                val result = interceptedInvocation.invoke(newArgs)
                childStep.stop(thr = null)
                return result
            } catch (thr: Throwable) {
                childStep.stop(thr)
                throw thr
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
