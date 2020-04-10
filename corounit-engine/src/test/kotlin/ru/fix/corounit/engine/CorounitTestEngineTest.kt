package ru.fix.corounit.engine

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.*
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
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
    private val beforeEach = ConcurrentLinkedDeque<Int>()
    private val afterEach = ConcurrentLinkedDeque<Int>()
    private val beforeAll = ConcurrentLinkedDeque<Int>()
    private val afterAll = ConcurrentLinkedDeque<Int>()

    private val tests = ConcurrentLinkedDeque<Int>()
    private val counter = AtomicInteger()

    open fun reset() {
        counter.set(0)
        beforeEach.clear()
        afterEach.clear()
        tests.clear()
        beforeAll.clear()
        afterAll.clear()
    }

    fun beforeEachInvoked() {
        beforeEach.addLast(counter.incrementAndGet())
    }

    fun beforeAllInvoked() {
        beforeAll.addLast(counter.incrementAndGet())
    }

    fun testInvoked(): Int {
        val count = counter.incrementAndGet()
        tests.addLast(count)
        return count
    }

    fun afterEachInvoked() {
        afterEach.addLast(counter.incrementAndGet())
    }

    fun afterAllInvoked() {
        afterAll.addLast(counter.incrementAndGet())
    }

    val beforeEachState get() = beforeEach.toList()
    val beforeAllState get() = beforeAll.toList()
    val testState get() = tests.toList()
    val afterEachState get() = afterEach.toList()
    val afterAllState get() = afterAll.toList()

}

@TestInstance(PER_CLASS)
class MyTestWithoutAnnotations {
    companion object : TestState()

    suspend fun beforeAll() {
        beforeAllInvoked()
    }

    suspend fun afterAll() {
        afterAllInvoked()
    }

    @Test
    suspend fun myTest() {
        testInvoked()
    }
}

@TestInstance(PER_CLASS)
class MyTestWithAnnotations {
    companion object : TestState()

    @BeforeAll
    fun setUp() {
        beforeAllInvoked()
    }

    @AfterAll
    suspend fun tearDown() {
        afterAllInvoked()
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

object CorounitConfig : CorounitPlugin, CorounitPluginsProvider {
    val beforeAllTestClassesInvocationCount = AtomicInteger()
    val skipMethodsInvocationCount = AtomicInteger()
    val providerBeforeAllTestClassesInvocationCount = AtomicInteger()

    fun reset(){
        beforeAllTestClassesInvocationCount.set(0)
        skipMethodsInvocationCount.set(0)
        providerBeforeAllTestClassesInvocationCount.set(0)
    }

    override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
        beforeAllTestClassesInvocationCount.incrementAndGet()
        return super.beforeAllTestClasses(globalContext)
    }

    override suspend fun skipTestMethod(testMethodContext: CoroutineContext, reason: String) {
        skipMethodsInvocationCount.incrementAndGet()
    }

    override fun plugins(): List<CorounitPlugin> {
        return listOf(object: CorounitPlugin{
            override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
                providerBeforeAllTestClassesInvocationCount.incrementAndGet()
                return super.beforeAllTestClasses(globalContext)
            }
        })
    }
}

class MyTestClassForPlugin {
    @Test
    suspend fun test() {
    }

}

class TestClassInstancePerMethodInvocation {
    companion object : TestState(){
        val instances = ConcurrentLinkedDeque<TestClassInstancePerMethodInvocation>()
        override fun reset() {
            super.reset()
            instances.clear()
        }
    }

    @Test
    suspend fun firstMethod() {
        instances.addLast(this)
        testInvoked()
    }

    @Test
    suspend fun secondMethod() {
        instances.addLast(this)
        testInvoked()
    }

    suspend fun beforeEach(){
        beforeEachInvoked()
    }

    fun beforeAll() {
        beforeAllInvoked()
    }

    suspend fun afterAll() {
        afterAllInvoked()
    }
}

class TestWithDisabledMethod{
    companion object{
        val test1 = AtomicBoolean()
        val test2 = AtomicBoolean()
        fun reset(){
            test1.set(false)
            test2.set(false)
        }
    }

    @Disabled
    @Test
    suspend fun test1(){
        test1.set(true)
    }
    @Test
    suspend fun test2(){
        test2.set(true)
    }
}

