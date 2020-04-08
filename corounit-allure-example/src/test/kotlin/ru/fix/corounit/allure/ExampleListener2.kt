package ru.fix.corounit.allure

import mu.KotlinLogging
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import ru.fix.corounit.engine.CorounitListener

private val log = KotlinLogging.logger { }

class ExampleListener2 : CorounitListener {
    override fun testRunStarted(testDescriptor: TestDescriptor) {
        log.debug { "Test run started: ${testDescriptor.displayName}" }
    }

    override fun testStarted(testDescriptor: TestDescriptor) {
        log.debug { "Test started: ${testDescriptor.displayName}" }
    }

    override fun testRunFinished(testDescriptor: TestDescriptor) {
        log.debug { "Test run finished: ${testDescriptor.displayName}" }
    }

    override fun testFailure(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
        log.debug { "Test failed: ${testDescriptor.displayName} with result: ${testExecutionResult.status}" }
    }

    override fun testFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
        log.debug { "Test finished: ${testDescriptor.displayName} with result: ${testExecutionResult.status}" }
    }

    override fun testIgnored(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
        log.debug { "Test ignored: ${testDescriptor.displayName} with result: ${testExecutionResult.status}" }
    }
}