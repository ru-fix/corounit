package ru.fix.corounit.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.supervisorScope
import org.junit.jupiter.api.Disabled
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import kotlin.coroutines.CoroutineContext

class ExecutionContext(
        val pluginDispatcher: PluginDispatcher,
        val listener: EngineExecutionListener,
        val configuration: Configuration
) {

    suspend fun notifyListenerAndRunInSupervisorScope(descriptor: TestDescriptor, block: suspend CoroutineScope.() -> Unit): Throwable? {
        listener.executionStarted(descriptor)
        try {
            supervisorScope {
                block()
            }
            listener.executionFinished(descriptor, TestExecutionResult.successful())
            return null
        } catch (thr: Throwable) {
            listener.executionFinished(descriptor, TestExecutionResult.failed(thr))
            return thr
        }
    }

    suspend fun notifyAboutDisabledMethod(methodDesc: CorounitMethodDescriptior,
                                                  methodContext: CoroutineContext,
                                                  disabledAnnotation: Disabled) {
        listener.executionSkipped(methodDesc, disabledAnnotation.value)
        pluginDispatcher.skipTestMethod(methodContext, disabledAnnotation.value)
    }
}