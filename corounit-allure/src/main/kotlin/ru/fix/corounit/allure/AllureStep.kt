package ru.fix.corounit.allure

import io.qameta.allure.AllureConstants
import io.qameta.allure.model.Attachment
import io.qameta.allure.model.Stage
import io.qameta.allure.model.Status
import io.qameta.allure.model.StepResult
import io.qameta.allure.util.ResultsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.time.Clock
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Context element represents current Allure step.
 * Stored within CoroutineContext or ThreadLocal
 *
 * AllureStep represents tree of step invocations.
 * Test method invocation is a root AllureStep.
 */
class AllureStep : AbstractCoroutineContextElement(Key) {
    val step = StepResult()
    val children: ConcurrentLinkedDeque<AllureStep> = ConcurrentLinkedDeque()

    companion object {
        private val Key = object : CoroutineContext.Key<AllureStep> {}
        private val clock = Clock.systemUTC()
        private val threadLocal = ThreadLocal<AllureStep>()

        fun fromCoroutineContext(coroutineContext: CoroutineContext) = coroutineContext[Key]!!

        suspend fun fromCurrentCoroutineContext() = coroutineContext[Key]!!

        suspend fun fromCurrentCoroutineContextToToThreadLocal() {
            val context = coroutineContext[Key]!!
            threadLocal.set(context)
        }

        fun fromThreadLocal(): AllureStep = threadLocal.get()!!

        suspend fun attachment(name: String, body: String) {
            fromCurrentCoroutineContext().attachment(name, body)
        }

    }

    suspend fun step(name: String, stepBody: suspend CoroutineScope.() -> Unit) {
        val parentContext = coroutineContext[Key]!!

        val childContext = createStep(name)
        parentContext.children.add(childContext)

        try {
            withContext(childContext) {
                stepBody()
            }
            childContext.stop(thr = null)
        } catch (thr: Throwable) {
            childContext.stop(thr)
            throw thr
        }
    }

    /**
     * Gets parent step context from parentCoroutineContext coroutine context
     * Creates child step context
     * @return new coroutine context derived from parentCoroutineContext and contained child step context
     */
    fun startChildStepWithCoroutineContext(name: String, parentCoroutineContext: CoroutineContext): CoroutineContext {
        val parentStep = parentCoroutineContext[Key]!!

        val childStepContext = createStep(name)
        parentStep.children.add(childStepContext)

        return parentStep + childStepContext
    }

    private fun createStep(name: String) =
            AllureStep().also {
                it.step.setName(name)
                        .setStart(clock.millis())
            }


    @Synchronized
    fun stop(thr: Throwable?) {
        step.stop = clock.millis()
        if (thr == null) {
            step.status = Status.PASSED
        } else {
            step.status = ResultsUtils.getStatus(thr).get()
            step.statusDetails = ResultsUtils.getStatusDetails(thr).get()
        }
        step.stage = Stage.FINISHED
    }


    @Synchronized
    fun attachment(name: String, body: String) {
        val source = UUID.randomUUID().toString() + AllureConstants.ATTACHMENT_FILE_SUFFIX + ".txt"
        AllureWriter.write(source, ByteArrayInputStream(body.toByteArray()))

        val attach = Attachment()
                .setName(name)
                .setType("text/plain")
                .setSource(source)
        step.attachments.add(attach)
    }

    @Synchronized
    fun <T> withStepResult(block: (step: StepResult) -> T): T = block(step)
}