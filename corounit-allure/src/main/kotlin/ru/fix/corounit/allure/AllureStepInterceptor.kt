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
                @Morph invocation: MorphingInvocation,
                @Origin method: Method,
                @AllArguments args: Array<Any?>): Any? {

            val continuation = args.findLast { it is Continuation<*> } as? Continuation<Any?>
            require(continuation != null)

            val step = ContextStepper.startStep(method.name, continuation.context)

            val newArgs = args.copyOf()
            newArgs[newArgs.lastIndex] = object : Continuation<Any?> {
                override val context: CoroutineContext
                    get() = step.stepContext

                override fun resumeWith(result: Result<Any?>) {
                    continuation.resumeWith(result)
                }
            }

            try {
                val result = invocation.invoke(newArgs)
                ContextStepper.stopStep(step, exc = null)
                return result
            } catch (exc: Exception) {
                ContextStepper.stopStep(step, exc)
                throw exc
            }
        }
    }
}
