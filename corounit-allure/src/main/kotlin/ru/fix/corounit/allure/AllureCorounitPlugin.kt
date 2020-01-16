package ru.fix.corounit.allure

import io.qameta.allure.model.Label
import io.qameta.allure.model.Status
import io.qameta.allure.model.StepResult
import io.qameta.allure.model.TestResult
import io.qameta.allure.util.ResultsUtils
import kotlinx.coroutines.CoroutineName
import ru.fix.corounit.engine.CorounitContext
import ru.fix.corounit.engine.CorounitPlugin
import java.time.Clock
import java.util.*
import kotlin.coroutines.CoroutineContext


class AllureCorounitPlugin : CorounitPlugin {

    private val clock = Clock.systemUTC()

    override suspend fun beforeTestMethod(testMethodContext: CoroutineContext): CoroutineContext {
        return testMethodContext +
                AllureContext(StepResult()) +
                TestResultContext(TestResult().apply {
                    start = clock.millis()
                    name = CorounitContext.fromContext(testMethodContext).testMethod.name
                })
    }

    override suspend fun afterTestMethod(testMethodContext: CoroutineContext, exc: Exception?) {
        val result = testMethodContext[TestResultContext.Key]!!.allureResult.apply {
            stop = clock.millis()
            if (exc == null) {
                status = Status.PASSED
            } else {
                status = ResultsUtils.getStatus(exc).get()
                statusDetails = ResultsUtils.getStatusDetails(exc).get()
            }
            uuid = UUID.randomUUID().toString()
            labels.add(Label()
                    .setName(ResultsUtils.THREAD_LABEL_NAME)
                    .setValue(testMethodContext.get(CoroutineName)?.name ?: uuid))

            val context = testMethodContext[AllureContext.Key]!!
            populateSteps(listOf(context))

            steps.addAll(context.step.steps)
            attachments.addAll(context.step.attachments)
        }
        AllureWriter.write(result)
    }

    private tailrec fun populateSteps(contexts: List<AllureContext>) {
        if (contexts.isEmpty()) return
        for (context in contexts) {
            context.step.steps.addAll(context.children.map { it.step })
        }
        populateSteps(contexts.flatMap { it.children })
    }
}