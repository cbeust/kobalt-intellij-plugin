package com.beust.kobalt.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
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
                            LOG.warn("0 Existing roots: " + modifiableModel.contentRoots.firstOrNull()?.canonicalPath)

                            modifiableModel.contentEntries.forEach {
                                modifiableModel.removeContentEntry(it)
                            }

                            val registrar = LibraryTablesRegistrar.getInstance()
                            val libraryTable = registrar.getLibraryTable(project)

                            //
                            // Libraries
                            //
                            fun initLibrary(dependencies: List<DependencyData>, name: String, scope: DependencyScope) {
                                val library = createLibrary(libraryTable, dependencies, name)
                                if (library != null) {
                                    modifiableModel.findLibraryOrderEntry(library)?.let {
                                        modifiableModel.removeOrderEntry(it)
                                    }
                                    modifiableModel.addLibraryEntry(library)
                                    modifiableModel.findLibraryOrderEntry(library)?.scope = scope
                                }
                            }
                            initLibrary(kp.compileDependencies, "${module.name} (Compile)", DependencyScope.COMPILE)
                            initLibrary(kp.testDependencies, "${module.name} (Test)", DependencyScope.TEST)

                            //
                            // Content roots and source dirs
                            //
                            val fullUrl = project.baseDir.url + "/" + kp.directory
                            val contentRoot = VirtualFileManager.getInstance().findFileByUrl(fullUrl)
                            LOG.warn("Existing roots: " + modifiableModel.contentRoots.firstOrNull()?.canonicalPath)
                            if (contentRoot != null) {
                                LOG.warn("Found contentRoot: $contentRoot")
                                val contentEntry = modifiableModel.addContentEntry(contentRoot)
                                fun addSourceDir(dir: String, isTest: Boolean) {
                                    val srcDir = contentRoot.findFileByRelativePath(dir.replace("\\", "/"))
                                    if (srcDir != null) {
                                        contentEntry.addSourceFolder(srcDir, isTest)
                                    }

                                }
                                kp.sourceDirs.forEach { addSourceDir(it, false) }
                                kp.testDirs.forEach { addSourceDir(it, true) }
                            }

                            //
                            // SDK
                            //
                            LOG.warn("ALL JDK's: " + ProjectJdkTable.getInstance().allJdks)
                            LOG.warn("Project sdk: " + ProjectRootManager.getInstance(project).projectSdk?.name)
                            if (ProjectRootManager.getInstance(project).projectSdk == null &&
                                    ProjectJdkTable.getInstance().allJdks.size > 0) {
                                LOG.warn("Setting SDK to " + ProjectJdkTable.getInstance().allJdks[0].name)
                                ProjectRootManager.getInstance(project).projectSdk =
                                    ProjectJdkTable.getInstance().allJdks[0]
                            }
                            ProjectRootManager.getInstance(project).projectSdk = ProjectJdkTable.getInstance().allJdks[0]

                            modifiableModel.commit()
                        }
                    }
                }

                // Add src and dependencies to the module
            }
        }

        fun createLibrary(libraryTable: LibraryTable, dependencies: List<DependencyData>,
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
                    dependencies.forEach { dependency ->
                        val location = dependency.path
                        val url = VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, location) +
                                JarFileSystem.JAR_SEPARATOR
                        libModel.addRoot(url, OrderRootType.CLASSES)

                    }
                    libModel.commit()
                }
                ltModel.commit()
            }
            return result
        }
    }
}