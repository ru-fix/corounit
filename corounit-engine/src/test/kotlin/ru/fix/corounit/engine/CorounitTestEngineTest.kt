package ru.fix.corounit.engine

import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.MethodSelector
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MyTestWithoutAnnotations {
    companion object {
        val before = AtomicBoolean()
        val after = AtomicBoolean()
        val test = AtomicBoolean()
    }

    suspend fun beforeAll() {
        before.set(true)
    }

    suspend fun afterAll() {
        after.set(true)
    }

    @Test
    suspend fun myTest() {
        test.set(true)
    }
}

class MyTestWithAnnotations {
    companion object {
        val before = AtomicBoolean()
        val after = AtomicBoolean()
        val test = AtomicBoolean()
    }

    @BeforeAll
    suspend fun setUp() {
        before.set(true)
    }

    @AfterAll
    suspend fun tearDown() {
        after.set(true)
    }

    @Test
    suspend fun myTest() {
        test.set(true)
    }
}

class CorounitTestEngineTest {

    @Test
    fun `beforeAll and afterAll method invoked in test suite without annotations`() {

        val engine = CorounitTestEngine()
        val discoveryRequest = mockk<EngineDiscoveryRequest>()
        every { discoveryRequest.getSelectorsByType(MethodSelector::class.java) } returns mutableListOf()


        val selector = mockk<ClassSelector>()
        every { discoveryRequest.getSelectorsByType(ClassSelector::class.java) } returns mutableListOf(selector)

        every { selector.javaClass } returns MyTestWithoutAnnotations::class.java
        every { selector.className } returns MyTestWithoutAnnotations::class.java.name

        val descriptor = engine.discover(discoveryRequest, UniqueId.forEngine("corounit"))

        MyTestWithoutAnnotations.before.set(false)
        MyTestWithoutAnnotations.test.set(false)
        MyTestWithoutAnnotations.after.set(false)

        val executionRequest = mockk<ExecutionRequest>(relaxed = true)
        val config = mockk<ConfigurationParameters>(relaxed = true)
        every { executionRequest.configurationParameters } returns config
        every { executionRequest.rootTestDescriptor } returns descriptor
        every { executionRequest.engineExecutionListener } returns mockk(relaxed = true)
        every { config.get(any()) } returns Optional.empty()

        engine.execute(executionRequest)

        MyTestWithoutAnnotations.before.get().shouldBe(true)
        MyTestWithoutAnnotations.test.get().shouldBe(true)
        MyTestWithoutAnnotations.after.get().shouldBe(true)
    }

    @Test
    fun `beforeAll and afterAll method invoked in test suite with annotations`() {

        val engine = CorounitTestEngine()
        val discoveryRequest = mockk<EngineDiscoveryRequest>()
        every { discoveryRequest.getSelectorsByType(MethodSelector::class.java) } returns mutableListOf()

        val selector = mockk<ClassSelector>()
        every { discoveryRequest.getSelectorsByType(ClassSelector::class.java) } returns mutableListOf(selector)

        every { selector.javaClass } returns MyTestWithAnnotations::class.java
        every { selector.className } returns MyTestWithAnnotations::class.java.name

        val descriptor = engine.discover(discoveryRequest, UniqueId.forEngine("corounit"))

        MyTestWithAnnotations.before.set(false)
        MyTestWithAnnotations.test.set(false)
        MyTestWithAnnotations.after.set(false)

        val executionRequest = mockk<ExecutionRequest>(relaxed = true)
        val config = mockk<ConfigurationParameters>(relaxed = true)
        every { executionRequest.configurationParameters } returns config
        every { executionRequest.rootTestDescriptor } returns descriptor
        every { executionRequest.engineExecutionListener } returns mockk(relaxed = true)
        every { config.get(any()) } returns Optional.empty()

        engine.execute(executionRequest)

        MyTestWithAnnotations.before.get().shouldBe(true)
        MyTestWithAnnotations.test.get().shouldBe(true)
        MyTestWithAnnotations.after.get().shouldBe(true)
    }
}