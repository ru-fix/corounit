package ru.fix.corounit.allure

import mu.KotlinLogging
import net.bytebuddy.implementation.bind.annotation.AllArguments
import net.bytebuddy.implementation.bind.annotation.Morph
import net.bytebuddy.implementation.bind.annotation.Origin
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

class AllureStepInterceptor {
    companion object {
        @JvmStatic
        fun intercept(
                @Morph interceptedInvocation: MorphingInterceptedInvocation,
                @Origin method: Method,
                @AllArguments args: Array<Any?>): Any? {

            val continuation = args.findLast { it is Continuation<*> } as? Continuation<Any?>
            require(continuation != null)

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
                childStep.stop(exc = null)
                return result
            } catch (exc: Exception) {
                childStep.stop(exc)
                throw exc
            }
        }
    }
}
