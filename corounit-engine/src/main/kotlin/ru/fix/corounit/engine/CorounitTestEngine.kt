package ru.fix.corounit.engine

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.MethodSelector
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

private val log = KotlinLogging.logger { }

class CorounitTestEngine : TestEngine {

    override fun getId(): String = "corounit"

    override fun discover(request: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {

        val execDesc = CorounitExecutionDescriptor(uniqueId)

        //TODO: reuse all types of selector form junit resolving engine,
        // see org.junit.platform.engine.support.discovery.ClassContainerSelectorResolver

        request.getSelectorsByType(MethodSelector::class.java).forEach { selector ->
            val method = selector.javaMethod.kotlinFunction

            if (method != null && method.isSuspend && method.javaMethod!!.isAnnotationPresent(Test::class.java)) {

                val classDesc = CorounitClassDescriptior(execDesc.uniqueId, selector.javaClass.kotlin)
                execDesc.addChild(classDesc)

                classDesc.addChild(CorounitMethodDescriptior(classDesc.uniqueId, method))
            }
        }

        request.getSelectorsByType(ClassSelector::class.java).forEach { selector ->

            val classDesc = CorounitClassDescriptior(execDesc.uniqueId, selector.javaClass.kotlin)
            execDesc.addChild(classDesc)

            for (method in ReflectionSupport.findMethods(
                    selector.javaClass,
                    { method ->
                        method.kotlinFunction?.isSuspend ?: false &&
                                method.isAnnotationPresent(Test::class.java)
                    },
                    HierarchyTraversalMode.TOP_DOWN).mapNotNull { it.kotlinFunction }) {

                classDesc.addChild(
                        CorounitMethodDescriptior(
                                classDesc.uniqueId,
                                method)
                )
            }
        }

        return execDesc
    }

    fun runBlockingInPool(parallelism: Int, block: suspend CoroutineScope.() -> Unit) {
        val threadCounter = AtomicInteger()
        val executor = Executors.newFixedThreadPool(parallelism) { Thread(it, "corounit-${threadCounter.getAndIncrement()}") }
        try {
            runBlocking(executor.asCoroutineDispatcher() +
                    SupervisorJob() +
                    CoroutineExceptionHandler { _, thr -> log.error(thr) { } }) {

                block()
            }
        } catch (exc: Exception) {
            log.error(exc) { }
        }
        executor.shutdown()
        require(executor.awaitTermination(1, TimeUnit.MINUTES))

    }


    override fun execute(request: ExecutionRequest) {

        val parallelism = request.configurationParameters.get("corounit.execution.parallelism")
                .map { it?.toInt() }
                .orElse(ForkJoinPool.getCommonPoolParallelism())!!

        log.debug { "Corounit uses parallelism level: $parallelism" }

        val execDesc = request.rootTestDescriptor as CorounitExecutionDescriptor

        val pluginDispatcher = PluginDispatcher(execDesc)


        runBlockingInPool(parallelism) {

            val globalContext = pluginDispatcher.beforeAllTestClasses(coroutineContext)
            withContext(globalContext) {
                suspend fun execute(descriptor: TestDescriptor, block: suspend CoroutineScope.() -> Unit): Throwable? {
                    request.engineExecutionListener.executionStarted(descriptor)
                    try {
                        supervisorScope {
                            block()
                        }
                        request.engineExecutionListener.executionFinished(descriptor, TestExecutionResult.successful())
                        return null
                    } catch (thr: Throwable) {
                        request.engineExecutionListener.executionFinished(descriptor, TestExecutionResult.failed(thr))
                        return thr
                    }
                }


                execute(execDesc) {
                    for (classDesc in execDesc.children.mapNotNull { it as? CorounitClassDescriptior }) {
                        launch {
                            val testInstance = pluginDispatcher.createTestClassInstance(classDesc.clazz)
                            val classContext = CorounitContext()
                            classContext[CorounitContext.TestClass] = classDesc.clazz
                            val pluginsClassContext = pluginDispatcher.beforeTestClass(classContext)
                            execute(classDesc) {

                                val beforeAll = classDesc.clazz.members
                                        .filter { it.name == "beforeAll" || it.findAnnotation<BeforeAll>() != null }
                                        .firstOrNull { it.parameters.size == 1 && it.isSuspend }


                                val afterAll = classDesc.clazz.members
                                        .filter { it.name == "afterAll" || it.findAnnotation<AfterAll>() != null }
                                        .firstOrNull { it.parameters.size == 1 && it.isSuspend}


                                beforeAll?.callSuspend(testInstance)

                                launch {
                                    supervisorScope {
                                        for (methodDesc in classDesc.children.mapNotNull { it as? CorounitMethodDescriptior }) {
                                            val methodContext = classContext.copy()
                                            methodContext[CorounitContext.TestMethod] = methodDesc.method

                                            val pluginsMethodContext = pluginDispatcher.beforeTestMethod(classContext + methodContext)

                                            launch(pluginsMethodContext) {

                                                val thr = execute(methodDesc) {
                                                    try {
                                                        methodDesc.method.callSuspend(testInstance)
                                                    } catch (invocationTargetExc: InvocationTargetException) {
                                                        throw invocationTargetExc.cause ?: invocationTargetExc
                                                    }
                                                }
                                                pluginDispatcher.afterTestMethod(pluginsMethodContext, thr)
                                            }
                                        }
                                    }
                                    afterAll?.callSuspend(testInstance)
                                }
                            }
                            pluginDispatcher.afterTestClass(pluginsClassContext)
                        }
                    }
                }
                pluginDispatcher.afterAllTestClasses(globalContext)
            }
        }
    }
}