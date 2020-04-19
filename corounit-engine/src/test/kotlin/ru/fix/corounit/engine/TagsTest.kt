package ru.fix.corounit.engine

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.UniqueId
import java.util.concurrent.atomic.AtomicBoolean

@Tag("myClassTag")
class StringTagClass{

    @Tag("myMethodTag")
    @Test
    suspend fun testWithTag(){
    }

    @Test
    suspend fun testWithoutTag() {
    }
}

/**
 * JUnit launcher filters tests by Tags between [org.junit.platform.engine.TestEngine.discover]
 * and [org.junit.platform.engine.TestEngine.execute].
 * Goal of [org.junit.platform.engine.TestEngine]
 * is to correctly populate [org.junit.platform.engine.TestDescriptor] with tags.
 */
class TagsTest {
    val engineEmulator = EngineEmulator()

    @Test
    fun `class and method tags are resolved`() {
        val request = engineEmulator.emulateDiscoveryStepForTestClass<StringTagClass>()

        val classDescriptor = request.rootTestDescriptor.children
                .single { it is CorounitClassDescriptior }
        classDescriptor.tags.shouldContainExactly(TestTag.create("myClassTag"))

        val methodDescriptor = classDescriptor.children
                .single { it is CorounitMethodDescriptior && it.method.name == StringTagClass::testWithTag.name }
        methodDescriptor.tags.shouldContainExactly(TestTag.create("myMethodTag"))
    }

}