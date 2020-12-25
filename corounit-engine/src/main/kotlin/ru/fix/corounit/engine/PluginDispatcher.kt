package ru.fix.corounit.engine

import mu.KotlinLogging
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions

private val log = KotlinLogging.logger { }

class PluginDispatcher(execDesc: CorounitExecutionDescriptor) : CorounitPlugin {
    private val plugins: List<CorounitPlugin>
    private val testClassInstanceCreator: CorounitPlugin?

    init {
        val byServiceLoader = ServiceLoader.load(CorounitPlugin::class.java)
        val byConfigObjects = execDesc.classDescriptors
                .flatMap {
                    it.clazz.java.packageName
                            .split('.')
                            .fold(mutableListOf<String>()) { acc, part ->
                                if (acc.isEmpty()) {
                                    acc.add(part)
                                } else {
                                    acc.add("${acc.last()}.$part")
                                }
                                acc
                            }
                }
                .toHashSet()
                .sortedByDescending { it.length }
                .mapNotNull {
                    try {
                        Class.forName("$it.CorounitConfig")
                    } catch (notFound: ClassNotFoundException) {
                        null
                    }
                }

        val byConfigPlugins = byConfigObjects.mapNotNull {
            it.kotlin.objectInstance as? CorounitPlugin
        }
        val byConfigPluginsProvidersPlugins = byConfigObjects.mapNotNull {
            it.kotlin.objectInstance as? CorounitPluginsProvider
        }.flatMap { it.plugins() }

        plugins = byServiceLoader + byConfigPlugins + byConfigPluginsProvidersPlugins

        testClassInstanceCreator = plugins.find {
            it.javaClass.kotlin.declaredFunctions.any {
                it.name == "createTestClassInstance"
            }
        }
    }

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

    override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
        return dispatch(globalContext) { beforeAllTestClasses(it) }
    }

    override suspend fun afterAllTestClasses(globalContext: CoroutineContext) {
        dispatch(globalContext) {
            afterAllTestClasses(it)
            it
        }
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

    override suspend fun beforeBeforeAllMethod(testMethodContext: CoroutineContext): CoroutineContext {
        return dispatch(testMethodContext) { beforeBeforeAllMethod(it)}
    }

    override suspend fun afterBeforeAllMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
        dispatch(testMethodContext) {
            afterBeforeAllMethod(it, thr)
            it
        }
    }

    override suspend fun beforeAfterAllMethod(testMethodContext: CoroutineContext): CoroutineContext {
        return dispatch(testMethodContext) { beforeAfterAllMethod(it)}
    }

    override suspend fun afterAfterAllMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
        dispatch(testMethodContext) {
            afterAfterAllMethod(it, thr)
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

    override suspend fun skipTestMethod(testMethodContext: CoroutineContext, reason: String) {
        dispatch(testMethodContext) {
            skipTestMethod(it, reason)
            it
        }
    }

    override fun <T : Any> createTestClassInstance(testClass: KClass<T>): T {
        return testClassInstanceCreator?.createTestClassInstance(testClass)
                ?: testClass.createInstance()
    }


}