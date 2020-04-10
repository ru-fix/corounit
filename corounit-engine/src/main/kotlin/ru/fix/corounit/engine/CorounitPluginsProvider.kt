package ru.fix.corounit.engine

/**
 * Create object named "CorounitConfig" near your test source.
 * Implement [CorounitPluginsProvider] interface and define several corounit plugins
 * ```
 * object CorounitConfig : CorounitPluginsProvider{
 *   override fun plugins(): List<CorounitPlugin> {
 *     return listOf(object: CorounitPlugin{
 *       override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
 *         //initialization logic
 *         return super.beforeAllTestClasses(globalContext)
 *       }
 *     })
 *   }
 * }
 * ```
 * Or you can make object named "CorounitConfig" implement [CorounitPlugin] interface instead.
 */
interface CorounitPluginsProvider {
    fun plugins(): List<CorounitPlugin> = emptyList()
}