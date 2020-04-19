package ru.fix.corounit.engine

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedDeque

class TestInstanceLifecycleTest {
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

    private val engine = EngineEmulator()

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
}