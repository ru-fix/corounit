package ru.fix.corounit.allure

import io.qameta.allure.AllureConstants
import io.qameta.allure.model.*
import io.qameta.allure.util.ResultsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringReader
import java.time.Clock
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext


object ContextStepper {
    private val clock = Clock.systemUTC()
    private val threadLocalContext = ThreadLocal<AllureContext>()

    suspend fun contextFromCoroutine() = coroutineContext[AllureContext.Key]!!

    suspend fun contextToThreadLocal(){
        val context = coroutineContext[AllureContext.Key]!!
        threadLocalContext.set(context)
    }

    fun contextFromThreadLocal(): AllureContext = threadLocalContext.get()!!

    suspend fun step(name: String, stepBody: suspend CoroutineScope.() -> Unit) {
        val parentContext = coroutineContext[AllureContext.Key]!!

        val step = createStep(name)

        val childContext = AllureContext(step = step)
        parentContext.children.add(childContext)

        try {
            withContext(childContext) {
                stepBody()
            }
            step.finalize(exc = null)
        } catch (exc: Exception) {
            step.finalize(exc)
        }
    }

    suspend fun attachment(name: String, body: String){
        val parentContext = coroutineContext[AllureContext.Key]!!
        attachment(name, body, parentContext)
    }
    //TODO: make methods that adds or changes StepResult synced
    fun threadLocalAttachment(name: String, body: String){
        val parentContext = threadLocalContext.get()!!
        attachment(name, body, parentContext)
    }

    private fun attachment(name: String, body: String, context: AllureContext){
        val source = UUID.randomUUID().toString() + AllureConstants.ATTACHMENT_FILE_SUFFIX + ".txt"
        AllureWriter.write(source, ByteArrayInputStream(body.toByteArray()))

        val attach = Attachment()
                .setName(name)
                .setType("text/plain")
                .setSource(source)
        context.step.attachments.add(attach)
    }

    data class StepExecution(val stepResult: StepResult, val stepContext: CoroutineContext)

    fun startStep(name: String, coroutineContext: CoroutineContext): StepExecution {
        val parentContext = coroutineContext[AllureContext.Key]!!
        val step = createStep(name)

        val childContext = AllureContext(step)
        parentContext.children.add(childContext)

        return StepExecution(step, parentContext + childContext)
    }

    fun stopStep(stepExecution: StepExecution, exc: Exception?) {
        stepExecution.stepResult.finalize(exc)
    }

    private fun createStep(name: String): StepResult {
        return StepResult()
                .setName(name)
                .setStart(clock.millis())
    }

    private fun StepResult.finalize(exc: Exception?) {
        stop = clock.millis()
        if(exc == null){
            status = Status.PASSED
        } else {
            status = ResultsUtils.getStatus(exc).get()
            statusDetails = ResultsUtils.getStatusDetails(exc).get()
        }
        stage = Stage.FINISHED
    }

}



