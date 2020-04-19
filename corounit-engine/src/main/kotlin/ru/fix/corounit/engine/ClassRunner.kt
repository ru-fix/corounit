package ru.fix.corounit.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.junit.jupiter.api.*
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KCallable
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation

class ClassRunner(
        private val context: ExecutionContext,
        private val classDesc: CorounitClassDescriptior
) {

    private val testInstanceLifecycle = resolveTestInstanceLifecycle(classDesc)

    private val beforeAllMethod: KCallable<*>?
    private val afterAllMethod: KCallable<*>?
    private val beforeEachMethod: KCallable<*>?
    private val afterEachMethod: KCallable<*>?

    init {
        val classWithBeforeAllAndAfterAllMethods = when (testInstanceLifecycle) {
            TestInstance.Lifecycle.PER_CLASS -> classDesc.clazz
            TestInstance.Lifecycle.PER_METHOD -> classDesc.clazz.companionObject
        }

        beforeAllMethod = classWithBeforeAllAndAfterAllMethods?.members
                ?.filter { it.name == "beforeAll" || it.findAnnotation<BeforeAll>() != null }
                ?.firstOrNull { it.parameters.size == 1 }

        afterAllMethod = classWithBeforeAllAndAfterAllMethods?.members
                ?.filter { it.name == "afterAll" || it.findAnnotation<AfterAll>() != null }
                ?.firstOrNull { it.parameters.size == 1 }

        beforeEachMethod = classDesc.clazz.members
                .filter { it.name == "beforeEach" || it.findAnnotation<BeforeEach>() != null }
                .firstOrNull { it.parameters.size == 1 }

        afterEachMethod = classDesc.clazz.members
                .filter { it.name == "afterEach" || it.findAnnotation<AfterEach>() != null }
                .firstOrNull { it.parameters.size == 1 }
    }

    suspend fun executeClass() {
        val classContext = TestClassContextElement(classDesc.clazz)
        val pluginsClassContext = context.pluginDispatcher.beforeTestClass(classContext)

        context.notifyListenerAndRunInSupervisorScope(classDesc) {
            when (testInstanceLifecycle) {
                TestInstance.Lifecycle.PER_CLASS -> {
                    val testInstance = context.pluginDispatcher.createTestClassInstance(classDesc.clazz)
                    beforeAllMethod?.invokeAspectMethodOnTestInstnace(testInstance)

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            executeMethod(classContext, methodDesc, testInstance)
                        }
                    }

                    afterAllMethod?.invokeAspectMethodOnTestInstnace(testInstance)

                }
                TestInstance.Lifecycle.PER_METHOD -> {
                    classDesc.clazz.companionObjectInstance?.let {
                        beforeAllMethod?.invokeAspectMethodOnTestInstnace(it)
                    }

                    supervisorScope {
                        for (methodDesc in classDesc.methodDescriptors) {
                            val testInstance = context.pluginDispatcher.createTestClassInstance(classDesc.clazz)
                            executeMethod(classContext, methodDesc, testInstance)
                        }
                    }

                    classDesc.clazz.companionObjectInstance?.let {
                        afterAllMethod?.invokeAspectMethodOnTestInstnace(it)
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

    private suspend fun KCallable<*>.invokeAspectMethodOnTestInstnace(testInstance: Any) {
        if (isSuspend) callSuspend(testInstance)
        else call(testInstance)
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
                try {
                    beforeEachMethod?.invokeAspectMethodOnTestInstnace(testInstance)
                    methodDesc.method.callSuspend(testInstance)
                } catch (invocationTargetExc: InvocationTargetException) {
                    throw invocationTargetExc.cause ?: invocationTargetExc
                } finally {
                    afterEachMethod?.invokeAspectMethodOnTestInstnace(testInstance)
                }
            }
            context.pluginDispatcher.afterTestMethod(pluginsMethodContext, thr)
        }
    }
}