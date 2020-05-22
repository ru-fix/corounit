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
        private val originMethod: Method,
        private val originArgs: Array<Any?>) {

    private val newArgs: Array<Any?>
    private val methodAllureStep: AllureStep?
    private val originContinuation: Continuation<Any?>?

    init {
        originContinuation = originArgs.findLast { it is Continuation<*> } as? Continuation<Any?>
        if (originContinuation == null){
            newArgs = originArgs
            methodAllureStep = null

        } else {
            val parentStep = AllureStep.tryFromCoroutineContext(originContinuation.context)
            if(parentStep == null){
                newArgs = originArgs
                methodAllureStep = null

            } else {
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

                if (!isMethodInvokedRecursivelyWithItsInnerContinuation()) {
                    newArgs[newArgs.lastIndex] = object : Continuation<Any?> {
                        override val context: CoroutineContext
                            get() = childCoroutineContext

                        override fun resumeWith(result: Result<Any?>) {
                            methodAllureStep.stop()
                            originContinuation.resumeWith(result)
                        }
                    }
                }
            }
        }
    }

    private fun isMethodInvokedRecursivelyWithItsInnerContinuation() =
            originContinuation?.javaClass?.enclosingClass?.name ==
            originMethod.declaringClass.name

    fun wrappedInvoke(originalMethodInvocation: (newArgs: Array<Any?>) -> Any?): Any? {
        try {
            val result = originalMethodInvocation(newArgs)
            if (result != kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                methodAllureStep?.stop()
            }
            return result
        } catch (thr: Throwable) {
            methodAllureStep?.stop(thr)
            throw thr
        }
    }
}