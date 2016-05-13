package com.beust.kobalt.intellij.frameworkSupport

import com.intellij.framework.FrameworkTypeEx
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtilRt
import icons.OtherIcons
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date:  13.05.2016
 */
class KobaltKotlinFrameworkSupportProvider : KobaltFrameworkSupportProvider() {
    companion object {
        @JvmField val ID = "kotlin"
    }

    override fun getFrameworkType() = object : FrameworkTypeEx(ID) {
        override fun getIcon() = OtherIcons.Kotlin

        override fun getPresentableName() = "Kotlin (Java)"

        override fun createProvider() = this@KobaltKotlinFrameworkSupportProvider

    }


    override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider, buildScriptBuilder: KobaltBuildScriptBuilder) {
        with(buildScriptBuilder) {
            val kobaltModuleRootDir = File(contentRootDir, projectId.artifactId)
            val kobaltModuleSourceDir = File(kobaltModuleRootDir, "src/main/kotlin")
            val kobaltModuleTestDir = File(kobaltModuleRootDir, "src/test/kotlin")
            FileUtilRt.createDirectory(kobaltModuleSourceDir)
            FileUtilRt.createDirectory(kobaltModuleTestDir)
            addSourceDirectory("""path("src/main/kotlin")""")
            addSourceTestDirectory("""path("src/test/kotlin")""")
            addDependencyTest("""compile("org.testng:testng:6.9.9")""")
        }
    }
}