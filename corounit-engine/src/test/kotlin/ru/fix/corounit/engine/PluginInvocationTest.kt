package ru.fix.corounit.engine

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class PluginInvocationTest {

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestClassForPluginWhereLifecyclePerClass {

        suspend fun beforeAll() { }

        suspend fun afterAll() { }

        @Test
        suspend fun test() { }
    }

    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    class TestClassForPluginWhereLifecyclePerMethod {

        companion object {
            suspend fun beforeAll() {}

            suspend fun afterAll() { }
        }

        @Test
        suspend fun test() { }
    }

    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    class TestClassWithoutBeforeAndAfterMethodsLifecyclePerMethod {
        @Test
        suspend fun test() { }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestClassWithoutBeforeAndAfterMethodsLifecyclePerClass {
        @Test
        suspend fun test() { }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestClassWithErrorInBeforeAndAfterAllMethodsLifecyclePerClass {

        suspend fun beforeAll() {
            error("error occurred in beforeAll")
        }

        suspend fun afterAll() {
            error("error occurred in afterAll")
        }

        @Test
        suspend fun test() { }
    }

    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    class TestClassWithErrorInBeforeAndAfterAllMethodsLifecyclePerMethod {

        companion object {
            suspend fun beforeAll() {
                error("error occurred in beforeAll")
            }

            suspend fun afterAll() {
                error("error occurred in afterAll")
            }
        }

        @Test
        suspend fun test() { }
    }

    private val engineEmulator = EngineEmulator()

    @Test
    fun `plugin is invoked (per class lifecycle)`() {
        PluginForTestInvocation.reset()

        engineEmulator.emulateTestClass<TestClassForPluginWhereLifecyclePerClass>()

        assertPluginInvocation()
    }

    @Test
    fun `plugin is invoked (pre method lifecycle)`() {
        PluginForTestInvocation.reset()

        engineEmulator.emulateTestClass<TestClassForPluginWhereLifecyclePerMethod>()

        assertPluginInvocation()
    }

    @Test
    fun `plugin is not invoked for beforeAll and afterAll methods if there are not present (per class lifecycle)`() {
        PluginForTestInvocation.reset()

        engineEmulator.emulateTestClass<TestClassWithoutBeforeAndAfterMethodsLifecyclePerClass>()

        assertPluginInvocation(
                beforeBeforeAllMethodInvocationCount = 0,
                afterBeforeAllMethodInvocationCount = 0,
                beforeAfterAllMethodInvocationCount = 0,
                afterAfterAllMethodInvocationCount = 0
        )
    }

    @Test
    fun `plugin is not invoked for beforeAll and afterAll methods if there are not present (per method lifecycle)`() {
        PluginForTestInvocation.reset()

        engineEmulator.emulateTestClass<TestClassWithoutBeforeAndAfterMethodsLifecyclePerMethod>()

        assertPluginInvocation(
                beforeBeforeAllMethodInvocationCount = 0,
                afterBeforeAllMethodInvocationCount = 0,
                beforeAfterAllMethodInvocationCount = 0,
                afterAfterAllMethodInvocationCount = 0
        )
    }

    @Test
    fun `plugin afterBeforeAllMethod is invoked if before or after all methods have error (per class lifecycle)`() {
        PluginForTestInvocation.reset()

        engineEmulator.emulateTestClass<TestClassWithErrorInBeforeAndAfterAllMethodsLifecyclePerClass>()

        assertPluginInvocation()
    }

    @Test
    fun `plugin afterAfterAllMethod is invoked if before or after all methods have error (per method lifecycle)`() {
        PluginForTestInvocation.reset()

        engineEmulator.emulateTestClass<TestClassWithErrorInBeforeAndAfterAllMethodsLifecyclePerMethod>()

        assertPluginInvocation()
    }

    private fun assertPluginInvocation(
            beforeBeforeAllMethodInvocationCount: Int = 1,
            afterBeforeAllMethodInvocationCount: Int = 1,
            beforeAfterAllMethodInvocationCount: Int = 1,
            afterAfterAllMethodInvocationCount: Int = 1
    ) {
        PluginForTestInvocation.beforeBeforeAllMethodInvocationCount.get().shouldBe(beforeBeforeAllMethodInvocationCount)
        PluginForTestInvocation.afterBeforeAllMethodInvocationCount.get().shouldBe(afterBeforeAllMethodInvocationCount)

        PluginForTestInvocation.beforeAfterAllMethodInvocationCount.get().shouldBe(beforeAfterAllMethodInvocationCount)
        PluginForTestInvocation.afterAfterAllMethodInvocationCount.get().shouldBe(afterAfterAllMethodInvocationCount)

        PluginForTestInvocation.beforeTestClassInvocationCount.get().shouldBe(1)
        PluginForTestInvocation.afterTestClassInvocationCount.get().shouldBe(1)

        PluginForTestInvocation.beforeAllTestClassesInvocationCount.get().shouldBe(1)
        PluginForTestInvocation.afterAllTestClassesInvocationCount.get().shouldBe(1)

        PluginForTestInvocation.beforeTestMethodInvocationCount.get().shouldBe(1)
        PluginForTestInvocation.afterTestMethodInvocationCount.get().shouldBe(1)
    }

}

/**
 * This class is used to for test [CorounitPlugin] invocation lifecycle.
 *
 * Please do not use for other cases to avoid errors with shared state.
 * Just create your own and add it to CorounitCofig
 */
object PluginForTestInvocation: CorounitPlugin {
    val beforeBeforeAllMethodInvocationCount = AtomicInteger()
    val afterBeforeAllMethodInvocationCount = AtomicInteger()

    val beforeAfterAllMethodInvocationCount = AtomicInteger()
    val afterAfterAllMethodInvocationCount = AtomicInteger()

    val beforeTestClassInvocationCount = AtomicInteger()
    val afterTestClassInvocationCount = AtomicInteger()

    val beforeAllTestClassesInvocationCount = AtomicInteger()
    val afterAllTestClassesInvocationCount = AtomicInteger()

    val beforeTestMethodInvocationCount = AtomicInteger()
    val afterTestMethodInvocationCount = AtomicInteger()

    fun reset() {
        beforeBeforeAllMethodInvocationCount.set(0)
        afterBeforeAllMethodInvocationCount.set(0)

        beforeAfterAllMethodInvocationCount.set(0)
        afterAfterAllMethodInvocationCount.set(0)

        beforeTestClassInvocationCount.set(0)
        afterTestClassInvocationCount.set(0)

        beforeAllTestClassesInvocationCount.set(0)
        afterAllTestClassesInvocationCount.set(0)

        beforeTestMethodInvocationCount.set(0)
        afterTestMethodInvocationCount.set(0)
    }

    override suspend fun beforeBeforeAllMethod(testMethodContext: CoroutineContext): CoroutineContext {
        beforeBeforeAllMethodInvocationCount.incrementAndGet()
        return testMethodContext
    }

    override suspend fun afterBeforeAllMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
        afterBeforeAllMethodInvocationCount.incrementAndGet()
    }

    override suspend fun beforeAfterAllMethod(testMethodContext: CoroutineContext): CoroutineContext {
        beforeAfterAllMethodInvocationCount.incrementAndGet()
        return testMethodContext
    }

    override suspend fun afterAfterAllMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
        afterAfterAllMethodInvocationCount.incrementAndGet()
    }

    override suspend fun beforeTestClass(testClassContext: CoroutineContext): CoroutineContext {
        beforeTestClassInvocationCount.incrementAndGet()
        return testClassContext
    }

    override suspend fun afterTestClass(testClassContext: CoroutineContext) {
        afterTestClassInvocationCount.incrementAndGet()
    }

    override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
        beforeAllTestClassesInvocationCount.incrementAndGet()
        return globalContext
    }

    override suspend fun afterAllTestClasses(globalContext: CoroutineContext) {
        afterAllTestClassesInvocationCount.incrementAndGet()
    }

    override suspend fun beforeTestMethod(testMethodContext: CoroutineContext): CoroutineContext {
        beforeTestMethodInvocationCount.incrementAndGet()
        return testMethodContext
    }

    override suspend fun afterTestMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
        afterTestMethodInvocationCount.incrementAndGet()
    }
}