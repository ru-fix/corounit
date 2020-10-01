package ru.fix.corounit.allure.kotlin.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(ComponentRegistrar::class)
class ComponentRegistrar : ComponentRegistrar {
  override fun registerProjectComponents(
      project: MockProject,
      configuration: CompilerConfiguration
  ) {
    ClassBuilderInterceptorExtension.registerExtension(
        project,
        ClassGenerationInterceptor(
            annotations = configuration[KEY_ANNOTATIONS]
                ?: error("CorounitAllure plugin requires at least one annotation class option passed to it")
        )
    )
    // TODO: IrGenerationExtension.registerExtension for Kotlin Native :)
  }
}

