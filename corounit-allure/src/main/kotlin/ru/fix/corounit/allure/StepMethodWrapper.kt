package ru.fix.corounit.allure

import mu.KotlinLogging
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction

private val log = KotlinLogging.logger { }

class IllegalMethodAspected(method: Method, message: String) :
        Exception("$message Corounit Allure aspected method should be suspendable: $method")

class StepMethodWrapper(
        originMethod: Method,
        originArgs: Array<Any?>) {

    private val newArgs: Array<Any?>
    private val methodAllureStep: AllureStep?

    init {
        val originContinuation = originArgs.findLast { it is Continuation<*> } as? Continuation<Any?>
                ?: throw IllegalMethodAspected(originMethod, "Continuation argument not found.")

        val parentStep = AllureStep.tryFromCoroutineContext(originContinuation.context)
                ?: throw IllegalMethodAspected(originMethod,
                        "Parent AllureStep not found in coroutine context")

        val parameterNames = originMethod.kotlinFunction?.parameters
                ?.filter { it.kind == KParameter.Kind.VALUE }
                ?.map { it.name }
                ?: originMethod.parameters.map { it.name }

        val title = "" +
                originMethod.name + " " +
                parameterNames
                        .zip(originArgs)
                        .map { (name, arg) -> "$name: $arg" }
                        .joinToString(prefix = "(", separator = ", ", postfix = ")")

        val childCoroutineContext = parentStep.startChildStepWithCoroutineContext(title, originContinuation.context)
        methodAllureStep = AllureStep.fromCoroutineContext(childCoroutineContext)

        newArgs = originArgs.copyOf()
        newArgs[newArgs.lastIndex] = object : Continuation<Any?> {
            override val context: CoroutineContext
                get() = childCoroutineContext

            override fun resumeWith(result: Result<Any?>) {
                originContinuation.resumeWith(result)
            }
        }
    }

    fun wrappedInvoke(originalMethodInvocation: (newArgs: Array<Any?>) -> Any?): Any? {
        try {
            val result = originalMethodInvocation(newArgs)
            methodAllureStep?.stop()
            return result
        } catch (thr: Throwable) {
            methodAllureStep?.stop(thr)
            throw thr
        }
    }
}