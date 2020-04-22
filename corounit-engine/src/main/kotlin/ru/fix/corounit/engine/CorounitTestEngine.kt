package ru.fix.corounit.engine

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.*
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate
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
                        isMethodASuspendableKotlinTestFunction(method)
                    },
                    HierarchyTraversalMode.TOP_DOWN)
                    .mapNotNull { it.kotlinFunction }
                    .any()
        }

        request.getSelectorsByType(MethodSelector::class.java).forEach { selector ->
            if (!isMethodASuspendableKotlinTestFunction(selector.javaMethod)) {
                return@forEach
            }
            val method = selector.javaMethod.kotlinFunction!!

            if (!classNameFilter.test(method.javaClass.name)) {
                return@forEach
            }

            val classDesc = CorounitClassDescriptior(execDesc.uniqueId, selector.javaClass.kotlin)
            execDesc.addChild(classDesc)

            classDesc.addChild(CorounitMethodDescriptior(classDesc.uniqueId, method))
        }

        fun addTestClassToDescriptor(execDesc: CorounitExecutionDescriptor, testClass: Class<*>) {
            val classDesc = CorounitClassDescriptior(execDesc.uniqueId, testClass.kotlin)
            execDesc.addChild(classDesc)
            log.trace { "Discover class $testClass" }
            try {

                for (method in ReflectionSupport.findMethods(
                        testClass,
                        { method ->
                            isMethodASuspendableKotlinTestFunction(method)
                        },
                        HierarchyTraversalMode.TOP_DOWN).mapNotNull { it.kotlinFunction }) {

                    classDesc.addChild(
                            CorounitMethodDescriptior(
                                    classDesc.uniqueId,
                                    method)
                    )
                }
            } catch (exc: Exception) {
                log.error(exc) { "Failed to search for test methods in class $testClass" }
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

    private fun isMethodASuspendableKotlinTestFunction(method: Method): Boolean {
        try {
            //check annotation first in order to minimize kotlin reflection access
            if (!method.isAnnotationPresent(Test::class.java))
                return false

            val kotlinMethod = method.kotlinFunction ?: return false

            if (!kotlinMethod.isSuspend)
                return false

            return true

        } catch (exc: Exception) {
            log.trace(exc) {
                """
                Failed to detect if method is a test though reflection.
                Kotlin reflection could fail with UnsupportedOperationException 
                if it access generated technical classes.
                """
            }
            return false
        }
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

        val config = Configuration(request.configurationParameters)

        log.debug { "Corounit uses parallelism level: ${config.parallelism}" }

        val execDesc = request.rootTestDescriptor as CorounitExecutionDescriptor
        val pluginDispatcher = PluginDispatcher(execDesc)

        runBlockingInPool(config.parallelism) {

            val globalContext = pluginDispatcher.beforeAllTestClasses(coroutineContext)
            withContext(globalContext) {

                val context = ExecutionContext(pluginDispatcher,
                        request.engineExecutionListener,
                        config)

                ExecutionRunner(context).executeExecution(execDesc)

                pluginDispatcher.afterAllTestClasses(globalContext)
            }
        }
    }
}