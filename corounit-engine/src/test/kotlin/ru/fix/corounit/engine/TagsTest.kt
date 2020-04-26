package ru.fix.corounit.engine

import io.kotlintest.matchers.collections.shouldContainAll
import io.kotlintest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestTag

/**
 * JUnit launcher filters tests by Tags between [org.junit.platform.engine.TestEngine.discover]
 * and [org.junit.platform.engine.TestEngine.execute].
 * Goal of [org.junit.platform.engine.TestEngine]
 * is to correctly populate [org.junit.platform.engine.TestDescriptor] with tags.
 */
class TagsTest {
    val engineEmulator = EngineEmulator()

    @Tag("myClassTag")
    class StringTagClass{

        companion object: TestClassState(){
            val TEST_WITH_TAG = 1
            val TEST_WITHOUT_TAG = 2
        }


        @Tag("myMethodTag")
        @Test
        suspend fun testWithTag(){
            testMethodInvoked(TEST_WITH_TAG)
        }

        @Test
        suspend fun testWithoutTag() {
            testMethodInvoked(TEST_WITHOUT_TAG)
        }
    }

    @Test
    fun `class and method tags are resolved`() {
        val request = engineEmulator.emulateDiscoveryForTestClass<StringTagClass>()

        val classDescriptor = request.rootTestDescriptor.children
                .single { it is CorounitClassDescriptior }
        classDescriptor.tags.shouldContainExactly(TestTag.create("myClassTag"))

        val methodDescriptor = classDescriptor.children
                .single { it is CorounitMethodDescriptior && it.method.name == StringTagClass::testWithTag.name }
        methodDescriptor.tags.shouldContainExactly(TestTag.create("myMethodTag"))

        StringTagClass.reset()
        engineEmulator.execute(request)

        StringTagClass.methodIdsState.shouldContainAll(
                StringTagClass.TEST_WITHOUT_TAG,
                StringTagClass.TEST_WITH_TAG)
    }

    @Tag("myTag")
    annotation class MyTagAnnotation

    @MyTagAnnotation
    class AnnotationTagClass{

        companion object: TestClassState(){
            val TEST_WITH_TAG = 1
            val TEST_WITHOUT_TAG = 2
        }

        @MyTagAnnotation
        @Test
        suspend fun testWithTag(){
            testMethodInvoked(TEST_WITH_TAG)
        }

        @Test
        suspend fun testWithoutTag() {
            testMethodInvoked(TEST_WITHOUT_TAG)
        }
    }

    @Test
    fun `class and method annotation class tags are resolved`() {
        val request = engineEmulator.emulateDiscoveryForTestClass<AnnotationTagClass>()

        val classDescriptor = request.rootTestDescriptor.children
                .single { it is CorounitClassDescriptior }
        classDescriptor.tags.shouldContainExactly(TestTag.create("myTag"))

        val methodDescriptor = classDescriptor.children
                .single { it is CorounitMethodDescriptior && it.method.name == AnnotationTagClass::testWithTag.name }
        methodDescriptor.tags.shouldContainExactly(TestTag.create("myTag"))

        AnnotationTagClass.reset()
        engineEmulator.execute(request)

        AnnotationTagClass.methodIdsState.shouldContainAll(
                AnnotationTagClass.TEST_WITHOUT_TAG,
                AnnotationTagClass.TEST_WITH_TAG)
    }

}