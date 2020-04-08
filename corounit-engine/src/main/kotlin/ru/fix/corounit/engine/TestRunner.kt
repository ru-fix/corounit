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
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.*

class TestRunner(
        private val pluginDispatcher: PluginDispatcher,
        private val listener: EngineExecutionListener,
        private val configurationParameters: ConfigurationParameters) {

    private suspend fun executeDescriptor(descriptor: TestDescriptor, block: suspend CoroutineScope.() -> Unit): Throwable? {
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

        val listeners = getListeners(classDesc.clazz.findAnnotation<Listeners>()?.classes)
        onTestRunStarted(classDesc, listeners)

        val beforeAllMethod = classWithBeforeAndAfterMethods?.members
                ?.filter { it.name == "beforeAll" || it.findAnnotation<BeforeAll>() != null }
                ?.firstOrNull { it.parameters.size == 1 && it.isSuspend }

        val afterAllMethod = classWithBeforeAndAfterMethods?.members
                ?.filter { it.name == "afterAll" || it.findAnnotation<AfterAll>() != null }
                ?.firstOrNull { it.parameters.size == 1 && it.isSuspend }

        val beforeEachMethod = classDesc.clazz.members
                .filter { it.name == "beforeEach" || it.findAnnotation<BeforeEach>() != null }
                .firstOrNull { it.parameters.size == 1 && it.isSuspend }

        val afterEachMethod = classDesc.clazz.members
                .filter { it.name == "afterEach" || it.findAnnotation<AfterEach>() != null }
                .firstOrNull { it.parameters.size == 1 && it.isSuspend }

        val classContext = CorounitContext()
        classContext[CorounitContext.TestClass] = classDesc.clazz
        val pluginsClassContext = pluginDispatcher.beforeTestClass(classContext)

        executeDescriptor(classDesc) {
            when (testInstanceLifecycle) {
                PER_CLASS -> {
                    val testInstance = pluginDispatcher.createTestClassInstance(classDesc.clazz)
                    beforeAllMethod?.callSuspend(testInstance)

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            launchMethod(classContext, methodDesc, testInstance, beforeEachMethod, afterEachMethod, listeners)
                        }
                    }

                    afterAllMethod?.callSuspend(testInstance)

                }
                PER_METHOD -> {
                    classDesc.clazz.companionObjectInstance?.let { beforeAllMethod?.callSuspend(it) }

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            val testInstance = pluginDispatcher.createTestClassInstance(classDesc.clazz)
                            launchMethod(classContext, methodDesc, testInstance, beforeEachMethod, afterEachMethod, listeners)
                        }
                    }

                    classDesc.clazz.companionObjectInstance?.let { afterAllMethod?.callSuspend(it) }
                }
            }
        }

        pluginDispatcher.afterTestClass(pluginsClassContext)
        onTestRunFinished(classDesc, listeners)
    }

    private suspend fun skipMethodAndNotifyIfDisabled(methodDesc: CorounitMethodDescriptior, methodContext: CorounitContext): Boolean{
        val disabledAnnotation = methodDesc.method.findAnnotation<Disabled>()
        if(disabledAnnotation != null){
            listener.executionSkipped(methodDesc, disabledAnnotation.value)
            pluginDispatcher.skipTestMethod(methodContext, disabledAnnotation.value)
            return true
        }
        return false
    }

    private suspend fun CoroutineScope.launchMethod(
            classContext: CorounitContext,
            methodDesc: CorounitMethodDescriptior,
            testInstance: Any,
            beforeEachMethod: KCallable<*>?,
            afterEachMethod: KCallable<*>?,
            listeners: List<CorounitListener>?
            ) {
        onTestStarted(methodDesc, listeners)
        val methodContext = classContext.copy()
        methodContext[CorounitContext.TestMethod] = methodDesc.method

        if(skipMethodAndNotifyIfDisabled(methodDesc, methodContext)){
            onTestSkipped(methodDesc, TestExecutionResult.aborted(null), listeners)
            onTestFinished(methodDesc, TestExecutionResult.aborted(null), listeners)
            return
        }

        val pluginsMethodContext = pluginDispatcher.beforeTestMethod(classContext + methodContext)

        launch(pluginsMethodContext) {

            val thr = executeDescriptor(methodDesc) {
                try {
                    beforeEachMethod?.callSuspend(testInstance)
                    methodDesc.method.callSuspend(testInstance)
                } catch (invocationTargetExc: InvocationTargetException) {
                    throw invocationTargetExc.cause ?: invocationTargetExc
                } finally {
                    afterEachMethod?.callSuspend(testInstance)
                }
            }
            pluginDispatcher.afterTestMethod(pluginsMethodContext, thr)
            if (thr == null) {
                onTestFinished(methodDesc, TestExecutionResult.successful(), listeners)
            } else {
                onTestFailure(methodDesc, TestExecutionResult.failed(thr), listeners)
                onTestFinished(methodDesc, TestExecutionResult.failed(thr), listeners)
            }
        }
    }

    suspend fun executeExecution(execDesc: CorounitExecutionDescriptor) {
        executeDescriptor(execDesc) {
            for (classDesc in execDesc.children.mapNotNull { it as? CorounitClassDescriptior }) {
                launch {
                    executeClass(classDesc)
                }
            }
        }
    }

    private fun getListeners(listeners: Array<KClass<out CorounitListener>>?): List<CorounitListener>? {
        if(listeners.isNullOrEmpty()) return null
        val listenerObjects: MutableList<CorounitListener> = mutableListOf()
        for (listener in listeners) listenerObjects.add(listener.createInstance())
        return listenerObjects
    }

    private fun onTestRunStarted(descriptor: TestDescriptor, listeners: List<CorounitListener>?) {
        listeners?.parallelStream()?.forEach{ it.testRunStarted(descriptor) }
    }

    private fun onTestStarted(descriptor: TestDescriptor, listeners: List<CorounitListener>?) {
        listeners?.parallelStream()?.forEach{ it.testStarted(descriptor) }
    }

    private fun onTestFinished(descriptor: TestDescriptor, result: TestExecutionResult, listeners: List<CorounitListener>?) {
        listeners?.parallelStream()?.forEach{ it.testFinished(descriptor, result) }
    }

    private fun onTestRunFinished(descriptor: TestDescriptor, listeners: List<CorounitListener>?) {
        listeners?.parallelStream()?.forEach{ it.testRunFinished(descriptor) }
    }

    private fun onTestSkipped(descriptor: TestDescriptor, result: TestExecutionResult, listeners: List<CorounitListener>?) {
        listeners?.parallelStream()?.forEach{ it.testIgnored(descriptor, result) }
    }

    private fun onTestFailure(descriptor: TestDescriptor, result: TestExecutionResult, listeners: List<CorounitListener>?) {
        listeners?.parallelStream()?.forEach{ it.testFailure(descriptor, result) }
    }
}