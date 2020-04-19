package ru.fix.corounit.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.junit.jupiter.api.*
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KCallable
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation

class ClassRunner(
        private val context: ExecutionContext,
        private val classDesc: CorounitClassDescriptior
) {
    suspend fun executeClass() {
        val testInstanceLifecycle = resolveTestInstanceLifecycle(classDesc)

        val classWithBeforeAndAfterMethods = when (testInstanceLifecycle) {
            TestInstance.Lifecycle.PER_CLASS -> classDesc.clazz
            TestInstance.Lifecycle.PER_METHOD -> classDesc.clazz.companionObject
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

        val pluginsClassContext = context.pluginDispatcher.beforeTestClass(classContext)

        context.notifyListenerAndRunInSupervisorScope(classDesc) {
            when (testInstanceLifecycle) {
                TestInstance.Lifecycle.PER_CLASS -> {
                    val testInstance = context.pluginDispatcher.createTestClassInstance(classDesc.clazz)
                    beforeAllMethod?.invokeAspectMethodOfTestInstnace(testInstance)

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            launchMethod(classContext, methodDesc, testInstance, beforeEachMethod, afterEachMethod)
                        }
                    }

                    afterAllMethod?.invokeAspectMethodOfTestInstnace(testInstance)

                }
                TestInstance.Lifecycle.PER_METHOD -> {
                    classDesc.clazz.companionObjectInstance?.let { beforeAllMethod?.invokeAspectMethodOfTestInstnace(it) }

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            val testInstance = context.pluginDispatcher.createTestClassInstance(classDesc.clazz)
                            launchMethod(classContext, methodDesc, testInstance, beforeEachMethod, afterEachMethod)
                        }
                    }

                    classDesc.clazz.companionObjectInstance?.let { afterAllMethod?.invokeAspectMethodOfTestInstnace(it) }
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

    private suspend fun KCallable<*>.invokeAspectMethodOfTestInstnace(testInstance: Any) {
        if (isSuspend) callSuspend(testInstance)
        else call(testInstance)
    }

    private suspend fun CoroutineScope.launchMethod(
            classContext: TestClassContextElement,
            methodDesc: CorounitMethodDescriptior,
            testInstance: Any,
            beforeEachMethod: KCallable<*>?,
            afterEachMethod: KCallable<*>?
    ) {
        val methodContext = classContext + TestMethodContextElement(methodDesc.method)

        val disabledAnnotation = methodDesc.method.findAnnotation<Disabled>()
        if (disabledAnnotation != null) {
            skipMethodAndNotifyIfDisabled(methodDesc, methodContext, disabledAnnotation)
            return
        }


        val pluginsMethodContext = context.pluginDispatcher.beforeTestMethod(methodContext)

        launch(pluginsMethodContext) {

            val thr = context.notifyListenerAndRunInSupervisorScope(methodDesc) {
                try {
                    beforeEachMethod?.invokeAspectMethodOfTestInstnace(testInstance)
                    methodDesc.method.callSuspend(testInstance)
                } catch (invocationTargetExc: InvocationTargetException) {
                    throw invocationTargetExc.cause ?: invocationTargetExc
                } finally {
                    afterEachMethod?.invokeAspectMethodOfTestInstnace(testInstance)
                }
            }
            context.pluginDispatcher.afterTestMethod(pluginsMethodContext, thr)
        }
    }

    private suspend fun skipMethodAndNotifyIfDisabled(methodDesc: CorounitMethodDescriptior,
                                                      methodContext: CoroutineContext,
                                                      disabledAnnotation: Disabled) {
        context.listener.executionSkipped(methodDesc, disabledAnnotation.value)
        context.pluginDispatcher.skipTestMethod(methodContext, disabledAnnotation.value)
    }
}