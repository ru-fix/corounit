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
import org.junit.platform.engine.TestExecutionResult
import java.util.concurrent.atomic.AtomicBoolean

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
        MyTestWithoutAnnotations.reset()

        val executionRequest = engineEmulator.emulateTestClass<MyTestWithoutAnnotations>()

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
        MyTestWithAnnotations.reset()

        val executionRequest = engineEmulator.emulateTestClass<MyTestWithAnnotations>()

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
        BeforeAfterEach.reset()

        val executionRequest = engineEmulator.emulateTestClass<BeforeAfterEach>()

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
        BeforeAfterEachWithAnnotations.reset()

        engineEmulator.emulateTestClass<BeforeAfterEachWithAnnotations>()

        BeforeAfterEachWithAnnotations.beforeAllState.shouldBeEmpty()
        BeforeAfterEachWithAnnotations.beforeEachState.shouldContain(1)
        BeforeAfterEachWithAnnotations.beforeEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.methodSequencesState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldContain(6)
        BeforeAfterEachWithAnnotations.afterAllState.shouldBeEmpty()
    }

    class BeforeEachWithException {
        companion object : TestClassState(){
            val enabledFailing = AtomicBoolean(false)
        }

        lateinit var failedToInitialize: String

        suspend fun beforeEach() {
            beforeEachInvoked()
            if(enabledFailing.get()) {
                throw Throwable("myThrowable")
            }

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
        BeforeEachWithException.reset()
        BeforeEachWithException.enabledFailing.set(true)
        CorounitConfig.reset()

        engineEmulator.emulateTestClass<BeforeEachWithException>()

        engineEmulator.trapListener.finishedTests.asClue{ finished ->
            val methodInvocation = finished.singleOrNull{ it.first is CorounitMethodDescriptior}
            methodInvocation.shouldNotBeNull()

            val (descriptor, result) = methodInvocation
            descriptor.displayName.shouldContain(BeforeEachWithException::testMethod.name)
            result.status.shouldBe(TestExecutionResult.Status.FAILED)
            result.throwable.isPresent.shouldBeTrue()
            result.throwable.get().message.shouldContain("myThrowable")
        }

        with(BeforeEachWithException){
            beforeEachState.shouldContainExactly(1)
            methodIdsState.shouldBeEmpty()
            afterEachState.shouldContainExactly(2)
        }

    }

}
