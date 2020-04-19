package ru.fix.corounit.engine

import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import org.junit.jupiter.api.Test

class PluginInvocationTest {

    class MyTestClassForPlugin {
        @Test
        suspend fun test() {
        }
    }

    private val engine = EngineEmulator()

    @Test
    fun `plugin object located in same package is invoked`() {
        CorounitConfig.reset()

        val executionRequest = engine.emulateDiscoveryStepForTestClass<MyTestClassForPlugin>()
        engine.execute(executionRequest)

        CorounitConfig.beforeAllTestClassesInvocationCount.get().shouldBeGreaterThan(0)
        CorounitConfig.providerBeforeAllTestClassesInvocationCount.get().shouldBeGreaterThan(0)
    }

}