package ru.fix.corounit.allure.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.util.*

class CorounitAllureGradlePlugin : KotlinCompilerPluginSupportPlugin {

    val pluginVersion = javaClass.getResourceAsStream("/corounit-allure-gradle-plugin.properties").use { stream ->
        Properties().apply { load(stream) }["ru.fix.corounit.allure.gradle.plugin.version"] as String
    }

    override fun apply(project: Project) {
        project.extensions.create(
                "corounitAllure",
                CorounitAllureExtension::class.java
        )
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val extension = kotlinCompilation.target.project.extensions.findByType(CorounitAllureExtension::class.java)
                ?: CorounitAllureExtension()

        if (extension.annotations.isEmpty()) {
            throw IllegalArgumentException("CorounitAllure requires at least one annotation to process.")
        }

        val annotationOptions = extension.annotations.map { SubpluginOption(key = "corounitAllureAnnotation", value = it) }
//        val enabledOption = SubpluginOption(key = "enabled", value = extension.enabled.toString())
        return kotlinCompilation.target.project.provider { annotationOptions }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.target.project.plugins.hasPlugin(CorounitAllureGradlePlugin::class.java)
    }


    /**
     * Just needs to be consistent with the key for DebugLogCommandLineProcessor#pluginId
     */
    override fun getCompilerPluginId(): String = "corounitAllure"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
            groupId = "ru.fix",
            artifactId = "corounit-allure-kotlin-plugin",
            version = pluginVersion
    )
}
