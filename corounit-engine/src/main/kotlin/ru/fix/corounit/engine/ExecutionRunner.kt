package ru.fix.corounit.engine

import kotlinx.coroutines.launch

class ExecutionRunner(private val context: ExecutionContext) {

    suspend fun executeExecution(execDesc: CorounitExecutionDescriptor) {
        context.notifyListenerAndRunInSupervisorScope(execDesc) {
            for (classDesc in execDesc.children.mapNotNull { it as? CorounitClassDescriptior }) {
                launch {
                    ClassRunner(context, classDesc).executeClass()
                }
            }
        }
    }
}