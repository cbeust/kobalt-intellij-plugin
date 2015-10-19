package com.beust.kobalt.intelli.plugin

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFileManager

public class SyncBuildFileAction : AnAction("Sync build file") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project

        val modules = ModuleManager.getInstance(project!!).modules
        if (modules.size() > 0) {
            val registrar = LibraryTablesRegistrar.getInstance()
            val libraryTable = registrar.getLibraryTable(project)

            ApplicationManager.getApplication().runWriteAction {
                val ltModel = libraryTable.modifiableModel
                val library = ltModel.createLibrary("JCommander")

                val location = "c:\\users\\cbeust\\.kobalt\\repository\\com\\beust\\jcommander\\1.48\\jcommander-1.48.jar"
                val url = VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, location) +
                        JarFileSystem.JAR_SEPARATOR
                val libModel = library.modifiableModel
                libModel.addRoot(url, OrderRootType.CLASSES)
                libModel.commit()
                ltModel.commit()

                val moduleRootManager = ModuleRootManager.getInstance(modules[0])
                val moduleModel = moduleRootManager.modifiableModel

                moduleModel.addLibraryEntry(library)
                moduleModel.commit()
            }
        }
    }
}