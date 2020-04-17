package ru.fix.corounit.engine

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class StringTagClass{
    companion object{
        val isTestWithTagInvoked = AtomicBoolean()
        val isTestWithoutTagInvoked = AtomicBoolean()
    }

    @Tag("myTag")
    @Test
    suspend fun testWithTag(){
        isTestWithTagInvoked.set(true)
    }

    @Test
    suspend fun testWithoutTag() {
        isTestWithoutTagInvoked.set(true)
    }
}


class TagsTest {
    val engineEmulator = EngineEmulator()

    @Test
    fun `method with string tag invoked and without tag is not invoked`() {
        val request = engineEmulator.emulateDiscoveryStepForTestClass<StringTagClass>()
        engineEmulator.execute(request)
        StringTagClass.isTestWithTagInvoked.get().shouldBeTrue()
        StringTagClass.isTestWithoutTagInvoked.get().shouldBeFalse()
    }

    @Test
    fun `method with string tag not invoked and without tag is invoked`() {
        val request = engineEmulator.emulateDiscoveryStepForTestClass<StringTagClass>()
        engineEmulator.execute(request)
        StringTagClass.isTestWithTagInvoked.get().shouldBeFalse()
        StringTagClass.isTestWithoutTagInvoked.get().shouldBeTrue()
    }
}