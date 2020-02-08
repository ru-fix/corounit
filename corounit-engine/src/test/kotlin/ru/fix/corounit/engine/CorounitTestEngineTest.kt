package ru.fix.corounit.engine

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import mu.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.engine.reporting.ReportEntry
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

open class TestState {
    private val before = AtomicInteger()
    private val after = AtomicInteger()
    private val tests = ConcurrentLinkedDeque<Int>()
    private val counter = AtomicInteger()

    fun reset() {
        counter.set(0)
        before.set(0)
        after.set(0)
        tests.clear()
    }

    fun beforeInvoked() {
        before.set(counter.incrementAndGet())
    }

    fun testInvoked(): Int {
        val count = counter.incrementAndGet()
        tests.addLast(count)
        return count
    }

    fun afterInvoked() {
        after.set(counter.incrementAndGet())
    }

    val beforeState get() = before.get()
    val testState get() = tests.toList()
    val afterState get() = after.get()

}

class MyTestWithoutAnnotations {
    companion object : TestState()

    suspend fun beforeAll() {
        beforeInvoked()
    }

    suspend fun afterAll() {
        afterInvoked()
    }

    @Test
    suspend fun myTest() {
        testInvoked()
    }
}

class MyTestWithAnnotations {
    companion object : TestState()

    @BeforeAll
    suspend fun setUp() {
        beforeInvoked()
    }

    @AfterAll
    suspend fun tearDown() {
        afterInvoked()
    }

    @Test
    suspend fun myTest() {
        testInvoked()
    }
}

/**
 * This test class detected by JunitTestEngine and executed during build
 * as separate test suite.
 * When we use this class as a test source we enable trapping behaviour explicitly.
 */
class FirstMethodsWaitsOthersTest {
    companion object : TestState() {
        val shouldFirstMethodWaitOthers = AtomicBoolean()
    }

    private fun concurrentTestInvoked() {
        val amIaFirstInvokedTest = testInvoked() == 1
        if (amIaFirstInvokedTest) {
            while (shouldFirstMethodWaitOthers.get() && !testState.containsAll(listOf(2, 3))) {
                Thread.sleep(100)
                log.info {
                    "Waiting for test state to contains [2, 3]." +
                            " Current state: $testState"
                }
            }
        }
    }

    @Test
    suspend fun myTest1() {
        concurrentTestInvoked()
    }

    @Test
    suspend fun myTest2() {
        concurrentTestInvoked()
    }

    @Test
    suspend fun myTest3() {
        concurrentTestInvoked()
    }
}

/**
 * This test class detected by JunitTestEngine and executed during build
 * as separate test suite.
 * When we use this class as a test source we enable trapping behaviour explicitly.
 */
class MyTestForListener {
    companion object {
        val shouldFailedTestFail = AtomicBoolean()
    }

    @Test
    suspend fun mySuccessTest() {
    }

    @Test
    suspend fun myFailedTest() {
        if (shouldFailedTestFail.get()) {
            throw Exception("oops")
        }
    }

}


class EngineExecutionListenerTrap : EngineExecutionListener {
    var reportedTests = ConcurrentLinkedDeque<Pair<TestDescriptor, TestExecutionResult>>()

    override fun executionFinished(descriptor: TestDescriptor, result: TestExecutionResult) {
        reportedTests.addLast(descriptor to result)
    }

    override fun reportingEntryPublished(p0: TestDescriptor?, p1: ReportEntry?) {
    }

    override fun executionSkipped(p0: TestDescriptor?, p1: String?) {
    }

    override fun executionStarted(p0: TestDescriptor?) {
    }

    override fun dynamicTestRegistered(p0: TestDescriptor?) {
    }
}

object CorounitConfig : CorounitPlugin {
    val invokedTimes = AtomicInteger()


    override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
        invokedTimes.incrementAndGet()
        return super.beforeAllTestClasses(globalContext)
    }
}

class MyTestClassForPlugin {
    @Test
    suspend fun test() {
    }

}

class CorounitTestEngineTest {

    private val engine: CorounitTestEngine = CorounitTestEngine()

