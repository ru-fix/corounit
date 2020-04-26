package ru.fix.corounit.engine

import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestExecutionResult
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

        @Disabled("skipped for test")
        @Test
        suspend fun mySkippedTest(){

        }

        @Test
        suspend fun myFailedTest() {
            if (shouldFailedTestFail.get()) {
                throw Exception("oops")
            }
        }
    }

    private val engine = EngineEmulator()

    @Test
    fun `success and failed tests reported to junit listener`() {
        MyTestForListener.shouldFailedTestFail.set(true)

        val executionRequest = engine.emulateTestClass<MyTestForListener>()

        engine.trapListener.finishedTests
                .single {
                    it.first.displayName.contains(MyTestForListener::mySuccessTest.name)
                }
                .second.status.shouldBe(TestExecutionResult.Status.SUCCESSFUL)

        engine.trapListener.finishedTests
                .single {
                    it.first.displayName.contains(MyTestForListener::myFailedTest.name)
                }
                .second.apply {
                    status.shouldBe(TestExecutionResult.Status.FAILED)
                    throwable.shouldNotBeNull()
                }

        engine.trapListener.skippedTests
                .single {
                    it.first.displayName.contains(MyTestForListener::mySkippedTest.name)
                }.second.shouldBe("skipped for test")
    }
}