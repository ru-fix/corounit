package ru.fix.corounit.engine

import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.util.concurrent.ConcurrentLinkedDeque

private val log = KotlinLogging.logger { }

@TestInstance(PER_CLASS)
class MyTestWithoutAnnotations {
    companion object : TestClassState()

    suspend fun beforeAll() {
        beforeAllInvoked()
    }

    suspend fun afterAll() {
        afterAllInvoked()
    }

    @Test
    suspend fun myTest() {
        testMethodInvoked(1)
    }
}

@TestInstance(PER_CLASS)
class MyTestWithAnnotations {
    companion object : TestClassState()

    @BeforeAll
    fun setUp() {
        beforeAllInvoked()
    }

    @AfterAll
    suspend fun tearDown() {
        afterAllInvoked()
    }

    @Test
    suspend fun myTest() {
        testMethodInvoked(1)
    }
}


@TestInstance(PER_CLASS)
class TestClassInstancePerClassInvocationWithAnnotation {
    companion object : TestClassState() {
        val instances = ConcurrentLinkedDeque<TestClassInstancePerClassInvocationWithAnnotation>()

        override fun reset() {
            super.reset()
            instances.clear()
        }
    }
    suspend fun beforeAll() {
        beforeAllInvoked()

    }

    fun afterAll() {
        afterAllInvoked()
    }

    @Test
    suspend fun firstMethod() {
        instances.addLast(this)
        testMethodInvoked(1)
    }

    @Test
    suspend fun secondMethod() {
        instances.addLast(this)
        testMethodInvoked(2)
    }
}

class BeforeAfterEach{
    companion object: TestClassState(){
    }
    suspend fun beforeEach(){
        beforeEachInvoked()
    }
    suspend fun afterEach(){
        afterEachInvoked()
    }

    @Test
    suspend fun firstMethod(){
        testMethodInvoked(1)
    }
    @Test
    suspend fun secondMethod(){
        testMethodInvoked(2)
    }
}

class BeforeAfterEachWithAnnotations{
    companion object: TestClassState(){
    }

    @BeforeEach
    suspend fun before(){
        beforeEachInvoked()
    }
    @AfterEach
    suspend fun after(){
        afterEachInvoked()
    }

    @Test
    suspend fun firstMethod(){
        testMethodInvoked(1)
    }
    @Test
    suspend fun secondMethod(){
        testMethodInvoked(2)
    }
}

class BeforeAndAfterMethodsTest {

    private val engine = EngineEmulator()


    @Test
    fun `beforeAll and afterAll invoked in test suite without annotations`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<MyTestWithoutAnnotations>()

        MyTestWithoutAnnotations.reset()

        engine.execute(executionRequest)

        MyTestWithoutAnnotations.beforeEachState.shouldContainExactly()
        MyTestWithoutAnnotations.beforeAllState.shouldContainExactly(1)
        MyTestWithoutAnnotations.testSequencesState.shouldContainExactly(2)
        MyTestWithoutAnnotations.afterEachState.shouldContainExactly()
        MyTestWithoutAnnotations.afterAllState.shouldContainExactly(3)
    }

    @Test
    fun `beforeAll and afterAll invoked in test suite with annotations`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<MyTestWithAnnotations>()

        MyTestWithAnnotations.reset()
        engine.execute(executionRequest)

        MyTestWithAnnotations.beforeEachState.shouldContainExactly()
        MyTestWithAnnotations.beforeAllState.shouldContainExactly(1)
        MyTestWithAnnotations.testSequencesState.shouldContainExactly(2)
        MyTestWithAnnotations.afterEachState.shouldContainExactly()
        MyTestWithAnnotations.afterAllState.shouldContainExactly(3)
    }


    @Test
    fun `beforeEach and afterEach invoked in test suite without annotations`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<BeforeAfterEach>()

        BeforeAfterEach.reset()
        engine.execute(executionRequest)

        BeforeAfterEach.beforeAllState.shouldBeEmpty()
        BeforeAfterEach.beforeEachState.shouldHaveSize(2)
        BeforeAfterEach.testSequencesState.shouldHaveSize(2)
        BeforeAfterEach.afterEachState.shouldHaveSize(2)
        BeforeAfterEach.afterAllState.shouldBeEmpty()
    }

    @Test
    fun `beforeEach and afterEach invoked in test suite with annotations`() {
        val executionRequest = engine.emulateDiscoveryStepForTestClass<BeforeAfterEachWithAnnotations>()

        BeforeAfterEachWithAnnotations.reset()
        engine.execute(executionRequest)

        BeforeAfterEachWithAnnotations.beforeAllState.shouldBeEmpty()
        BeforeAfterEachWithAnnotations.beforeEachState.shouldContain(1)
        BeforeAfterEachWithAnnotations.beforeEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.testSequencesState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldHaveSize(2)
        BeforeAfterEachWithAnnotations.afterEachState.shouldContain(6)
        BeforeAfterEachWithAnnotations.afterAllState.shouldBeEmpty()
    }


}