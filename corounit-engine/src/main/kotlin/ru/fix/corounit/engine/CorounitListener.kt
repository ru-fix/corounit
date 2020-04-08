package ru.fix.corounit.engine

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult

interface CorounitListener {
    fun testRunStarted(testDescriptor: TestDescriptor)
    fun testStarted(testDescriptor: TestDescriptor)
    fun testRunFinished(testDescriptor: TestDescriptor)
    fun testFailure(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult)
    fun testFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult)
    fun testIgnored(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult)
}