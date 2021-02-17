package ru.fix.corounit.engine

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicBoolean
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

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestClassWithErrorInBeforeAllMethodLifecyclePerClass {

        companion object {
            val enabledFailing = AtomicBoolean(false)
        }

        suspend fun beforeAll() {
            if (enabledFailing.get()) {
                error("error occurred in beforeAll")
            }
        }

        suspend fun afterAll() { }

        @Test
        suspend fun test() { }
    }

    @Test
    fun `plugin afterBeforeAllMethod is invoked if before method has error (per class lifecycle)`() {
        PluginForTestInvocation.reset()
        TestClassWithErrorInBeforeAllMethodLifecyclePerClass.enabledFailing.set(true)

        engineEmulator.emulateTestClass<TestClassWithErrorInBeforeAllMethodLifecyclePerClass>()

        assertPluginInvocation(
                beforeBeforeAllMethodInvocationCount = 1,
                afterBeforeAllMethodInvocationCount = 1,
                beforeAfterAllMethodInvocationCount = 0,
                afterAfterAllMethodInvocationCount = 0,
                beforeTestMethodInvocationCount = 0,
                afterTestMethodInvocationCount = 0
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    class TestClassWithErrorInBeforeAllMethodLifecyclePerMethod {

        companion object {
            val enabledFailing = AtomicBoolean(false)

            suspend fun beforeAll() {
                if (enabledFailing.get()) {
                    error("error occurred in beforeAll")
                }
            }

            suspend fun afterAll() { }
        }

        @Test
        suspend fun test() { }
    }

    @Test
    fun `plugin afterBeforeAllMethod is invoked if before method has error (per method lifecycle)`() {
        PluginForTestInvocation.reset()
        TestClassWithErrorInBeforeAllMethodLifecyclePerMethod.enabledFailing.set(true)

        engineEmulator.emulateTestClass<TestClassWithErrorInBeforeAllMethodLifecyclePerMethod>()

        assertPluginInvocation(
                beforeBeforeAllMethodInvocationCount = 1,
                afterBeforeAllMethodInvocationCount = 1,
                beforeAfterAllMethodInvocationCount = 0,
                afterAfterAllMethodInvocationCount = 0,
                beforeTestMethodInvocationCount = 0,
                afterTestMethodInvocationCount = 0
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class TestClassWithErrorInAfterAllMethodLifecyclePerClass {

        companion object {
            val enabledFailing = AtomicBoolean(false)
        }

        suspend fun beforeAll() { }

        suspend fun afterAll() {
            if (enabledFailing.get()) {
                error("error occurred in afterAll")
            }
        }

        @Test
        suspend fun test() { }
    }

    @Test
    fun `plugin afterAfterAllMethod is invoked if afterAll method has error (per class lifecycle)`() {
        PluginForTestInvocation.reset()
        TestClassWithErrorInAfterAllMethodLifecyclePerClass.enabledFailing.set(true)

        engineEmulator.emulateTestClass<TestClassWithErrorInAfterAllMethodLifecyclePerClass>()

        assertPluginInvocation(
                beforeBeforeAllMethodInvocationCount = 1,
                afterBeforeAllMethodInvocationCount = 1,
                beforeAfterAllMethodInvocationCount = 1,
                afterAfterAllMethodInvocationCount = 1,
                beforeTestMethodInvocationCount = 1,
                afterTestMethodInvocationCount = 1
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
    class TestClassWithErrorInAfterAllMethodLifecyclePerMethod {

        companion object {
            val enabledFailing = AtomicBoolean(false)

            suspend fun beforeAll() { }

            suspend fun afterAll() {
                if (enabledFailing.get()) {
                    error("error occurred in afterAll")
                }
            }
        }

        @Test
        suspend fun test() { }
    }

    @Test
    fun `plugin afterAfterAllMethod is invoked if afterAll method has error (per method lifecycle)`() {
        PluginForTestInvocation.reset()
        TestClassWithErrorInAfterAllMethodLifecyclePerMethod.enabledFailing.set(true)

        engineEmulator.emulateTestClass<TestClassWithErrorInAfterAllMethodLifecyclePerMethod>()

        assertPluginInvocation(
                beforeBeforeAllMethodInvocationCount = 1,
                afterBeforeAllMethodInvocationCount = 1,
                beforeAfterAllMethodInvocationCount = 1,
                afterAfterAllMethodInvocationCount = 1,
                beforeTestMethodInvocationCount = 1,
                afterTestMethodInvocationCount = 1
        )
    }

    private fun assertPluginInvocation(
            beforeBeforeAllMethodInvocationCount: Int = 1,
            afterBeforeAllMethodInvocationCount: Int = 1,
            beforeAfterAllMethodInvocationCount: Int = 1,
            afterAfterAllMethodInvocationCount: Int = 1,
            beforeTestMethodInvocationCount: Int = 1,
            afterTestMethodInvocationCount: Int = 1
    ) {
        PluginForTestInvocation.beforeBeforeAllMethodInvocationCount.get().shouldBe(beforeBeforeAllMethodInvocationCount)
        PluginForTestInvocation.afterBeforeAllMethodInvocationCount.get().shouldBe(afterBeforeAllMethodInvocationCount)

        PluginForTestInvocation.beforeAfterAllMethodInvocationCount.get().shouldBe(beforeAfterAllMethodInvocationCount)
        PluginForTestInvocation.afterAfterAllMethodInvocationCount.get().shouldBe(afterAfterAllMethodInvocationCount)

        PluginForTestInvocation.beforeTestClassInvocationCount.get().shouldBe(1)
        PluginForTestInvocation.afterTestClassInvocationCount.get().shouldBe(1)

        PluginForTestInvocation.beforeAllTestClassesInvocationCount.get().shouldBe(1)
        PluginForTestInvocation.afterAllTestClassesInvocationCount.get().shouldBe(1)

        PluginForTestInvocation.beforeTestMethodInvocationCount.get().shouldBe(beforeTestMethodInvocationCount)
        PluginForTestInvocation.afterTestMethodInvocationCount.get().shouldBe(afterTestMethodInvocationCount)
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