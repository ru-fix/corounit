package ru.fix.corounit.engine

import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class DisabledMethodTest{

    class TestWithDisabledMethod{
        companion object: TestClassState(){
        }

        @Disabled
        @Test
        suspend fun test1(){
            testMethodInvoked(1)
        }
        @Test
        suspend fun test2(){
            testMethodInvoked(2)
        }
    }

    private val engine = EngineEmulator()

    @Test
    fun `disabled test does not start`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<TestWithDisabledMethod>()

        CorounitConfig.reset()
        TestWithDisabledMethod.reset()

        engine.execute(executionRequest)

        TestWithDisabledMethod.testIdsState.shouldContainExactly(2)
        CorounitConfig.skipMethodsInvocationCount.get().shouldBeGreaterThan(0)
        CorounitConfig.skipTestMethodLog
                .find { it.testMethod.name == TestWithDisabledMethod::test1.name }
                .shouldNotBeNull()
    }
}