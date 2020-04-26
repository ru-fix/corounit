package ru.fix.corounit.engine

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import java.util.*

class EngineEmulator {
    val engine: CorounitTestEngine = CorounitTestEngine()
    val trapListener = EngineExecutionTrapListener()


    inline fun <reified T> mockDiscoveryRequestByClassSelector(): EngineDiscoveryRequest {
        val selector = mockk<ClassSelector>()
        every { selector.javaClass } returns T::class.java
        every { selector.className } returns T::class.java.name

        return mockDiscoveryRequest(selector)
    }

    fun mockDiscoveryRequest(selector: DiscoverySelector): EngineDiscoveryRequest {
        val discoveryRequest = mockk<EngineDiscoveryRequest>()

        val selectorClass = slot<Class<DiscoverySelector>>()
        every { discoveryRequest.getSelectorsByType(capture(selectorClass)) } answers {
            if (selectorClass.captured == selector::class.java) {
                mutableListOf(selector)
            } else {
                emptyList()
            }
        }

        every { discoveryRequest.getFiltersByType<DiscoveryFilter<*>>(any()) } returns emptyList()

        return discoveryRequest
    }


    fun mockExecutionRequest(descriptor: TestDescriptor): ExecutionRequest {
        val executionRequest = mockk<ExecutionRequest>(relaxed = true)
        val config = mockk<ConfigurationParameters>(relaxed = true)
        every { executionRequest.configurationParameters } returns config
        every { executionRequest.rootTestDescriptor } returns descriptor
        every { executionRequest.engineExecutionListener } returns trapListener
        every { config.get(any()) } returns Optional.empty()
        return executionRequest
    }

    inline fun <reified T> emulateTestClass() {
        val discoveryRequest = mockDiscoveryRequestByClassSelector<T>()
        val descriptor = discover(discoveryRequest)
        val executionrequest = mockExecutionRequest(descriptor)
        execute(executionrequest)
    }

    fun emulateTestsBySelector(selector: DiscoverySelector) {
        val discoveryRequest = mockDiscoveryRequest(selector)
        val descriptor = discover(discoveryRequest)
        val executionRequest = mockExecutionRequest(descriptor)
        execute(executionRequest)
    }

    inline fun <reified T> emulateDiscoveryForTestClass(): ExecutionRequest {
        val discoveryRequest = mockDiscoveryRequestByClassSelector<T>()
        val descriptor = discover(discoveryRequest)
        return mockExecutionRequest(descriptor)
    }

    fun emulateDiscovery(selector: DiscoverySelector): ExecutionRequest {
        val discoveryRequest = mockDiscoveryRequest(selector)
        val descriptor = discover(discoveryRequest)
        return mockExecutionRequest(descriptor)
    }

    fun discover(request: EngineDiscoveryRequest) = engine.discover(request, UniqueId.forEngine("corounit"))
    fun execute(request: ExecutionRequest) = engine.execute(request)
}