@TestInstance(PER_CLASS)
class TestClassInstancePerClassInvocationWithAnnotation {
    companion object : TestState() {
        val instances = ConcurrentLinkedDeque<TestClassInstancePerClassInvocationWithAnnotation>()

        override fun reset() {
            super.reset()
            instances.clear()
        }
    }
    suspend fun beforeAll() {
        beforeAllInvoked()

    }

    fun afterAll() {
        afterAllInvoked()
    }

    @Test
    suspend fun firstMethod() {
        instances.addLast(this)
        testInvoked()
    }

    @Test
    suspend fun secondMethod() {
        instances.addLast(this)
        testInvoked()
    }
}

class BeforeAfterEach{
    companion object: TestState(){
    }
    suspend fun beforeEach(){
        beforeEachInvoked()
    }
    suspend fun afterEach(){
        afterEachInvoked()
    }

    @Test
    suspend fun firstMethod(){
        testInvoked()
    }
    @Test
    suspend fun secondMethod(){
        testInvoked()
    }
}

class BeforeAfterEachWithAnnotations{
    companion object: TestState(){
    }

    @BeforeEach
    suspend fun before(){
        beforeEachInvoked()
    }
    @AfterEach
    suspend fun after(){
        afterEachInvoked()
    }

    @Test
    suspend fun firstMethod(){
        testInvoked()
    }
    @Test
    suspend fun secondMethod(){
        testInvoked()
    }
}

class CorounitTestEngineTest {

    private val engine: CorounitTestEngine = CorounitTestEngine()

    @Test
    fun `beforeAll and afterAll invoked in test suite without annotations`() {
        val executionRequest = emulateDiscoveryStepForTestClass<MyTestWithoutAnnotations>()

        MyTestWithoutAnnotations.reset()

        engine.execute(executionRequest)

        MyTestWithoutAnnotations.beforeEachState.shouldContainExactly()
        MyTestWithoutAnnotations.beforeAllState.shouldContainExactly(1)
        MyTestWithoutAnnotations.testState.shouldContainExactly(2)
        MyTestWithoutAnnotations.afterEachState.shouldContainExactly()
        MyTestWithoutAnnotations.afterAllState.shouldContainExactly(3)
    }

    @Test
    fun `beforeAll and afterAll invoked in test suite with annotations`() {
        val executionRequest = emulateDiscoveryStepForTestClass<MyTestWithAnnotations>()

        MyTestWithAnnotations.reset()
        engine.execute(executionRequest)

        MyTestWithAnnotations.beforeEachState.shouldContainExactly()
        MyTestWithAnnotations.beforeAllState.shouldContainExactly(1)
        MyTestWithAnnotations.testState.shouldContainExactly(2)
        MyTestWithAnnotations.afterEachState.shouldContainExactly()
        MyTestWithAnnotations.afterAllState.shouldContainExactly(3)
    }

    @Test
    fun `first test method waits others to complete and whole suite passes without timeout`() {
        val executionRequest = emulateDiscoveryStepForTestClass<FirstMethodsWaitsOthersTest>()

        FirstMethodsWaitsOthersTest.reset()
        FirstMethodsWaitsOthersTest.shouldFirstMethodWaitOthers.set(true)
        engine.execute(executionRequest)
        FirstMethodsWaitsOthersTest.shouldFirstMethodWaitOthers.set(false)

        FirstMethodsWaitsOthersTest.beforeEachState.shouldContainExactly()
        FirstMethodsWaitsOthersTest.testState.shouldContainExactlyInAnyOrder(1, 2, 3)
        FirstMethodsWaitsOthersTest.afterEachState.shouldContainExactly()
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
        CorounitConfig.reset()

        val executionRequest = emulateDiscoveryStepForTestClass<MyTestClassForPlugin>()
        engine.execute(executionRequest)

        CorounitConfig.beforeAllTestClassesInvocationCount.get().shouldBeGreaterThan(0)
        CorounitConfig.providerBeforeAllTestClassesInvocationCount.get().shouldBeGreaterThan(0)
    }

