package com.beust.kobalt.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFileManager

class Modules {
    companion object {
        val LOG = Logger.getInstance(Modules::class.java)


        fun configureModules(project: Project, projects: List<ProjectData>) = projects.forEach {
            configureModule(project, it)
        }

        private fun configureModule(project: Project, kp: ProjectData) {
            ModuleManager.getInstance(project).let { moduleManager ->
                // Delete the module if it already exists
                moduleManager.findModuleByName(kp.name)?.let {
                    ApplicationManager.getApplication().invokeLater {
                        runWriteAction {
                            with(moduleManager.modifiableModel) {
                                disposeModule(it)
                                commit()
                            }
                        }
                    }
                }

                // Create the libraries
                val libraryMap = hashMapOf<String, Library>()
                ApplicationManager.getApplication().invokeLater {
                    runWriteAction {
                        LOG.warn("Creating module " + kp.directory)
                        val moduleDir = project.basePath + "/" + kp.directory + "/"
                        val module = moduleManager.newModule(moduleDir + kp.name
                                + ".iml",
                                StdModuleTypes.JAVA.id)
                        ModuleRootManager.getInstance(module).modifiableModel.let { modifiableModel ->
                            val registrar = LibraryTablesRegistrar.getInstance()
                            val libraryTable = registrar.getLibraryTable(project)
                            //
                            // Libraries
                            //
                            kp.dependencies.forEach { dd ->
                                LOG.warn("Adding library " + dd.id)
                                val library = createLibrary(libraryTable, dd, dd.id)
                                libraryMap.put(dd.id, library!!)
                                modifiableModel.addLibraryEntry(library)

                                val entry = modifiableModel.findLibraryOrderEntry(library)
                                if (entry != null) {
                                    entry.scope = toScope(dd.scope)
                                }
                            }

                            //
                            // Content roots and source dirs
                            //
                            val fullUrl = project.baseDir.url + "/" + kp.directory
                            LOG.warn("URL: " + project.baseDir.url + " FULL URL: $fullUrl")
                            val contentRoot = VirtualFileManager.getInstance().findFileByUrl(fullUrl)
                            LOG.warn("srcDir: $contentRoot")
                            if (contentRoot != null) {
                                val contentEntry = modifiableModel.addContentEntry(contentRoot)
                                LOG.warn("ADDED CONTENT ROOT $contentRoot")

                                fun addSourceDir(dir: String, isTest: Boolean) {
                                    val srcDir = contentRoot.findFileByRelativePath(dir.replace("\\", "/"))
                                    if (srcDir != null) {
                                        LOG.warn("FOUND PROJECT SRC: $srcDir")
                                        contentEntry.addSourceFolder(srcDir, isTest)
                                    }

                                }
                                kp.sourceDirs.forEach { addSourceDir(it, false) }
                                kp.testDirs.forEach { addSourceDir(it, true) }

//                                val srcDir = contentRoot.findChild("src")?.findChild("main")?.findChild("java")
//                                LOG.warn("SRC DIR: $srcDir")
//                                if (srcDir != null) {
//                                    contentEntry.addSourceFolder(srcDir, false)
//                                }
                            }

                            //
                            // SDK
                            //
//                            ProjectRootManager.getInstance(project).projectSdk?.let {
//                                modifiableModel.addContentEntry(it.homeDirectory!!)
//                            }

                            modifiableModel.commit()
                        }
                    }
                }

                // Add src and dependencies to the module
            }
        }

        private fun toScope(scope: String) =
                when (scope) {
                    "provided" -> DependencyScope.PROVIDED
                    "runtime" -> DependencyScope.RUNTIME
                    "testCompile" -> DependencyScope.TEST
                    else -> DependencyScope.COMPILE
                }

        private fun createLibrary(libraryTable: LibraryTable, dependency: DependencyData,
                libraryName: String): Library? {
            var result: Library? = null
            libraryTable.modifiableModel.let { ltModel ->
                // Delete the old library if there's one
                ltModel.getLibraryByName(libraryName)?.let {
                    KobaltProjectComponent.LOG.info("Removing existing library $it")
                    ltModel.removeLibrary(it)
                }

                // Create the library
                result = ltModel.createLibrary(libraryName)
                result!!.modifiableModel.let { libModel ->
//                    dependencies.forEach { dependency ->
                        val location = dependency.path
                        val url = VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, location) +
                                JarFileSystem.JAR_SEPARATOR
                        libModel.addRoot(url, OrderRootType.CLASSES)

//                    }
                    libModel.commit()
                }
                ltModel.commit()
            }
            return result
        }
    }
}