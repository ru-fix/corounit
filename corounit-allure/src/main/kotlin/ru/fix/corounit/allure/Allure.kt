package ru.fix.corounit.allure

import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

fun <T: Any> createStepClassInstance(clazz: KClass<T>): T = AllureAspect.newAspectedInstanceViaSubtyping(clazz)

suspend operator fun String.invoke(stepBody: suspend CoroutineScope.()->Unit) {
    AllureStep.fromCurrentCoroutineContext().step(this, stepBody)
}

@UseExperimental(ExperimentalContracts::class)
suspend fun <T> repeatUntilSuccess(timeout: Int = 15_000,
                                   delay: Int = 1_000,
                                   block: suspend ()->T):T {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }

    var result: T? = null

    AllureStep.fromCurrentCoroutineContext().step(
            "repeat until success, at most ${java.time.Duration.ofMillis(timeout.toLong())}") {
        val start = System.currentTimeMillis()
        fun isTimeOut() = System.currentTimeMillis() > start + timeout

        var iteration = 0
        while (true) {
            try {
                iteration++
                AllureStep.fromCurrentCoroutineContext().step("iteration $iteration"){
                    result = block()
                }
                break
            } catch (thr: Throwable) {
                if (isTimeOut()) {
                    throw thr
                } else {
                    kotlinx.coroutines.delay(delay.toLong())
                    continue
                }
            }
        }
    }
    return result as T
}
