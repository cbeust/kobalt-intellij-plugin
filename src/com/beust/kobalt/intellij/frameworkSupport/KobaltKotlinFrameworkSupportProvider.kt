package com.beust.kobalt.intellij.frameworkSupport

import com.intellij.framework.FrameworkTypeEx
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import icons.OtherIcons

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
            addSourceDirectory("""path("src/main/kotlin")""")
            addSourceTestDirectory("""path("src/test/kotlin")""")
            addDependencyTest("""compile("org.testng:testng:6.9.9")""")
        }
    }
}