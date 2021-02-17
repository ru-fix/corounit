package ru.fix.corounit.engine

import io.kotlintest.matchers.asClue
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.*
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.platform.engine.TestExecutionResult
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean

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

        engineEmulator.emulateTestClass<MyTestWithoutAnnotations>()

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

        engineEmulator.emulateTestClass<MyTestWithAnnotations>()

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

        engineEmulator.emulateTestClass<BeforeAfterEach>()

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

    @TestInstance(PER_CLASS)
    class BeforeAllAndAfterAllInRightSequence {
        companion object : TestClassState() {
            val methodInvokeQueue = LinkedBlockingDeque<String>()
        }

        @BeforeAll
        suspend fun beforeAll() {
            beforeAllInvoked()
            methodInvokeQueue.push("beforeAll")
            /**
             * delay is for emulation case when beforeAll method is take a long time
             * and test methods must not be invoked before it completes
             */
            delay(5000)
        }

        @AfterAll
        suspend fun afterAll() {
            afterAllInvoked()
            methodInvokeQueue.push("afterAll")

        }

        @Test
        suspend fun firstMethod() {
            testMethodInvoked(1)
            methodInvokeQueue.push("first")
        }

        @Test
        suspend fun secondMethod() {
            /**
             * delay is for emulation case when when test methods are take a long time
             * and afterAll must not be invoked before they complete
             */
            delay(5000)
            testMethodInvoked(2)
            methodInvokeQueue.push("second")
        }
    }

    @Test
    fun `until beforeAll is completed not test methods are invoked, afterAll is invoked when all test method are completed`() = runBlocking {
        BeforeAllAndAfterAllInRightSequence.reset()

        engineEmulator.emulateTestClass<BeforeAllAndAfterAllInRightSequence>()

        BeforeAllAndAfterAllInRightSequence.beforeAllState.shouldHaveSize(1)
        BeforeAllAndAfterAllInRightSequence.afterAllState.shouldHaveSize(1)
        BeforeAllAndAfterAllInRightSequence.methodSequencesState.shouldHaveSize(2)

        BeforeAllAndAfterAllInRightSequence.methodInvokeQueue.poll().shouldBe("afterAll")
        BeforeAllAndAfterAllInRightSequence.methodInvokeQueue.poll().shouldBeOneOf("second", "first")
        BeforeAllAndAfterAllInRightSequence.methodInvokeQueue.poll().shouldBeOneOf("second", "first")
        BeforeAllAndAfterAllInRightSequence.methodInvokeQueue.poll().shouldBe("beforeAll")
    }

}
