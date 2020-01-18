package ru.fix.corounit.engine

import mu.KotlinLogging
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

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
                .mapNotNull {
                    it.kotlin.objectInstance as? CorounitPlugin
                }

        plugins = byServiceLoader + byConfigObjects
        testClassInstanceCreator = plugins.find { it.abilities().contains(CorounitPlugin.Ability.CREATE_INSTANCE) }
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

    override suspend fun beforeAll(globalContext: CoroutineContext): CoroutineContext {
        return dispatch(globalContext) { beforeAll(it) }
    }

    override suspend fun afterAll(globalContext: CoroutineContext) {
        dispatch(globalContext) {
            afterAll(it)
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

    override suspend fun beforeTestMethod(testMethodContext: CoroutineContext): CoroutineContext {
        return dispatch(testMethodContext) { beforeTestMethod(it) }
    }

    override suspend fun afterTestMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
        dispatch(testMethodContext) {
            afterTestMethod(it, thr)
            it
        }
    }

    override fun <T : Any> createTestClassInstance(testClass: KClass<T>): T {
        return testClassInstanceCreator?.createTestClassInstance(testClass)
                ?: super.createTestClassInstance(testClass)
    }


}