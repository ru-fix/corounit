package ru.fix.corounit.allure.kotlin.plugin

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.junit.jupiter.api.Test

class IrPluginTest {
    @Test
    fun `IR plugin success`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt", """
suspend fun main() {
  println( debug())
}
suspend fun debug() = "Hello, World!"
"""
            )
        )
        KotlinCompilation.ExitCode.OK shouldBe result.exitCode
    }
}

fun compile(
    sourceFiles: List<SourceFile>,
    plugin: ComponentRegistrar = CorounitComponentRegistrar(),
): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles
        useIR = true
        compilerPlugins = listOf(plugin)
        inheritClassPath = true
    }.compile()
}

fun compile(
    sourceFile: SourceFile,
    plugin: ComponentRegistrar = CorounitComponentRegistrar(),
): KotlinCompilation.Result {
    return compile(listOf(sourceFile), plugin)
}