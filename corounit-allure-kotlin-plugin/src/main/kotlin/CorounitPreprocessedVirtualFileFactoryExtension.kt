package ru.fix.corounit.allure.kotlin.plugin

import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.extensions.PreprocessedVirtualFileFactoryExtension
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.math.min

class CorounitPreprocessedVirtualFileFactoryExtension : PreprocessedVirtualFileFactoryExtension {

    override fun createPreprocessedFile(file: VirtualFile?): VirtualFile? {
        Files.writeString(Paths.get("preprocessing-corounit"),
                "VF:" +
                        file?.contentsToByteArray()?.decodeToString()?.let { it.substring(0..min(30, it.length)) },
                StandardOpenOption.APPEND, StandardOpenOption.CREATE)

        return file
    }

    override fun createPreprocessedLightFile(file: LightVirtualFile?): LightVirtualFile? {
        Files.writeString(Paths.get("preprocessing-corounit"),
                "LVF:" +
                        file?.content?.let { it.substring(0..min(30, it.length)) },
                StandardOpenOption.APPEND, StandardOpenOption.CREATE)

        return file
    }

    override fun isPassThrough(): Boolean {
        return false
    }

}