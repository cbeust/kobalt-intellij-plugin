package com.beust.kobalt.intelli.plugin

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

public class SyncBuildFileAction : AnAction("Sync build file") {

    val log = Logger.getInstance(SyncBuildFileAction::class.java)

    companion object {
        const val WRAPPER = "kobalt-wrapper.properties"
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { project ->
            readVersion(project)?.let {
                val kobaltJar = findKobaltJar(it)
                println("Kobalt jar: $kobaltJar")
            }
        }
    }

    private fun findKobaltJar(version: String) = Paths.get(System.getProperty("user.home"),
            ".kobalt/wrapper/dist/$version/kobalt/wrapper/kobalt-$version.jar")

    private fun readVersion(project: Project): String? {
        val scope = GlobalSearchScope.allScope(project)

        val wrappers = FilenameIndex.getFilesByName(project, WRAPPER, scope)
        if (wrappers.size() != 1) {
            log.error("Expected to find exactly one $WRAPPER, found ${wrappers.size()}")
            return null
        }

        val wrapper = wrappers[0]
        val content = wrapper.viewProvider.contents
        val properties = Properties()
        val ins = ByteArrayInputStream(content.toString().toByteArray(StandardCharsets.UTF_8))
        properties.load(ins)
        return properties.getProperty("kobalt.version", null)
    }


    private fun addToDependencies(project: Project) {
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