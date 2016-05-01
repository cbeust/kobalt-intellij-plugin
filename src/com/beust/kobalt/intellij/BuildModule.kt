package com.beust.kobalt.intellij

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.Disposer
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
            addBuildModule(kobaltJar, project)
        } else {
            KobaltProjectComponent.LOG.info("Couldn't find kobalt/src/Build.kt, not creating the Build.kt module")
        }
    }

    /**
     * Add a new "build" module that enables auto completion on Build.kt.
     */
    private fun addBuildModule(kobaltJar: Path, project: Project) {
        val registrar = LibraryTablesRegistrar.getInstance()
        val libraryTable = registrar.getLibraryTable(project)

        ModuleManager.getInstance(project).let { moduleManager ->
            // Delete the module if it already exists
            moduleManager.findModuleByName(KobaltProjectComponent.BUILD_MODULE_NAME)?.let {
                runWriteAction {
                    with(moduleManager.modifiableModel) {
                        Modules.lm("Deleting module " + KobaltProjectComponent.BUILD_MODULE_NAME)
                        disposeModule(it)
                        commit()
                    }
                }
            }

            // Create the module
            runWriteAction {
                val module = moduleManager.newModule(project.baseDir.path
                        + "/kobalt/${KobaltProjectComponent.BUILD_IML_NAME}", StdModuleTypes.JAVA.id)
                ModuleRootManager.getInstance(module).modifiableModel.let { modifiableModel ->
                    //
                    // Add the root content entry
                    //
                    val kobaltDir = VirtualFileManager.getInstance().findFileByUrl(project.baseDir.url)
                            ?.findChild("kobalt")
                    if (kobaltDir != null) {
                        // Setting the content root to the "kobalt" directory will automatically add "src"
                        // as a source folder
                        modifiableModel.addContentEntry(kobaltDir)

//                        val sdk = ProjectRootManager.getInstance(project).projectSdk
//                        modifiableModel.addContentEntry(sdk!!.homeDirectory!!)

                        //
                        // Add kobalt.jar
                        //
                        val kobaltDependency = DependencyData("", "compile", kobaltJar.toFile().absolutePath)
                        val kobaltLibrary = Dependencies.createLibrary(libraryTable,
                                arrayListOf(kobaltDependency), KobaltProjectComponent.KOBALT_JAR)
                        Disposer.register(module, kobaltLibrary as Disposable)
                        Dependencies.addLibrary(kobaltLibrary, "compile", modifiableModel)
                        modifiableModel.commit()
                    } else {
                        Modules.lm("Couldn't find kobalt/src, autocomplete disabled")
                        modifiableModel.dispose()
                    }
                }
            }
        }
    }

}