    @Test
    fun `new test instance created for each method invocation by default`() {

        val executionRequest = emulateDiscoveryStepForTestClass<TestClassInstancePerMethodInvocation>()
        TestClassInstancePerMethodInvocation.reset()

        engine.execute(executionRequest)

        TestClassInstancePerMethodInvocation.instances.shouldHaveSize(2)
        (TestClassInstancePerMethodInvocation.instances.first !== TestClassInstancePerMethodInvocation.instances.last)
                .shouldBeTrue()

        TestClassInstancePerMethodInvocation.beforeAllState.shouldContainExactly()
        TestClassInstancePerMethodInvocation.beforeEachState.shouldHaveSize(2)
        TestClassInstancePerMethodInvocation.testState.shouldHaveSize(2)
        TestClassInstancePerMethodInvocation.afterEachState.shouldBeEmpty()
        TestClassInstancePerMethodInvocation.afterAllState.shouldBeEmpty()
    }

    @Test
    fun `single instance create for all method invocation if annotation present`() {

        val executionRequest = emulateDiscoveryStepForTestClass<TestClassInstancePerClassInvocationWithAnnotation>()
        TestClassInstancePerClassInvocationWithAnnotation.reset()
        engine.execute(executionRequest)

        TestClassInstancePerClassInvocationWithAnnotation.instances.shouldHaveSize(2)
        (TestClassInstancePerClassInvocationWithAnnotation.instances.first ===
                TestClassInstancePerClassInvocationWithAnnotation.instances.last)
                .shouldBeTrue()

        TestClassInstancePerClassInvocationWithAnnotation.beforeAllState.shouldContainExactly(1)
        TestClassInstancePerClassInvocationWithAnnotation.beforeEachState.shouldContainExactly()
        TestClassInstancePerClassInvocationWithAnnotation.testState.shouldContainExactly(2, 3)
        TestClassInstancePerClassInvocationWithAnnotation.afterEachState.shouldContainExactly()
        TestClassInstancePerClassInvocationWithAnnotation.afterAllState.shouldContainExactly(4)

    }

    @Test
    fun `beforeEach and afterEach invoked in test suite without annotations`() {
        val executionRequest = emulateDiscoveryStepForTestClass<BeforeAfterEach>()

        BeforeAfterEach.reset()
        engine.execute(executionRequest)

        BeforeAfterEach.beforeAllState.shouldBeEmpty()
        BeforeAfterEach.beforeEachState.shouldHaveSize(2)
        BeforeAfterEach.testState.shouldHaveSize(2)
        BeforeAfterEach.afterEachState.shouldHaveSize(2)
        BeforeAfterEach.afterAllState.shouldBeEmpty()
    }

    @Test
    fun `disabled test does not start`() {
        val executionRequest = emulateDiscoveryStepForTestClass<TestWithDisabledMethod>()

        CorounitConfig.reset()
        TestWithDisabledMethod.reset()
        engine.execute(executionRequest)

        TestWithDisabledMethod.test1.get().shouldBe(false)
        TestWithDisabledMethod.test2.get().shouldBe(true)
        CorounitConfig.skipMethodsInvocationCount.get().shouldBeGreaterThan(0)
    }


    @Test
    fun `beforeEach and afterEach invoked in test suite with annotations`() {
        val executionRequest = emulateDiscoveryStepForTestClass<BeforeAfterEachWithAnnotations>()

        BeforeAfterEachWithAnnotations.reset()
        engine.execute(executionRequest)

        BeforeAfterEachWithAnnotations.beforeAllState.shouldBeEmpty()
        BeforeAfterEachWithAnnotations.beforeEachState.shouldContain(1)
        BeforeAfterEachWithAnnotations.beforeEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.testState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldContain(6)
        BeforeAfterEachWithAnnotations.afterAllState.shouldBeEmpty()
    }

    private inline fun <reified T> mockDiscoveryRequest(): EngineDiscoveryRequest {
        val discoveryRequest = mockk<EngineDiscoveryRequest>()
        every { discoveryRequest.getSelectorsByType(MethodSelector::class.java) } returns mutableListOf()

        val selector = mockk<ClassSelector>()

        val selectorClass = slot<Class<DiscoverySelector>>()
        every { discoveryRequest.getSelectorsByType<DiscoverySelector>( capture(selectorClass) ) } answers {
            if(selectorClass.captured ==  ClassSelector::class.java){
                mutableListOf<DiscoverySelector>(selector)
            } else {
                emptyList()
            }
        }
        every { discoveryRequest.getFiltersByType<DiscoveryFilter<*>>(any()) } returns emptyList()

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