package com.beust.kobalt.intellij.frameworkSupport

import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * @author Dmitry Zhuravlev
 *         Date:  11.05.2016
 */
abstract class KobaltFrameworkSupportProvider : FrameworkSupportInModuleProvider(){
    companion object {
         @JvmField val EP_NAME = ExtensionPointName.create<KobaltFrameworkSupportProvider>("com.beust.kobalt.intellij.frameworkSupport")
    }
}