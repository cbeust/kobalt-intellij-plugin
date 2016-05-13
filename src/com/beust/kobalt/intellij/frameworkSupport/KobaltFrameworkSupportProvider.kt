package com.beust.kobalt.intellij.frameworkSupport

import com.beust.kobalt.intellij.project.wizard.KobaltModuleBuilder
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel

/**
 * @author Dmitry Zhuravlev
 *         Date:  11.05.2016
 */
abstract class KobaltFrameworkSupportProvider : FrameworkSupportInModuleProvider() {
    companion object {
        @JvmField val EP_NAME = ExtensionPointName.create<KobaltFrameworkSupportProvider>("com.beust.kobalt.intellij.frameworkSupport")
    }

    abstract fun addSupport(module: Module, rootModel: ModifiableRootModel,
                            modifiableModelsProvider: ModifiableModelsProvider,
                            buildScriptBuilder: KobaltBuildScriptBuilder)

    override fun createConfigurable(model: FrameworkSupportModel) = object : FrameworkSupportInModuleConfigurable() {
        override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider) {
            KobaltModuleBuilder.getBuildScriptBuilder(module)?.let { scriptBuilder ->
                this@KobaltFrameworkSupportProvider.addSupport(module, rootModel, modifiableModelsProvider, scriptBuilder)
            }
        }
        override fun createComponent() = null
    }

    override fun isEnabledForModuleType(moduleType: ModuleType<*>) = false
}