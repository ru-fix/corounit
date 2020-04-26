package ru.fix.corounit.engine

import io.kotlintest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.ClasspathRootSelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import ru.fix.corounit.engine.test.discovery.ComplexDiscoveryPlainTest
import ru.fix.corounit.engine.test.discovery.ComplexDiscoveryTest
import ru.fix.corounit.engine.test.discovery.plain.PlainDiscoveryTest
import kotlin.reflect.jvm.javaMethod


class DiscoveryTestClassesTest {

    val engineEmulator = EngineEmulator()

    @Test
    fun `discovery test classes among non test and dollar-signled generated classes`() {
        val selector = DiscoverySelectors.selectPackage(ComplexDiscoveryTest::class.java.packageName)

        ComplexDiscoveryTest.reset()
        ComplexDiscoveryPlainTest.reset()

        engineEmulator.emulateTestsBySelector(selector)

        ComplexDiscoveryTest.methodIdsState.shouldContainExactly(1)
        ComplexDiscoveryPlainTest.methodIdsState.shouldContainExactly(1)
    }

    @Test
    fun `method selector`() {
        val selector = DiscoverySelectors.selectMethod(
                PlainDiscoveryTest::class.java,
                PlainDiscoveryTest::testMethod1.javaMethod)

        PlainDiscoveryTest.reset()
        engineEmulator.emulateTestsBySelector(selector)
        PlainDiscoveryTest.methodIdsState.shouldContainExactly(1)
    }

    @Test
    fun `class selector`() {
        val selector = DiscoverySelectors.selectClass(PlainDiscoveryTest::class.java)

        PlainDiscoveryTest.reset()
        engineEmulator.emulateTestsBySelector(selector)
        PlainDiscoveryTest.methodIdsState.shouldContainExactly(1)
    }

    @Test
    fun `package selector`() {
        val selector = DiscoverySelectors.selectPackage(PlainDiscoveryTest::class.java.packageName)

        PlainDiscoveryTest.reset()
        engineEmulator.emulateTestsBySelector(selector)
        PlainDiscoveryTest.methodIdsState.shouldContainExactly(1)
    }

    @Test
    fun `class path selector`() {
        val selector = mockk<ClasspathRootSelector>()
        every { selector.classpathRoot } returns PlainDiscoveryTest::class.java.getResource("/").toURI()

        PlainDiscoveryTest.reset()
        engineEmulator.emulateTestsBySelector(selector)
        PlainDiscoveryTest.methodIdsState.shouldContainExactly(1)
    }

    @Test
    fun `module selector by default returns empty class list`() {
        val selector = DiscoverySelectors.selectModule("moduleName")

        PlainDiscoveryTest.reset()
        engineEmulator.emulateTestsBySelector(selector)
        PlainDiscoveryTest.methodIdsState.shouldContainExactly()
    }
}