package com.beust.kobalt.intellij

import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFileManager

class Dependencies {
    companion object {
//        fun addToDependencies(project: Project, dependencies: List<DependencyData>) {
//            val modules = ModuleManager.getInstance(project).modules.filter {
//                it.name != KobaltProjectComponent.BUILD_MODULE_NAME
//            }
//            val registrar = LibraryTablesRegistrar.getInstance()
//            val libraryTable = registrar.getLibraryTable(project)
//
//            with(ApplicationManager.getApplication()) {
//                invokeLater {
//                    com.intellij.openapi.application.runWriteAction {
//                        deleteLibrariesAndContentEntries(modules, libraryTable)
//                        addDependencies(modules, dependencies, libraryTable)
//                    }
//                }
//            }
//        }
//
//        private fun addDependencies(modules: List<Module>, dependencies: List<DependencyData>,
//                libraryTable: LibraryTable) {
//            //
//            // Finally, add all the dependencies received from the server
//            //
//            val byScope = ArrayListMultimap.create<String, DependencyData>()
//            dependencies.forEach {
//                byScope.put(it.scope, it)
//            }
//
//            modules.forEach { module ->
//                //
//                // Add each dependency per scope
//                //
//                byScope.keySet().forEach { scope ->
//                    // Add the library as dependencies to the project
//                    // TODO: Do this to all models? How do we map IDEA modules to Kobalt projects?
//                    val scopedDependencies = byScope.get(scope)
//                    if (scopedDependencies.size > 0) {
//                        val libraryName = "kobalt (${toScope(scope)})"
//                        val library = createLibrary(libraryTable, scopedDependencies, libraryName)
//                        val modifiableModel = ModuleRootManager.getInstance(module).modifiableModel
//                        addLibrary(library, module, scope, modifiableModel)
//                        modifiableModel.commit()
//                    }
//                }
//            }
//        }

        /**
         * Delete all the Kobalt libraries and their ContentEntries.
         */
//        private fun deleteLibrariesAndContentEntries(modules: List<Module>, libraryTable: LibraryTable) {
//            fun isKobalt(name: String) = name.toLowerCase().startsWith("kobalt")
//                    && name.toLowerCase() != KobaltProjectComponent.KOBALT_JAR
//
//            //
//            // Delete all the kobalt libraries
//            //
//            libraryTable.modifiableModel.libraries.filter {
//                isKobalt(it.name!!)
//            }.map {
//                val library = libraryTable.getLibraryByName(it.name!!)
//                if (library != null) {
//                    libraryTable.removeLibrary(library)
//                } else {
//                    KobaltProjectComponent.LOG.error("Couldn't find library: " + it.name!!)
//                }
//            }
//            // ... and their OrderEntries
//            modules.forEach { module ->
//                with (ModuleRootManager.getInstance(module).modifiableModel) {
//                    orderEntries.forEach { ce ->
//                        if (isKobalt(ce.presentableName)) {
//                            removeOrderEntry(ce)
//                        }
//                    }
//                    commit()
//                }
//            }
//        }

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

        fun addLibrary(library: Library?, scope: String, modifiableModel: ModifiableRootModel) {
            if (library != null) {
                // Add the library to the module
                val existing = modifiableModel.findLibraryOrderEntry(library)
                if (existing == null) {
                    modifiableModel.addLibraryEntry(library)
                }

                // Update the scope for the library
                modifiableModel.findLibraryOrderEntry(library)?.let {
                    it.scope = toScope(scope)
                }
            } else {
                error("Couldn't create library for scope $scope")
            }
        }

        private fun toScope(scope: String) =
                when (scope) {
                    "provided" -> DependencyScope.PROVIDED
                    "runtime" -> DependencyScope.RUNTIME
                    "testCompile" -> DependencyScope.TEST
                    else -> DependencyScope.COMPILE
                }
    }
}
