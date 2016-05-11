package com.beust.kobalt.intellij.frameworkSupport

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.icons.AllIcons
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel

/**
 * @author Dmitry Zhuravlev
 *         Date:  11.05.2016
 */
class KobaltJavaFrameworkSupportProvider : KobaltFrameworkSupportProvider() {
    companion object {
       @JvmField val ID = "java"
    }

    override fun getFrameworkType() = object : FrameworkTypeEx(ID) {
        override fun getIcon() = AllIcons.Nodes.Module

        override fun getPresentableName() = "Java"

        override fun createProvider() = this@KobaltJavaFrameworkSupportProvider

    }

    override fun isEnabledForModuleType(moduleType: ModuleType<*>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun createConfigurable(model: FrameworkSupportModel) = object : FrameworkSupportInModuleConfigurable() {
        override fun addSupport(module: Module, rootModel: ModifiableRootModel, modifiableModelsProvider: ModifiableModelsProvider) {
           //TODO
        }

        override fun createComponent() = null

    }
}