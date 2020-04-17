package ru.fix.corounit.engine

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.MethodSelector
import java.util.*

class EngineEmulator {
    val engine: CorounitTestEngine = CorounitTestEngine()

    inline fun <reified T> mockDiscoveryRequest(): EngineDiscoveryRequest {
        val discoveryRequest = mockk<EngineDiscoveryRequest>()
        every { discoveryRequest.getSelectorsByType(MethodSelector::class.java) } returns mutableListOf()

        val selector = mockk<ClassSelector>()

        val selectorClass = slot<Class<DiscoverySelector>>()
        every { discoveryRequest.getSelectorsByType<DiscoverySelector>( capture(selectorClass) ) } answers {
            if(selectorClass.captured ==  ClassSelector::class.java){
                mutableListOf<DiscoverySelector>(selector)
            } else {
                emptyList()
            }
        }
        every { discoveryRequest.getFiltersByType<DiscoveryFilter<*>>(any()) } returns emptyList()

        every { selector.javaClass } returns T::class.java
        every { selector.className } returns T::class.java.name

        return discoveryRequest
    }

    fun mockExecutionRequest(descriptor: TestDescriptor, listener: EngineExecutionListener? = null): ExecutionRequest {
        val executionRequest = mockk<ExecutionRequest>(relaxed = true)
        val config = mockk<ConfigurationParameters>(relaxed = true)
        every { executionRequest.configurationParameters } returns config
        every { executionRequest.rootTestDescriptor } returns descriptor
        every { executionRequest.engineExecutionListener } returns (listener ?: mockk(relaxed = true))
        every { config.get(any()) } returns Optional.empty()
        return executionRequest
    }

    inline fun <reified T> emulateDiscoveryStepForTestClass(listener: EngineExecutionListener? = null): ExecutionRequest {
        val discoveryRequest = mockDiscoveryRequest<T>()
        val descriptor = engine.discover(discoveryRequest, UniqueId.forEngine("corounit"))
        return mockExecutionRequest(descriptor, listener)
    }

    fun execute(request: ExecutionRequest) = engine.execute(request)

}