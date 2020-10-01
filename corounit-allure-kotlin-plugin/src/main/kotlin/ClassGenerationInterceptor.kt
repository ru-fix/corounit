package ru.fix.corounit.allure.kotlin.plugin

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

abstract class DelegatingClassBuilder(val delegatingClassBuilder: ClassBuilder) : org.jetbrains.kotlin.codegen.DelegatingClassBuilder() {
  override fun getDelegate(): ClassBuilder = delegatingClassBuilder
}

class ClassGenerationInterceptor(
    val annotations: List<String>
) : ClassBuilderInterceptorExtension {
  /**
   * Our [ClassBuilderFactory] has identical behavior to the [interceptedFactory] parameter given, but returns a
   * [DelegatingClassBuilder] that wraps any [ClassBuilder] returned by [ClassBuilderFactory.newClassBuilder]
   */
  override fun interceptClassBuilderFactory(
      interceptedFactory: ClassBuilderFactory,
      bindingContext: BindingContext,
      diagnostics: DiagnosticSink
  ): ClassBuilderFactory = object : ClassBuilderFactory by interceptedFactory {
    override fun newClassBuilder(origin: JvmDeclarationOrigin) =
        ClassBuilder(annotations, interceptedFactory.newClassBuilder(origin))
  }

}

