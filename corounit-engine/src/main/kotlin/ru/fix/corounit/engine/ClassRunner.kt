package ru.fix.corounit.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.junit.jupiter.api.*
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

private val log = KotlinLogging.logger { }

class ClassRunner(
        private val context: ExecutionContext,
        private val classDesc: CorounitClassDescriptior
) {

    private val testInstanceLifecycle = resolveTestInstanceLifecycle(classDesc)

    private val beforeAllMethod: KFunction<*>?
    private val afterAllMethod: KFunction<*>?
    private val beforeEachMethod: KFunction<*>?
    private val afterEachMethod: KFunction<*>?

    init {
        val classWithBeforeAllAndAfterAllMethods = when (testInstanceLifecycle) {
            TestInstance.Lifecycle.PER_CLASS -> classDesc.clazz
            TestInstance.Lifecycle.PER_METHOD -> classDesc.clazz.companionObject
        }

        beforeAllMethod = classWithBeforeAllAndAfterAllMethods?.functions
                ?.filter { it.name == "beforeAll" || it.findAnnotation<BeforeAll>() != null }
                ?.firstOrNull { it.parameters.size == 1 }

        afterAllMethod = classWithBeforeAllAndAfterAllMethods?.functions
                ?.filter { it.name == "afterAll" || it.findAnnotation<AfterAll>() != null }
                ?.firstOrNull { it.parameters.size == 1 }

        beforeEachMethod = classDesc.clazz.functions
                .filter { it.name == "beforeEach" || it.findAnnotation<BeforeEach>() != null }
                .firstOrNull { it.parameters.size == 1 }

        afterEachMethod = classDesc.clazz.functions
                .filter { it.name == "afterEach" || it.findAnnotation<AfterEach>() != null }
                .firstOrNull { it.parameters.size == 1 }
    }

    suspend fun executeClass(methodsRunsSemaphore: Semaphore?) {
        val classContext = TestClassContextElement(classDesc.clazz)
        val pluginsClassContext = context.pluginDispatcher.beforeTestClass(classContext)

        context.notifyListenerAndRunInSupervisorScope(classDesc) {
            when (testInstanceLifecycle) {
                TestInstance.Lifecycle.PER_CLASS -> {
                    val testInstance = context.pluginDispatcher.createTestClassInstance(classDesc.clazz)

                    executeBeforeAllMethodIfPresent(classContext, testInstance)

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            methodsRunsSemaphore.withPermitIfNotNull {
                                executeMethod(classContext, methodDesc, testInstance)
                            }
                        }
                    }

                    executeAfterAllMethodIfPresent(classContext, testInstance)
                }
                TestInstance.Lifecycle.PER_METHOD -> {
                    classDesc.clazz.companionObjectInstance?.let { testInstance ->
                        executeBeforeAllMethodIfPresent(classContext, testInstance)
                    }

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            val testInstance = context.pluginDispatcher.createTestClassInstance(classDesc.clazz)
                            methodsRunsSemaphore.withPermitIfNotNull {
                                executeMethod(classContext, methodDesc, testInstance)
                            }
                        }
                    }

                    classDesc.clazz.companionObjectInstance?.let { testInstance ->
                        executeAfterAllMethodIfPresent(classContext, testInstance)
                    }
                }
            }
        }
        context.pluginDispatcher.afterTestClass(pluginsClassContext)
    }


    private fun resolveTestInstanceLifecycle(classDesc: CorounitClassDescriptior): TestInstance.Lifecycle {
        val annotationLifecycle = classDesc.clazz.findAnnotation<TestInstance>()?.value
        if (annotationLifecycle != null) return annotationLifecycle

        return context.configuration.testInstanceLifecycle
    }

    private suspend fun CoroutineScope.executeMethod(
            classContext: TestClassContextElement,
            methodDesc: CorounitMethodDescriptior,
            testInstance: Any) {

        val methodContext = classContext + TestMethodContextElement(methodDesc.method)

        val disabledAnnotation = methodDesc.method.findAnnotation<Disabled>()
        if (disabledAnnotation != null) {
            context.notifyAboutDisabledMethod(methodDesc, methodContext, disabledAnnotation)
            return
        }

        val pluginsMethodContext = context.pluginDispatcher.beforeTestMethod(methodContext)

        launch(pluginsMethodContext) {

            val thr = context.notifyListenerAndRunInSupervisorScope(methodDesc) {

                var testFailReason: Throwable? = null
                try {
                    beforeEachMethod?.invokeMethodOnTestInstance(testInstance)
                    methodDesc.method.invokeMethodOnTestInstance(testInstance)
                } catch (thr: Throwable) {
                    testFailReason = thr
                } finally {
                    try {
                        afterEachMethod?.invokeMethodOnTestInstance(testInstance)
                    } catch (afterEachThrowable: Throwable) {
                        if (testFailReason == null) {
                            testFailReason = afterEachThrowable
                        } else {
                            log.error(afterEachThrowable) { "Method ${testInstance.javaClass.name}.${afterEachMethod?.name} failed" }
                        }
                    }
                }

                if (testFailReason != null) {
                    throw testFailReason
                }
            }
            context.pluginDispatcher.afterTestMethod(pluginsMethodContext, thr)
        }
    }

    private suspend fun executeBeforeAllMethodIfPresent(
            classContext: TestClassContextElement,
            testInstance: Any
    ) {
        executeBeforeOrAfterAllMethodIfPresent(
                classContext,
                testInstance,
                beforeAllMethod,
                { methodContext -> context.pluginDispatcher.beforeBeforeAllMethod(methodContext) },
                { pluginContext, thr -> context.pluginDispatcher.afterBeforeAllMethod(pluginContext, thr) }
        )
    }

    private suspend fun executeAfterAllMethodIfPresent(
            classContext: TestClassContextElement,
            testInstance: Any
    ) {
        executeBeforeOrAfterAllMethodIfPresent(
                classContext,
                testInstance,
                afterAllMethod,
                { methodContext -> context.pluginDispatcher.beforeAfterAllMethod(methodContext) },
                { pluginContext, thr -> context.pluginDispatcher.afterAfterAllMethod(pluginContext, thr) }
        )
    }

    private suspend fun executeBeforeOrAfterAllMethodIfPresent(
            classContext: TestClassContextElement,
            testInstance: Any,
            method: KFunction<*>?,
            beforePluginCall: suspend (methodContext: CoroutineContext) -> CoroutineContext,
            afterPluginCall: suspend (pluginContext: CoroutineContext, thr: Throwable?) -> Unit
    ) {
        method?.let {
            val methodContext = classContext + TestMethodContextElement(it)
            val pluginContext = beforePluginCall(methodContext)
            withContext(pluginContext) {
                val throwable = try {
                    it.invokeMethodOnTestInstance(testInstance)
                    null
                } catch (thr: Throwable) {
                    thr
                }
                afterPluginCall(pluginContext, throwable)

                if (null != throwable) {
                    throw throwable
                }
            }
        }
    }

    private suspend fun KCallable<*>.invokeMethodOnTestInstance(testInstance: Any) {
        try {
            if (isSuspend) callSuspend(testInstance)
            else call(testInstance)
        } catch (invocationTargetExc: InvocationTargetException) {
            throw invocationTargetExc.cause ?: invocationTargetExc
        }
    }
}