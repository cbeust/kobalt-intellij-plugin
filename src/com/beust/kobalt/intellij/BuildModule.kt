package com.beust.kobalt.intellij

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Path

/**
 * Add a module "Build.kt" that enables auto completion of the build file.
 */
class BuildModule {
    companion object {
        val LOG = Logger.getInstance(BuildModule::class.java)
    }

    fun run(project: Project, kobaltJar: Path) = maybeAddBuildModule(project, kobaltJar)

    private fun maybeAddBuildModule(project: Project, kobaltJar: Path) {
        if (BuildUtils.buildFileExist(project)) {
            doRefreshBuildModule(kobaltJar, project)
        } else {
            KobaltProjectComponent.LOG.info("Couldn't find kobalt/src/Build.kt, not creating the Build.kt module")
        }
    }

    /**
     * Add a new "build" module that enables auto completion on Build.kt.
     */
    private fun doRefreshBuildModule(kobaltJar: Path, project: Project) {
        val registrar = LibraryTablesRegistrar.getInstance()
        val libraryTable = registrar.getLibraryTable(project)

        ModuleManager.getInstance(project).let { moduleManager ->
                runWriteAction {
                    updateBuildModule(kobaltJar, libraryTable, moduleManager, project)
                }

        }
    }

    private fun updateBuildModule(kobaltJar: Path, libraryTable: LibraryTable, moduleManager: ModuleManager, project: Project) {
        // Find or create the Build.kt module
        val module = moduleManager.findModuleByName(KobaltProjectComponent.BUILD_MODULE_NAME)
                ?:moduleManager.newModule(project.baseDir.path
                + "/kobalt/${KobaltProjectComponent.BUILD_IML_NAME}", StdModuleTypes.JAVA.id)
        ModuleRootManager.getInstance(module).modifiableModel.let { modifiableModel ->
            //
            // Add the root content entry
            //
            val kobaltDir = VirtualFileManager.getInstance().findFileByUrl(project.baseDir.url)
                    ?.findChild("kobalt")
            if (kobaltDir != null) {
                modifiableModel.addContentEntry(kobaltDir).apply {
                    kobaltDir.findChild("src")?.let {
                        addSourceFolder(it, false)
                    }
                }
                modifiableModel.sdk = ProjectRootManager.getInstance(project).projectSdk;

                //
                // Add kobalt.jar
                //
                val kobaltLibrary = refreshKobaltLibrary(libraryTable,
                        kobaltJar, KobaltProjectComponent.KOBALT_JAR)
                Disposer.register(module, kobaltLibrary as Disposable)
                refreshBuildModuleContent(kobaltLibrary, modifiableModel)
                modifiableModel.commit()
            } else {
                Modules.lm("Couldn't find kobalt/src, autocomplete disabled")
                modifiableModel.dispose()
            }
        }
    }

    private fun refreshKobaltLibrary(libraryTable: LibraryTable, kobaltJar: Path,
                             libraryName: String): Library? {
        var result: Library? = null
        libraryTable.modifiableModel.let { ltModel ->
            result = ltModel.getLibraryByName(libraryName) ?: ltModel.createLibrary(libraryName)
            result!!.modifiableModel.let { libModel ->
                    val location = kobaltJar.toFile().absolutePath
                    val url = VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, location) +
                            JarFileSystem.JAR_SEPARATOR
                    //remove old kobalt.jar
                    libModel.getUrls(OrderRootType.CLASSES).forEach {
                        libModel.removeRoot(it, OrderRootType.CLASSES)
                    }
                    libModel.addRoot(url, OrderRootType.CLASSES)

                libModel.commit()
            }
            ltModel.commit()
        }
        return result
    }

   private fun refreshBuildModuleContent(library: Library, modifiableModel: ModifiableRootModel) {
            // Add the library to the module
            val existing = modifiableModel.findLibraryOrderEntry(library)
            if (existing == null) {
                modifiableModel.addLibraryEntry(library)
            }
            // Update the scope for the library
            modifiableModel.findLibraryOrderEntry(library)?.let {
                it.scope = DependencyScope.COMPILE
            }
    }


}
