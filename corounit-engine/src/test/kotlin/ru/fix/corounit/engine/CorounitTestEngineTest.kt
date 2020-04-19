package ru.fix.corounit.engine

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.*
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.platform.engine.*
import org.junit.platform.engine.reporting.ReportEntry
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger { }

@TestInstance(PER_CLASS)
class MyTestWithoutAnnotations {
    companion object : TestClassState()

    suspend fun beforeAll() {
        beforeAllInvoked()
    }

    suspend fun afterAll() {
        afterAllInvoked()
    }

    @Test
    suspend fun myTest() {
        testMethodInvoked(1)
    }
}

@TestInstance(PER_CLASS)
class MyTestWithAnnotations {
    companion object : TestClassState()

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
        testMethodInvoked(1)
    }
}

/**
 * This test class detected by JunitTestEngine and executed during build
 * as separate test suite.
 * When we use this class as a test source we enable trapping behaviour explicitly.
 */
class FirstMethodsWaitsOthersTest {
    companion object : TestClassState() {
        val shouldFirstMethodWaitOthers = AtomicBoolean()
    }

    private fun concurrentTestInvoked() {
        val amIaFirstInvokedTest = testMethodInvoked(1) == 1
        if (amIaFirstInvokedTest) {
            while (shouldFirstMethodWaitOthers.get() && !testSequencesState.containsAll(listOf(2, 3))) {
                Thread.sleep(100)
                log.info {
                    "Waiting for test state to contains [2, 3]." +
                            " Current state: $testSequencesState"
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
    companion object : TestClassState(){
        val instances = ConcurrentLinkedDeque<TestClassInstancePerMethodInvocation>()
        override fun reset() {
            super.reset()
            instances.clear()
        }
    }

    @Test
    suspend fun firstMethod() {
        instances.addLast(this)
        testMethodInvoked(1)
    }

    @Test
    suspend fun secondMethod() {
        instances.addLast(this)
        testMethodInvoked(2)
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
    companion object : TestClassState() {
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
        testMethodInvoked(1)
    }

    @Test
    suspend fun secondMethod() {
        instances.addLast(this)
        testMethodInvoked(2)
    }
}

class BeforeAfterEach{
    companion object: TestClassState(){
    }
    suspend fun beforeEach(){
        beforeEachInvoked()
    }
    suspend fun afterEach(){
        afterEachInvoked()
    }

    @Test
    suspend fun firstMethod(){
        testMethodInvoked(1)
    }
    @Test
    suspend fun secondMethod(){
        testMethodInvoked(2)
    }
}

class BeforeAfterEachWithAnnotations{
    companion object: TestClassState(){
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
        testMethodInvoked(1)
    }
    @Test
    suspend fun secondMethod(){
        testMethodInvoked(2)
    }
}

class CorounitTestEngineTest {

    private val engine = EngineEmulator()


    @Test
    fun `beforeAll and afterAll invoked in test suite without annotations`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<MyTestWithoutAnnotations>()

        MyTestWithoutAnnotations.reset()

        engine.execute(executionRequest)

        MyTestWithoutAnnotations.beforeEachState.shouldContainExactly()
        MyTestWithoutAnnotations.beforeAllState.shouldContainExactly(1)
        MyTestWithoutAnnotations.testSequencesState.shouldContainExactly(2)
        MyTestWithoutAnnotations.afterEachState.shouldContainExactly()
        MyTestWithoutAnnotations.afterAllState.shouldContainExactly(3)
    }

    @Test
    fun `beforeAll and afterAll invoked in test suite with annotations`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<MyTestWithAnnotations>()

        MyTestWithAnnotations.reset()
        engine.execute(executionRequest)

        MyTestWithAnnotations.beforeEachState.shouldContainExactly()
        MyTestWithAnnotations.beforeAllState.shouldContainExactly(1)
        MyTestWithAnnotations.testSequencesState.shouldContainExactly(2)
        MyTestWithAnnotations.afterEachState.shouldContainExactly()
        MyTestWithAnnotations.afterAllState.shouldContainExactly(3)
    }

    @Test
    fun `first test method waits others to complete and whole suite passes without timeout`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<FirstMethodsWaitsOthersTest>()

        FirstMethodsWaitsOthersTest.reset()
        FirstMethodsWaitsOthersTest.shouldFirstMethodWaitOthers.set(true)
        engine.execute(executionRequest)
        FirstMethodsWaitsOthersTest.shouldFirstMethodWaitOthers.set(false)

        FirstMethodsWaitsOthersTest.beforeEachState.shouldContainExactly()
        FirstMethodsWaitsOthersTest.testSequencesState.shouldContainExactlyInAnyOrder(1, 2, 3)
        FirstMethodsWaitsOthersTest.afterEachState.shouldContainExactly()
    }

    @Test
    fun `success and failed tests reported to junit listener`() {
        val trapListener = EngineExecutionListenerTrap()
        val executionRequest = engine.emulateDiscoveryStepForTestClass<MyTestForListener>(trapListener)

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

        val executionRequest = engine.emulateDiscoveryStepForTestClass<MyTestClassForPlugin>()
        engine.execute(executionRequest)

        CorounitConfig.beforeAllTestClassesInvocationCount.get().shouldBeGreaterThan(0)
        CorounitConfig.providerBeforeAllTestClassesInvocationCount.get().shouldBeGreaterThan(0)
    }

    @Test
    fun `new test instance created for each method invocation by default`() {

        val executionRequest = engine.emulateDiscoveryStepForTestClass<TestClassInstancePerMethodInvocation>()
        TestClassInstancePerMethodInvocation.reset()

        engine.execute(executionRequest)

        TestClassInstancePerMethodInvocation.instances.shouldHaveSize(2)
        (TestClassInstancePerMethodInvocation.instances.first !== TestClassInstancePerMethodInvocation.instances.last)
                .shouldBeTrue()

        TestClassInstancePerMethodInvocation.beforeAllState.shouldContainExactly()
        TestClassInstancePerMethodInvocation.beforeEachState.shouldHaveSize(2)
        TestClassInstancePerMethodInvocation.testSequencesState.shouldHaveSize(2)
        TestClassInstancePerMethodInvocation.afterEachState.shouldBeEmpty()
        TestClassInstancePerMethodInvocation.afterAllState.shouldBeEmpty()
    }

    @Test
    fun `single instance create for all method invocation if annotation present`() {

        val executionRequest = engine.emulateDiscoveryStepForTestClass<TestClassInstancePerClassInvocationWithAnnotation>()
        TestClassInstancePerClassInvocationWithAnnotation.reset()
        engine.execute(executionRequest)

        TestClassInstancePerClassInvocationWithAnnotation.instances.shouldHaveSize(2)
        (TestClassInstancePerClassInvocationWithAnnotation.instances.first ===
                TestClassInstancePerClassInvocationWithAnnotation.instances.last)
                .shouldBeTrue()

        TestClassInstancePerClassInvocationWithAnnotation.beforeAllState.shouldContainExactly(1)
        TestClassInstancePerClassInvocationWithAnnotation.beforeEachState.shouldContainExactly()
        TestClassInstancePerClassInvocationWithAnnotation.testSequencesState.shouldContainExactly(2, 3)
        TestClassInstancePerClassInvocationWithAnnotation.afterEachState.shouldContainExactly()
        TestClassInstancePerClassInvocationWithAnnotation.afterAllState.shouldContainExactly(4)

    }

    @Test
    fun `beforeEach and afterEach invoked in test suite without annotations`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<BeforeAfterEach>()

        BeforeAfterEach.reset()
        engine.execute(executionRequest)

        BeforeAfterEach.beforeAllState.shouldBeEmpty()
        BeforeAfterEach.beforeEachState.shouldHaveSize(2)
        BeforeAfterEach.testSequencesState.shouldHaveSize(2)
        BeforeAfterEach.afterEachState.shouldHaveSize(2)
        BeforeAfterEach.afterAllState.shouldBeEmpty()
    }

    @Test
    fun `disabled test does not start`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<TestWithDisabledMethod>()

        CorounitConfig.reset()
        TestWithDisabledMethod.reset()
        engine.execute(executionRequest)

        TestWithDisabledMethod.test1.get().shouldBe(false)
        TestWithDisabledMethod.test2.get().shouldBe(true)
        CorounitConfig.skipMethodsInvocationCount.get().shouldBeGreaterThan(0)
    }


    @Test
    fun `beforeEach and afterEach invoked in test suite with annotations`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<BeforeAfterEachWithAnnotations>()

        BeforeAfterEachWithAnnotations.reset()
        engine.execute(executionRequest)

        BeforeAfterEachWithAnnotations.beforeAllState.shouldBeEmpty()
        BeforeAfterEachWithAnnotations.beforeEachState.shouldContain(1)
        BeforeAfterEachWithAnnotations.beforeEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.testSequencesState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldContain(6)
        BeforeAfterEachWithAnnotations.afterAllState.shouldBeEmpty()
    }


}