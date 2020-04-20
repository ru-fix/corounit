package ru.fix.corounit.engine

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult

private val log = KotlinLogging.logger { }

class BeforeAndAfterMethodsTest {

    private val engineEmulator = EngineEmulator()

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

    @Test
    fun `beforeAll and afterAll invoked in test suite without annotations`() {
        val executionRequest = engineEmulator.emulateDiscoveryStepForTestClass<MyTestWithoutAnnotations>()

        MyTestWithoutAnnotations.reset()

        engineEmulator.execute(executionRequest)

        MyTestWithoutAnnotations.beforeEachState.shouldContainExactly()
        MyTestWithoutAnnotations.beforeAllState.shouldContainExactly(1)
        MyTestWithoutAnnotations.methodSequencesState.shouldContainExactly(2)
        MyTestWithoutAnnotations.afterEachState.shouldContainExactly()
        MyTestWithoutAnnotations.afterAllState.shouldContainExactly(3)
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

    @Test
    fun `beforeAll and afterAll invoked in test suite with annotations`() {
        val executionRequest = engineEmulator.emulateDiscoveryStepForTestClass<MyTestWithAnnotations>()

        MyTestWithAnnotations.reset()
        engineEmulator.execute(executionRequest)

        MyTestWithAnnotations.beforeEachState.shouldContainExactly()
        MyTestWithAnnotations.beforeAllState.shouldContainExactly(1)
        MyTestWithAnnotations.methodSequencesState.shouldContainExactly(2)
        MyTestWithAnnotations.afterEachState.shouldContainExactly()
        MyTestWithAnnotations.afterAllState.shouldContainExactly(3)
    }

    class BeforeAfterEach {
        companion object : TestClassState() {
        }

        suspend fun beforeEach() {
            beforeEachInvoked()
        }

        suspend fun afterEach() {
            afterEachInvoked()
        }

        @Test
        suspend fun firstMethod() {
            testMethodInvoked(1)
        }

        @Test
        suspend fun secondMethod() {
            testMethodInvoked(2)
        }
    }

    @Test
    fun `beforeEach and afterEach invoked in test suite without annotations`() {
        val executionRequest = engineEmulator.emulateDiscoveryStepForTestClass<BeforeAfterEach>()

        BeforeAfterEach.reset()
        engineEmulator.execute(executionRequest)

        BeforeAfterEach.beforeAllState.shouldBeEmpty()
        BeforeAfterEach.beforeEachState.shouldHaveSize(2)
        BeforeAfterEach.methodSequencesState.shouldHaveSize(2)
        BeforeAfterEach.afterEachState.shouldHaveSize(2)
        BeforeAfterEach.afterAllState.shouldBeEmpty()
    }

    class BeforeAfterEachWithAnnotations {
        companion object : TestClassState() {
        }

        @BeforeEach
        suspend fun before() {
            beforeEachInvoked()
        }

        @AfterEach
        suspend fun after() {
            afterEachInvoked()
        }

        @Test
        suspend fun firstMethod() {
            testMethodInvoked(1)
        }

        @Test
        suspend fun secondMethod() {
            testMethodInvoked(2)
        }
    }

    @Test
    fun `beforeEach and afterEach invoked in test suite with annotations`() {
        val executionRequest = engineEmulator.emulateDiscoveryStepForTestClass<BeforeAfterEachWithAnnotations>()

        BeforeAfterEachWithAnnotations.reset()
        engineEmulator.execute(executionRequest)

        BeforeAfterEachWithAnnotations.beforeAllState.shouldBeEmpty()
        BeforeAfterEachWithAnnotations.beforeEachState.shouldContain(1)
        BeforeAfterEachWithAnnotations.beforeEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.methodSequencesState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldContain(6)
        BeforeAfterEachWithAnnotations.afterAllState.shouldBeEmpty()
    }

    class BeforeEachWithException {
        companion object : TestClassState()

        lateinit var failedToInitialize: String

        suspend fun beforeEach() {
            beforeEachInvoked()
            throw Exception("myException")

            @Suppress("UNREACHABLE_CODE")
            failedToInitialize = "4"
        }

        @Test
        suspend fun testMethod() {
            testMethodInvoked(1)
            println(failedToInitialize + "2")
        }

        suspend fun afterEach() {
            afterEachInvoked()
        }
    }

    @Test
    fun `beforeEach throws Exception should stop test`() {
         val executionRequest = engineEmulator.emulateDiscoveryStepForTestClass<BeforeEachWithException>()
        BeforeEachWithException.reset()
        CorounitConfig.reset()

        engineEmulator.execute(executionRequest)

        engineEmulator.trapListener.finishedTests.asClue{ finished ->
            val methodInvocation = finished.singleOrNull{ it.first is CorounitMethodDescriptior}
            methodInvocation.shouldNotBeNull()

            val (descriptor, result) = methodInvocation
            descriptor.displayName.shouldContain(BeforeEachWithException::testMethod.name)
            result.status.shouldBe(TestExecutionResult.Status.FAILED)
            result.throwable.isPresent.shouldBeTrue()
            result.throwable.get().message.shouldContain("myException")
        }

        with(BeforeEachWithException){
            beforeEachState.shouldContainExactly(1)
            methodIdsState.shouldBeEmpty()
            afterEachState.shouldContainExactly(2)
        }

    }

}