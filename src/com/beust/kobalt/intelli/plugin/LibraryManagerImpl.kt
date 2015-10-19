package com.beust.kobalt.intelli.plugin

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleComponent

class LibraryManagerImpl(val module: Module) : ModuleComponent {
    companion object {
        const val NAME = "Kobalt-LibraryManager"
    }

    init {
        println("Module: $module")
    }

    override fun getComponentName() = NAME

    override fun disposeComponent() {
    }

    override fun initComponent() {
    }

    override fun moduleAdded() {
    }

    override fun projectClosed() {
    }

    override fun projectOpened() {
    }
}
