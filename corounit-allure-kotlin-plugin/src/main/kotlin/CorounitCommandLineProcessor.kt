package ru.fix.corounit.allure.kotlin.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@AutoService(CommandLineProcessor::class)
class CorounitCommandLineProcessor : CommandLineProcessor {
    companion object {
        private const val OPTION_ALLURE_ANNOTATION = "corounitAllureAnnotation"
        val ARG_ALLURE_ANNOTATION = CompilerConfigurationKey<String>(OPTION_ALLURE_ANNOTATION)
    }

    /**
     * Just needs to be consistent with the key for DebugLogGradleSubplugin#getCompilerPluginId
     */
    override val pluginId: String = "corounitAllure"

    /**
     * Should match up with the options we return from our gradle plugin.
     * Should also have matching when branches for each name in the [processOption] function below
     */
    override val pluginOptions: Collection<CliOption> = listOf(
            CliOption(
                    optionName = "corounitAllureAnnotation", valueDescription = "<fqname>",
                    description = "fully qualified name of the allure step annotation",
                    required = true,
                    allowMultipleOccurrences = false
            )
    )

    override fun processOption(
            option: AbstractCliOption,
            value: String,
            configuration: CompilerConfiguration
    ) = when (option.optionName) {
        OPTION_ALLURE_ANNOTATION -> configuration.put(ARG_ALLURE_ANNOTATION, value)
        else -> error("Unexpected config option ${option.optionName}")
    }
}
