package ru.fix.corounit.engine

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.MethodSelector
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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
                .orElse(Math.max(ForkJoinPool.getCommonPoolParallelism(), 2))!!

        log.debug { "Corounit uses parallelism level: $parallelism" }

        val execDesc = request.rootTestDescriptor as CorounitExecutionDescriptor
        val pluginDispatcher = PluginDispatcher(execDesc)

        runBlockingInPool(parallelism) {

            val globalContext = pluginDispatcher.beforeAllTestClasses(coroutineContext)
            withContext(globalContext) {

                TestRunner(pluginDispatcher,
                        request.engineExecutionListener,
                        request.configurationParameters).executeExecution(execDesc)

                pluginDispatcher.afterAllTestClasses(globalContext)
            }
        }
    }
}