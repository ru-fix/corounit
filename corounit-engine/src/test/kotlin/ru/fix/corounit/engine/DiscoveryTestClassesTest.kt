package ru.fix.corounit.engine

import io.kotlintest.matchers.collections.shouldBeSingleton
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.PackageSelector
import ru.fix.corounit.engine.test.discovery.PlainTest


class DiscoveryTestClassesTest {
    @Test
    fun `discovery test classes among non test and dollar-signled generated classes`() {
        val engine = CorounitTestEngine()

        val discoveryRequest = mockk<EngineDiscoveryRequest>()

        val selector = mockk<PackageSelector>()

        val selectorClass = slot<Class<DiscoverySelector>>()
        every { discoveryRequest.getSelectorsByType<DiscoverySelector>( capture(selectorClass) ) } answers {
            if(selectorClass.captured ==  PackageSelector::class.java){
                mutableListOf(selector as DiscoverySelector)
            } else {
                emptyList()
            }
        }
        every { discoveryRequest.getFiltersByType<DiscoveryFilter<*>>(any()) } returns emptyList()

        every { selector.packageName } returns "ru.fix.corounit.engine.test"


        val descriptor = engine.discover(discoveryRequest, UniqueId.forEngine("corounit"))
        descriptor.children.shouldBeSingleton()
        descriptor.children.single().shouldBeInstanceOf<CorounitClassDescriptior>{
            it.clazz.qualifiedName.shouldBe(PlainTest::class.qualifiedName)
            it.methodDescriptors.shouldBeSingleton()
            it.methodDescriptors.single().method.name.shouldBe(PlainTest::plainTestMethod.name)
        }
    }
}