    @Test
    fun `beforeAll and afterAll invoked in test suite without annotations`() {
        val executionRequest = emulateDiscoveryStepForTestClass<MyTestWithoutAnnotations>()

        MyTestWithoutAnnotations.reset()

        engine.execute(executionRequest)

        MyTestWithoutAnnotations.beforeState.shouldBe(1)
        MyTestWithoutAnnotations.testState.shouldContain(2)
        MyTestWithoutAnnotations.afterState.shouldBe(3)
    }

    @Test
    fun `beforeAll and afterAll invoked in test suite with annotations`() {
        val executionRequest = emulateDiscoveryStepForTestClass<MyTestWithAnnotations>()

        MyTestWithAnnotations.reset()
        engine.execute(executionRequest)

        MyTestWithAnnotations.beforeState.shouldBe(1)
        MyTestWithAnnotations.testState.shouldContain(2)
        MyTestWithAnnotations.afterState.shouldBe(3)
    }

    @Test
    fun `first test method waits others to complete and whole suite passes without timeout`() {
        val executionRequest = emulateDiscoveryStepForTestClass<FirstMethodsWaitsOthersTest>()

        FirstMethodsWaitsOthersTest.reset()
        FirstMethodsWaitsOthersTest.shouldFirstMethodWaitOthers.set(true)
        engine.execute(executionRequest)
        FirstMethodsWaitsOthersTest.shouldFirstMethodWaitOthers.set(false)

        FirstMethodsWaitsOthersTest.beforeState.shouldBe(0)
        FirstMethodsWaitsOthersTest.testState.asClue { it.shouldContainAll(1, 2, 3) }
        FirstMethodsWaitsOthersTest.afterState.shouldBe(0)
    }

    @Test
    fun `success and failed tests reported to junit listener`() {
        val trapListener = EngineExecutionListenerTrap()
        val executionRequest = emulateDiscoveryStepForTestClass<MyTestForListener>(trapListener)

        MyTestForListener.shouldFailedTestFail.set(true)
        engine.execute(executionRequest)

        trapListener.reportedTests
                .single {
                    it.first.displayName.contains(MyTestForListener::mySuccessTest.name)
                }
                .second.status.shouldBe(TestExecutionResult.Status.SUCCESSFUL)

        trapListener.reportedTests
                .single {
                    it.first.displayName.contains(MyTestForListener::myFailedTest.name)
                }
                .second.apply {
            status.shouldBe(TestExecutionResult.Status.FAILED)
            throwable.shouldNotBeNull()
        }
    }

    @Test
    fun `plugin object located in same package is invoked`() {
        CorounitConfig.invokedTimes.set(0)

        val executionRequest = emulateDiscoveryStepForTestClass<MyTestClassForPlugin>()
        engine.execute(executionRequest)

        CorounitConfig.invokedTimes.get().shouldBeGreaterThan(0)
    }


    private inline fun <reified T> mockDiscoveryRequest(): EngineDiscoveryRequest {
        val discoveryRequest = mockk<EngineDiscoveryRequest>()
        every { discoveryRequest.getSelectorsByType(MethodSelector::class.java) } returns mutableListOf()

        val selector = mockk<ClassSelector>()
        every { discoveryRequest.getSelectorsByType(ClassSelector::class.java) } returns mutableListOf(selector)

        every { selector.javaClass } returns T::class.java
        every { selector.className } returns T::class.java.name

        return discoveryRequest
    }

    private fun mockExecutionRequest(descriptor: TestDescriptor, listener: EngineExecutionListener? = null): ExecutionRequest {
        val executionRequest = mockk<ExecutionRequest>(relaxed = true)
        val config = mockk<ConfigurationParameters>(relaxed = true)
        every { executionRequest.configurationParameters } returns config
        every { executionRequest.rootTestDescriptor } returns descriptor
        every { executionRequest.engineExecutionListener } returns (listener ?: mockk(relaxed = true))
        every { config.get(any()) } returns Optional.empty()
        return executionRequest
    }

    private inline fun <reified T> emulateDiscoveryStepForTestClass(listener: EngineExecutionListener? = null): ExecutionRequest {
        val discoveryRequest = mockDiscoveryRequest<T>()
        val descriptor = engine.discover(discoveryRequest, UniqueId.forEngine("corounit"))
        return mockExecutionRequest(descriptor, listener)
    }
}