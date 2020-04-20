package ru.fix.corounit.engine

import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry
import java.util.concurrent.ConcurrentLinkedDeque

class EngineExecutionTrapListener : EngineExecutionListener {
    var finishedTests = ConcurrentLinkedDeque<Pair<TestDescriptor, TestExecutionResult>>()
    var skippedTests = ConcurrentLinkedDeque<Pair<TestDescriptor, String>>()

    override fun executionFinished(descriptor: TestDescriptor, result: TestExecutionResult) {
        finishedTests.addLast(descriptor to result)
    }

    override fun reportingEntryPublished(p0: TestDescriptor?, p1: ReportEntry?) {
    }

    override fun executionSkipped(descriptor: TestDescriptor, reason: String) {
        skippedTests.addLast(descriptor to reason)
    }

    override fun executionStarted(p0: TestDescriptor?) {
    }

    override fun dynamicTestRegistered(p0: TestDescriptor?) {
    }
}