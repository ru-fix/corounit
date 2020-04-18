package ru.fix.corounit.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KCallable
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation

class ExecutionRunner(
        private val pluginDispatcher: PluginDispatcher,
        private val listener: EngineExecutionListener,
        private val configurationParameters: ConfigurationParameters) {

    private suspend fun notifyListenerSupervisorScope(descriptor: TestDescriptor, block: suspend CoroutineScope.() -> Unit): Throwable? {
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

    private fun resolveTestInstanceLifecycle(classDesc: CorounitClassDescriptior): TestInstance.Lifecycle {
        val annotationLifecycle = classDesc.clazz.findAnnotation<TestInstance>()?.value
        if (annotationLifecycle != null) return annotationLifecycle

        val configLifecycle = configurationParameters.get("corounit.testinstance.lifecycle.default")
        if (configLifecycle.isPresent) {
            if (configLifecycle.get() == "per_class") {
                return PER_CLASS
            }
        }
        return PER_METHOD
    }

    private suspend fun executeClass(classDesc: CorounitClassDescriptior) {
        val testInstanceLifecycle = resolveTestInstanceLifecycle(classDesc)

        val classWithBeforeAndAfterMethods = when (testInstanceLifecycle) {
            PER_CLASS -> classDesc.clazz
            PER_METHOD -> classDesc.clazz.companionObject
        }

        val beforeAllMethod = classWithBeforeAndAfterMethods?.members
                ?.filter { it.name == "beforeAll" || it.findAnnotation<BeforeAll>() != null }
                ?.firstOrNull { it.parameters.size == 1 }

        val afterAllMethod = classWithBeforeAndAfterMethods?.members
                ?.filter { it.name == "afterAll" || it.findAnnotation<AfterAll>() != null }
                ?.firstOrNull { it.parameters.size == 1 }

        val beforeEachMethod = classDesc.clazz.members
                .filter { it.name == "beforeEach" || it.findAnnotation<BeforeEach>() != null }
                .firstOrNull { it.parameters.size == 1 }

        val afterEachMethod = classDesc.clazz.members
                .filter { it.name == "afterEach" || it.findAnnotation<AfterEach>() != null }
                .firstOrNull { it.parameters.size == 1 }

        val classContext = TestClassContextElement(classDesc.clazz)

        val pluginsClassContext = pluginDispatcher.beforeTestClass(classContext)

        notifyListenerSupervisorScope(classDesc) {
            when (testInstanceLifecycle) {
                PER_CLASS -> {
                    val testInstance = pluginDispatcher.createTestClassInstance(classDesc.clazz)
                    beforeAllMethod?.invokeAspectMethodOfTestInstnace(testInstance)

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            launchMethod(classContext, methodDesc, testInstance, beforeEachMethod, afterEachMethod)
                        }
                    }

                    afterAllMethod?.invokeAspectMethodOfTestInstnace(testInstance)

                }
                PER_METHOD -> {
                    classDesc.clazz.companionObjectInstance?.let { beforeAllMethod?.invokeAspectMethodOfTestInstnace(it) }

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            val testInstance = pluginDispatcher.createTestClassInstance(classDesc.clazz)
                            launchMethod(classContext, methodDesc, testInstance, beforeEachMethod, afterEachMethod)
                        }
                    }

                    classDesc.clazz.companionObjectInstance?.let { afterAllMethod?.invokeAspectMethodOfTestInstnace(it) }
                }
            }
        }

        pluginDispatcher.afterTestClass(pluginsClassContext)
    }

    private suspend fun skipMethodAndNotifyIfDisabled(methodDesc: CorounitMethodDescriptior, methodContext: CoroutineContext): Boolean {
        val disabledAnnotation = methodDesc.method.findAnnotation<Disabled>()
        if (disabledAnnotation != null) {
            listener.executionSkipped(methodDesc, disabledAnnotation.value)
            pluginDispatcher.skipTestMethod(methodContext, disabledAnnotation.value)
            return true
        }
        return false
    }

    private suspend fun CoroutineScope.launchMethod(
            classContext: TestClassContextElement,
            methodDesc: CorounitMethodDescriptior,
            testInstance: Any,
            beforeEachMethod: KCallable<*>?,
            afterEachMethod: KCallable<*>?
    ) {
        val methodContext = classContext + TestMethodContextElement(methodDesc.method)

        if (skipMethodAndNotifyIfDisabled(methodDesc, methodContext)) {
            return
        }

        val pluginsMethodContext = pluginDispatcher.beforeTestMethod(methodContext)

        launch(pluginsMethodContext) {

            val thr = notifyListenerSupervisorScope(methodDesc) {
                try {
                    beforeEachMethod?.invokeAspectMethodOfTestInstnace(testInstance)
                    methodDesc.method.callSuspend(testInstance)
                } catch (invocationTargetExc: InvocationTargetException) {
                    throw invocationTargetExc.cause ?: invocationTargetExc
                } finally {
                    afterEachMethod?.invokeAspectMethodOfTestInstnace(testInstance)
                }
            }
            pluginDispatcher.afterTestMethod(pluginsMethodContext, thr)
        }
    }

    suspend fun executeExecution(execDesc: CorounitExecutionDescriptor) {
        notifyListenerSupervisorScope(execDesc) {
            for (classDesc in execDesc.children.mapNotNull { it as? CorounitClassDescriptior }) {
                launch {
                    executeClass(classDesc)
                }
            }
        }
    }

    private suspend fun KCallable<*>.invokeAspectMethodOfTestInstnace(testInstance: Any) {
        if (isSuspend) callSuspend(testInstance)
        else call(testInstance)
    }
}