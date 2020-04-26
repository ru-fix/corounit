package ru.fix.corounit.engine

import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import org.junit.jupiter.api.Test

class PluginInvocationTest {

    class MyTestClassForPlugin {
        @Test
        suspend fun test() {
        }
    }

    private val engineEmulator = EngineEmulator()

    @Test
    fun `plugin object located in same package is invoked`() {
        CorounitConfig.reset()

        engineEmulator.emulateTestClass<MyTestClassForPlugin>()

        CorounitConfig.beforeAllTestClassesInvocationCount.get().shouldBeGreaterThan(0)
        CorounitConfig.providerBeforeAllTestClassesInvocationCount.get().shouldBeGreaterThan(0)
    }

}