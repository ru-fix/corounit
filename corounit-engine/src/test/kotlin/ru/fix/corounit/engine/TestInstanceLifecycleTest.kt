package ru.fix.corounit.engine

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.ConcurrentLinkedDeque

class TestInstanceLifecycleTest {
    private val engine = EngineEmulator()

    class TestClassInstancePerMethodInvocation {
        companion object : TestClassState() {
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

        suspend fun beforeEach() {
            beforeEachInvoked()
        }

        fun beforeAll() {
            beforeAllInvoked()
        }

        suspend fun afterAll() {
            afterAllInvoked()
        }
    }


    @Test
    fun `new test instance created for each method invocation by default`() {
        TestClassInstancePerMethodInvocation.reset()

        val executionRequest = engine.emulateTestClass<TestClassInstancePerMethodInvocation>()

        TestClassInstancePerMethodInvocation.instances.shouldHaveSize(2)
        (TestClassInstancePerMethodInvocation.instances.first !== TestClassInstancePerMethodInvocation.instances.last)
                .shouldBeTrue()

        TestClassInstancePerMethodInvocation.beforeAllState.shouldContainExactly()
        TestClassInstancePerMethodInvocation.beforeEachState.shouldHaveSize(2)
        TestClassInstancePerMethodInvocation.methodSequencesState.shouldHaveSize(2)
        TestClassInstancePerMethodInvocation.afterEachState.shouldBeEmpty()
        TestClassInstancePerMethodInvocation.afterAllState.shouldBeEmpty()
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @Test
    fun `single instance create for all method invocation if annotation present`() {
        TestClassInstancePerClassInvocationWithAnnotation.reset()

        val executionRequest = engine.emulateTestClass<TestClassInstancePerClassInvocationWithAnnotation>()

        TestClassInstancePerClassInvocationWithAnnotation.instances.shouldHaveSize(2)
        (TestClassInstancePerClassInvocationWithAnnotation.instances.first ===
                TestClassInstancePerClassInvocationWithAnnotation.instances.last)
                .shouldBeTrue()

        TestClassInstancePerClassInvocationWithAnnotation.beforeAllState.shouldContainExactly(1)
        TestClassInstancePerClassInvocationWithAnnotation.beforeEachState.shouldContainExactly()
        TestClassInstancePerClassInvocationWithAnnotation.methodSequencesState.shouldContainExactly(2, 3)
        TestClassInstancePerClassInvocationWithAnnotation.afterEachState.shouldContainExactly()
        TestClassInstancePerClassInvocationWithAnnotation.afterAllState.shouldContainExactly(4)

    }
}