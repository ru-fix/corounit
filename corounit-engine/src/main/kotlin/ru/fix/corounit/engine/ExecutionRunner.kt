package ru.fix.corounit.engine

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ExecutionRunner(private val context: ExecutionContext) {

    private val classesRunsSemaphore = context.configuration.concurrentTestClassesLimit?.let { Semaphore(it) }
    private val methodsRunsSemaphore = context.configuration.concurrentTestMethodsLimit?.let { Semaphore(it) }

    suspend fun executeExecution(execDesc: CorounitExecutionDescriptor) {
        context.notifyListenerAndRunInSupervisorScope(execDesc) {
            for (classDesc in execDesc.children.mapNotNull { it as? CorounitClassDescriptior }) {
                launch {
                    classesRunsSemaphore.withPermitIfNotNull {
                        ClassRunner(context, classDesc).executeClass(methodsRunsSemaphore)
                    }
                }
            }
        }
    }
}

suspend fun Semaphore?.withPermitIfNotNull(block: suspend () -> Unit) = if(this != null) {
    withPermit { block() }
} else {
    block()
}