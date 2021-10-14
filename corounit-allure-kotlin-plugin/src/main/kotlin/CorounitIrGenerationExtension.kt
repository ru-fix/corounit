package ru.fix.corounit.allure.kotlin.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import java.io.File

class CorounitIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val allureAnnotation: String
) : IrGenerationExtension {


    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'annotation' = $allureAnnotation")

        for (file in moduleFragment.files) {
            val fileSource = File(file.path).readText()
                .replace("\r\n", "\n") // https://youtrack.jetbrains.com/issue/KT-41888

            CorounitTransformer(file, fileSource, pluginContext, messageCollector)
                .visitFile(file)
        }
    }

}