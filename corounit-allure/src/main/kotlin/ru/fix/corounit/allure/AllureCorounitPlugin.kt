package ru.fix.corounit.allure

import io.qameta.allure.AllureResultsWriter
import io.qameta.allure.Description
import io.qameta.allure.model.Label
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.model.TestResult
import io.qameta.allure.util.AnnotationUtils
import io.qameta.allure.util.ResultsUtils
import kotlinx.coroutines.CoroutineName
import ru.fix.corounit.engine.CorounitPlugin
import ru.fix.corounit.engine.TestClassContextElement
import ru.fix.corounit.engine.TestMethodContextElement
import java.time.Clock
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod


class AllureCorounitPlugin(
        private val clock: Clock = Clock.systemUTC(),
        private val writer: AllureResultsWriter = AllureWriter
) : CorounitPlugin {

    override suspend fun beforeTestMethod(testMethodContext: CoroutineContext): CoroutineContext {
        val testResult = createAllureTestResult(testMethodContext)
        return testMethodContext +
                AllureStep() +
                TestResultContextElement(testResult)
    }

    private fun createAllureTestResult(testMethodContext: CoroutineContext): TestResult {
        val testClass = testMethodContext[TestClassContextElement]!!.testClass
        val testMethod = testMethodContext[TestMethodContextElement]!!.testMethod

        return TestResult().apply {
            start = clock.millis()
            name = testMethod.name
            fullName = testMethod.name
            testCaseId = testClass.qualifiedName
            uuid = UUID.randomUUID().toString()
            historyId = ResultsUtils.md5("${testClass.qualifiedName}::${testMethod.name}")
            description = testMethod.findAnnotation<Description>()?.value

            val labelsMap = (listOf(ResultsUtils.createFrameworkLabel("corounit"),
                    ResultsUtils.createPackageLabel(testClass.qualifiedName),
                    ResultsUtils.createTestClassLabel(testClass.qualifiedName),
                    ResultsUtils.createTestMethodLabel(testMethod.name),
                    ResultsUtils.createSuiteLabel(testClass.qualifiedName)) +
                    AnnotationUtils.getLabels(testClass.java) +
                    AnnotationUtils.getLabels(testMethod.javaMethod))
                    .map { it.name to it.value }
                    .toMap(HashMap())

            (testMethod.findAnnotation<Package>() ?: testClass.findAnnotation<Package>())?.let {
                labelsMap.put(ResultsUtils.PACKAGE_LABEL_NAME, it.name)
            }
            labels.addAll(labelsMap.map { (k, v) -> Label().setName(k).setValue(v) })


            links.addAll(AnnotationUtils.getLinks(testClass.java))
            links.addAll(AnnotationUtils.getLinks(testMethod.javaMethod))
        }
    }

    override suspend fun afterTestMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
        val testResult = testMethodContext[TestResultContextElement]!!.allureResult
        val methodInvocationStep = AllureStep.fromCoroutineContext(testMethodContext)

        testResult.apply {
            populateSteps(listOf(methodInvocationStep))
            steps.addAll(methodInvocationStep.step.steps)
            attachments.addAll(methodInvocationStep.step.attachments)
        }

        if (thr == null) {
            finalizeAllureTestResult(
                    testResult = testResult,
                    testMethodContext = testMethodContext,
                    status = Status.PASSED)
        } else {
            finalizeAllureTestResult(
                    testResult =  testResult,
                    testMethodContext = testMethodContext,
                    status = ResultsUtils.getStatus(thr).get(),
                    statusDetails = ResultsUtils.getStatusDetails(thr).get())
        }

        writer.write(testResult)
    }

    override suspend fun skipTestMethod(testMethodContext: CoroutineContext, reason: String) {
        val testResult = createAllureTestResult(testMethodContext)
        finalizeAllureTestResult(testResult, testMethodContext, Status.SKIPPED, StatusDetails().setMessage(reason))
        writer.write(testResult)
    }

    private fun finalizeAllureTestResult(
            testResult: TestResult,
            testMethodContext: CoroutineContext,
            status: Status,
            statusDetails: StatusDetails? = null){

        testResult.apply{
            stop = clock.millis()
            this.status = status
            this.statusDetails = statusDetails

            labels.addAll(listOf(
                    Label().setName(ResultsUtils.THREAD_LABEL_NAME)
                            .setValue(testMethodContext.get(CoroutineName)?.name ?: uuid)
            ))
        }
    }

    private tailrec fun populateSteps(contexts: List<AllureStep>) {
        if (contexts.isEmpty()) return
        for (context in contexts) {
            context.step.steps.addAll(context.children.map { it.step })
        }
        populateSteps(contexts.flatMap { it.children })
    }
}