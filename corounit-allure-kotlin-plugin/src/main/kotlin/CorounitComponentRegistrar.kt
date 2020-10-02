package ru.fix.corounit.allure.kotlin.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.PreprocessedVirtualFileFactoryExtension
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@AutoService(ComponentRegistrar::class)
class CorounitComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
            project: MockProject,
            configuration: CompilerConfiguration
    ) {
//    ClassBuilderInterceptorExtension.registerExtension(
//        project,
//        ClassGenerationInterceptor(
//            annotations = configuration[KEY_ANNOTATIONS]
//                ?: error("CorounitAllure plugin requires at least one annotation class option passed to it")
//        )
//    )

        Files.writeString(Paths.get("preprocessing-corounit"),
                "register",
        StandardOpenOption.APPEND, StandardOpenOption.CREATE)


        PreprocessedVirtualFileFactoryExtension.registerExtension(
                project,
                CorounitPreprocessedVirtualFileFactoryExtension()
                )
        // TODO: IrGenerationExtension.registerExtension for Kotlin Native :)
    }
}

