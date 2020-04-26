package ru.fix.corounit.engine

import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger { }

class ConcurrentExecutionTest{
    /**
     * This test class detected by JunitTestEngine and executed during build
     * as separate test suite.
     * When we use this class as a test source we enable trapping behaviour explicitly.
     */
    class MyTestFirstMethodsWaitsOthers {
        companion object : TestClassState() {
            val shouldFirstMethodWaitOthers = AtomicBoolean()
        }

        private fun concurrentTestInvoked() {
            val amIaFirstInvokedTest = testMethodInvoked(1) == 1
            if (amIaFirstInvokedTest) {
                while (shouldFirstMethodWaitOthers.get() && !methodSequencesState.containsAll(listOf(2, 3))) {
                    Thread.sleep(100)
                    log.info {
                        "Waiting for test state to contains [2, 3]." +
                                " Current state: $methodSequencesState"
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


    private val engine = EngineEmulator()


    @Test
    fun `first test method blocks thread and waits others to complete, whole suite passes without timeout`() {
        MyTestFirstMethodsWaitsOthers.reset()
        MyTestFirstMethodsWaitsOthers.shouldFirstMethodWaitOthers.set(true)

        engine.emulateTestClass<MyTestFirstMethodsWaitsOthers>()

        MyTestFirstMethodsWaitsOthers.shouldFirstMethodWaitOthers.set(false)

        MyTestFirstMethodsWaitsOthers.beforeEachState.shouldContainExactly()
        MyTestFirstMethodsWaitsOthers.methodSequencesState.shouldContainExactlyInAnyOrder(1, 2, 3)
        MyTestFirstMethodsWaitsOthers.afterEachState.shouldContainExactly()
    }

}