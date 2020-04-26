package ru.fix.corounit.engine.test.discovery.plain

import org.junit.jupiter.api.Test
import ru.fix.corounit.engine.TestClassState

class PlainDiscoveryTest {

    companion object: TestClassState()

    @Test
    suspend fun testMethod1(){
        testMethodInvoked(1)
    }
}