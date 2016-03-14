package com.beust.kobalt.intellij

import com.google.gson.GsonBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.jps.model.java.JavaResourceRootType

class Modules {
    companion object {
        val LOG = Logger.getInstance(Modules::class.java)


        fun configureModules(project: Project, projects: List<ProjectData>) = projects.forEach {
            ApplicationManager.getApplication().invokeLater {
                runWriteAction {
                    configureModule(project, it)
                }
            }
        }

        private fun configureModule(project: Project, kp: ProjectData) {
            if (Constants.DEV_MODE) {
                LOG.warn("Configuring modules with\n " + println(GsonBuilder().setPrettyPrinting().create().toJson(kp)))
            }
            ModuleManager.getInstance(project).let { moduleManager ->
                // Delete the module if it already exists
                LOG.warn("Creating module " + kp.directory)
                val moduleDir = project.basePath + "/" + kp.directory + "/"
                val module = moduleManager.findModuleByName(kp.name) ?:
                        moduleManager.newModule(moduleDir + kp.name
                                + ".iml",
                                StdModuleTypes.JAVA.id)
//                val prm = ProjectRootManager.getInstance(project)
                ModuleRootManager.getInstance(module).modifiableModel.let { modifiableModel ->
                    LOG.warn("0 Existing roots: " + modifiableModel.contentRoots.firstOrNull()?.canonicalPath)

                    modifiableModel.contentEntries.forEach {
                        modifiableModel.removeContentEntry(it)
                    }

                    val registrar = LibraryTablesRegistrar.getInstance()
                    val libraryTable = registrar.getLibraryTable(project)

                    //
                    // Output directory
                    //
                    val cpe = CompilerProjectExtension.getInstance(project)
                    if (cpe?.compilerOutputUrl.isNullOrEmpty()) {
                        cpe?.compilerOutputUrl = VfsUtilCore.pathToUrl(project.basePath + "/" + "out")
                    }

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
                        fun addSourceDir(dir: String, isTest: Boolean, isResource: Boolean) {
                            val srcDir = contentRoot.findFileByRelativePath(dir.replace("\\", "/"))
                            if (srcDir != null) {
                                if (isResource) {
                                    if (isTest) {
                                        contentEntry.addSourceFolder(srcDir, JavaResourceRootType.TEST_RESOURCE,
                                                JavaResourceRootType.RESOURCE.createDefaultProperties())
                                    } else {
                                        contentEntry.addSourceFolder(srcDir, JavaResourceRootType.RESOURCE,
                                                JavaResourceRootType.RESOURCE.createDefaultProperties())

                                    }
                                } else {
                                    contentEntry.addSourceFolder(srcDir, isTest)
                                }
                            }

                        }
                        kp.sourceDirs.forEach { addSourceDir(it, false, false) }
                        kp.testDirs.forEach { addSourceDir(it, true, false) }
                        kp.sourceResourceDirs.forEach { addSourceDir(it, false, true) }
                        kp.testResourceDirs.forEach { addSourceDir(it, true, true) }
                    }

                    //
                    // Dependent modules
                    //
                    modifiableModel.orderEntries.filter { it is ModuleOrderEntry }.forEach {
                        modifiableModel.removeOrderEntry(it)
                    }
                    kp.dependentProjects.forEach {
                        val dependentModule = moduleManager.findModuleByName(it)
                        if (dependentModule != null) {
                            modifiableModel.addModuleOrderEntry(dependentModule)
                        }
                    }

                    //
                    // SDK
                    //
                    modifiableModel.inheritSdk()

                    modifiableModel.commit()
                }
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