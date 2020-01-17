package ru.fix.corounit.engine

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.MethodSelector
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

private val log = KotlinLogging.logger { }

class CorounitTestEngine : TestEngine {

    class PluginDispatcher : CorounitPlugin {
        private val plugins = ServiceLoader.load(CorounitPlugin::class.java)
        private suspend fun dispatch(
                context: CoroutineContext,
                action: suspend CorounitPlugin.(CoroutineContext) -> CoroutineContext): CoroutineContext {

            var currentContext = context
            for (plugin in plugins) {
                try {
                    currentContext = plugin.action(currentContext)
                } catch (exc: Exception) {
                    log.error(exc) {
                        """
                        Failed to dispatch plugin event.
                        Plugin class: ${plugin::class.java}.
                        Plugin: $plugin
                        """.trimIndent()
                    }
                }
            }
            return currentContext
        }


        override suspend fun beforeTestClass(testClassContext: CoroutineContext): CoroutineContext {
            return dispatch(testClassContext) { beforeTestClass(it) }
        }

        override suspend fun afterTestClass(testClassContext: CoroutineContext) {
            dispatch(testClassContext) {
                afterTestClass(it)
                it
            }

        }

        override suspend fun beforeTestMethod(testMethodContext: CoroutineContext): CoroutineContext {
            return dispatch(testMethodContext) { beforeTestMethod(it) }
        }

        override suspend fun afterTestMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
            dispatch(testMethodContext) {
                afterTestMethod(it, thr)
                it
            }
        }
    }

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

        val pluginDispatcher = PluginDispatcher()


        runBlockingInPool(parallelism) {

            //TODO: implement support for @BeforeAll and @AfterAll

            suspend fun execute(descriptor: TestDescriptor, block: suspend CoroutineScope.() -> Unit): Exception? {
                request.engineExecutionListener.executionStarted(descriptor)
                try {
                    supervisorScope {
                        block()
                    }
                    request.engineExecutionListener.executionFinished(descriptor, TestExecutionResult.successful())
                    return null
                } catch (exc: Exception) {
                    request.engineExecutionListener.executionFinished(descriptor, TestExecutionResult.failed(exc))
                    return exc
                }
            }

            val execDesc = request.rootTestDescriptor as CorounitExecutionDescriptor
            execute(execDesc) {
                for (classDesc in execDesc.children.mapNotNull { it as? CorounitClassDescriptior }) {
                    launch {
                        val testInstance = classDesc.clazz.createInstance()
                        val classContext = CorounitContext()
                        classContext[CorounitContext.TestClass] = classDesc.clazz
                        val pluginsClassContext = pluginDispatcher.beforeTestClass(classContext)
                        execute(classDesc) {

                            for (methodDesc in classDesc.children.mapNotNull { it as? CorounitMethodDescriptior }) {
                                val methodContext = CorounitContext()
                                methodContext[CorounitContext.TestMethod] = methodDesc.method

                                val pluginsMethodContext = pluginDispatcher.beforeTestMethod(classContext + methodContext)

                                launch(pluginsMethodContext) {

                                    val exc = execute(methodDesc) {
                                        methodDesc.method.callSuspend(testInstance)
                                    }
                                    pluginDispatcher.afterTestMethod(pluginsMethodContext, exc)
                                }
                            }
                        }
                        pluginDispatcher.afterTestClass(pluginsClassContext)
                    }
                }
            }
        }
    }
}