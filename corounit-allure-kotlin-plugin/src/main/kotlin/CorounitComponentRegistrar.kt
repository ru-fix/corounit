package ru.fix.corounit.allure.kotlin.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(ComponentRegistrar::class)
class CorounitComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
            project: MockProject,
            configuration: CompilerConfiguration
    ) {

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val allureAnnotation = configuration.get(CorounitCommandLineProcessor.ARG_ALLURE_ANNOTATION, "invalid.default.Step")

//        Files.writeString(Paths.get("preprocessing-corounit"),
//                "register",
//        StandardOpenOption.APPEND, StandardOpenOption.CREATE)

        IrGenerationExtension.registerExtension(project, CorounitIrGenerationExtension(messageCollector, allureAnnotation))
    }
}

