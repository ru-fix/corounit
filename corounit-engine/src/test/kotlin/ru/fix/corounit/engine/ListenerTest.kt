package ru.fix.corounit.engine

import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

class ListenerTest {
    class MyTestForListener {
        companion object {
            /**
             * This test class detected by JunitTestEngine and executed during build
             * as separate test suite.
             * When we use this class as a test source we enable failing behaviour explicitly.
             */
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

    private class EngineExecutionListenerTrap : EngineExecutionListener {
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

    private val engine = EngineEmulator()

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
}