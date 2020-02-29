package ru.fix.corounit.engine

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

private val log = KotlinLogging.logger { }

class CorounitTestEngine : TestEngine {

    override fun getId(): String = "corounit"

    override fun discover(request: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {

        val execDesc = CorounitExecutionDescriptor(uniqueId)

        val classNameFilter = buildClassNamePredicate(request)
        val classFilter = Predicate<Class<*>> { testClass ->
            ReflectionSupport.findMethods(
                    testClass,
                    { method ->
                        method.kotlinFunction?.isSuspend ?: false &&
                                method.isAnnotationPresent(Test::class.java)
                    },
                    HierarchyTraversalMode.TOP_DOWN)
                    .mapNotNull { it.kotlinFunction }
                    .any()
        }

        request.getSelectorsByType(MethodSelector::class.java).forEach { selector ->
            val method = selector.javaMethod.kotlinFunction ?: return@forEach

            if (!classNameFilter.test(method.javaClass.name)) {
                return@forEach
            }

            if (method.isSuspend && method.javaMethod!!.isAnnotationPresent(Test::class.java)) {
                val classDesc = CorounitClassDescriptior(execDesc.uniqueId, selector.javaClass.kotlin)
                execDesc.addChild(classDesc)

                classDesc.addChild(CorounitMethodDescriptior(classDesc.uniqueId, method))
            }
        }

        fun addTestClassToDescriptor(execDesc: CorounitExecutionDescriptor, testClass: Class<*>) {
            val classDesc = CorounitClassDescriptior(execDesc.uniqueId, testClass.kotlin)
            execDesc.addChild(classDesc)

            for (method in ReflectionSupport.findMethods(
                    testClass,
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

        request.getSelectorsByType(ClassSelector::class.java).forEach { selector ->
            val selectorClass = selector.getJavaClass()
            if (!classNameFilter.test(selectorClass.name)) {
                return@forEach
            }
            addTestClassToDescriptor(execDesc, selectorClass)
        }

        request.getSelectorsByType(ClasspathRootSelector::class.java).forEach { selector ->
            ReflectionSupport.findAllClassesInClasspathRoot(selector.classpathRoot, classFilter, classNameFilter)
                    .forEach { addTestClassToDescriptor(execDesc, it) }
        }

        request.getSelectorsByType(ModuleSelector::class.java).forEach { selector ->
            ReflectionSupport.findAllClassesInModule(selector.moduleName, classFilter, classNameFilter)
                    .forEach { addTestClassToDescriptor(execDesc, it) }
        }

        request.getSelectorsByType(PackageSelector::class.java).forEach { selector ->
            ReflectionSupport.findAllClassesInPackage(selector.packageName, classFilter, classNameFilter)
                    .forEach { addTestClassToDescriptor(execDesc, it) }
        }

        return execDesc
    }

    private fun buildClassNamePredicate(request: EngineDiscoveryRequest): Predicate<String> {
        val filters: MutableList<DiscoveryFilter<String>> = ArrayList()
        filters.addAll(request.getFiltersByType(ClassNameFilter::class.java))
        filters.addAll(request.getFiltersByType(PackageNameFilter::class.java))
        return Filter.composeFilters(filters).toPredicate()
    }

    private fun runBlockingInPool(parallelism: Int, block: suspend CoroutineScope.() -> Unit) {
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