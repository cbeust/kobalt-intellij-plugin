package com.beust.kobalt.intelli.plugin;

import com.google.common.collect.ArrayListMultimap
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.*
import java.net.ConnectException
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors

class KobaltProjectComponent(val project: Project) : ProjectComponent {
    companion object {
        const val WRAPPER = "kobalt-wrapper.properties"
    }

    override fun getComponentName() = "KobaltProjectComponent"

    val executor = Executors.newFixedThreadPool(2)
    val port = 1234

    override fun initComponent() {
    }

    override fun disposeComponent() {
        executor.shutdown()
    }

    override fun projectOpened() {
    }

    override fun projectClosed() {
    }

    fun syncBuildFile() {
        println("SYNCING BUILD FILE FOR $project")

        readVersion(project)?.let { version ->
//                ProgressManager.getInstance().runProcess({
//                    launchServer(port, version, project.basePath!!)
//                }, null)
//                ProgressManager.getInstance().runProcess({
//
//                }, null)
            executor.submit {
                launchServer(port, version, project.basePath!!)
            }
            executor.submit {
                sendGetDependencies(port, project)
            }
//                executor.awaitTermination(30, TimeUnit.SECONDS)
//                executor.shutdown()
        }
    }

    private fun sendGetDependencies(port: Int, project: Project) {
        var attempts = 0
        var connected = false
        var socket: Socket? = null
        while (attempts < 5 && ! connected) {
            try {
                socket = Socket("localhost", port)
                connected = true
            } catch(ex: ConnectException) {
                logInfo("Server not started yet, sleeping a bit")
                Thread.sleep(2000)
                attempts++
            }
        }
        if (connected) {
            val outgoing = PrintWriter(socket!!.outputStream, true)
            ApplicationManager.getApplication().runReadAction {
                val buildFiles = FilenameIndex.getFilesByName(project, "Build.kt", GlobalSearchScope.allScope(project))
                buildFiles.forEach {
                    val buildFile = it.viewProvider.virtualFile.canonicalPath
                    val command: String = "{ \"name\":\"GetDependencies\", \"buildFile\": \"$buildFile\"}"

                    outgoing.println(command)

                    val ins = BufferedReader(InputStreamReader(socket!!.inputStream))
                    var line = ins.readLine()
                    var done = false
                    while (!done && line != null) {
                        logInfo("Received from server: " + line)
                        val jo = JsonParser().parse(line) as JsonObject
                        if (jo.has("name") && "Quit" == jo.get("name").asString) {
                            logInfo("Quitting")
                            done = true
                        } else {
                            val data = jo.get("data").asString
                            val dd = Gson().fromJson(data, GetDependenciesData::class.java)

                            logInfo("Read GetDependencyData, project count: ${dd.projects.size()}")

                            dd.projects.forEach { kobaltProject ->
                                addToDependencies(project, kobaltProject.dependencies)
                            }
                            line = ins.readLine()
                        }
                    }
                }
            }

            outgoing.println(QUIT_COMMAND)
        } else {
            logError("Couldn't connect to server")
        }

    }

    private val QUIT_COMMAND = "{ \"name\" : \"Quit\" }"

    private fun launchServer(port: Int, version: String, directory: String) {
        val kobaltJar = findKobaltJar(version)
        logInfo("Kobalt jar: $kobaltJar")
        val args = arrayListOf("java", "-jar", kobaltJar.toFile().absolutePath, "--server", "--port", port.toString())
        val pb = ProcessBuilder(args)
        pb.directory(File(directory))
        pb.inheritIO()
        pb.environment().put("JAVA_HOME", ProjectJdkTable.getInstance().allJdks[0].homePath)
        logInfo("Launching " + args.join(" "))
        val process = pb.start()
        val errorCode = process.waitFor()
        if (errorCode == 0) {
            logInfo("Server exiting")
        } else {
            logInfo("Server exiting with error")
        }
    }

    private val DEV_MODE = true

    private fun findKobaltJar(version: String) =
            if (DEV_MODE) {
                Paths.get(System.getProperty("user.home"), "kotlin/kobalt/kobaltBuild/libs/kobalt-0.194.jar")
            } else {
                Paths.get(System.getProperty("user.home"),
                        ".kobalt/wrapper/dist/$version/kobalt/wrapper/kobalt-$version.jar")
            }

    private fun readVersion(project: Project): String? {
        val scope = GlobalSearchScope.allScope(project)

        val wrappers = FilenameIndex.getFilesByName(project, WRAPPER, scope)
        if (wrappers.size() != 1) {
            logError("Expected to find exactly one $WRAPPER, found ${wrappers.size()}")
            return null
        }

        val wrapper = wrappers[0]
        val content = wrapper.viewProvider.contents
        val properties = Properties()
        val ins = ByteArrayInputStream(content.toString().toByteArray(StandardCharsets.UTF_8))
        properties.load(ins)
        val result = properties.getProperty("kobalt.version", null)
        if (result != null) {
            val MIN = 0.194
            if (java.lang.Float.parseFloat(result) < MIN) {
                Messages.showMessageDialog(project,
                        "You need Kobalt version $MIN or above, please update your kobalt-wrapper.properties file" +
                                " to the latest version",
                        "Can't synchronize build file",
                        Messages.getInformationIcon())
                return null
            }
        }
        return result
    }

    /**
     * Add the dependencies received from the server to the IDEA project.
     */
    private fun addToDependencies(project: Project, dependencies: List<DependencyData>) {
        val modules = ModuleManager.getInstance(project).modules
        if (modules.size() > 0) {

            val byScope = ArrayListMultimap.create<String, DependencyData>()
            dependencies.forEach {
                byScope.put(it.scope, it)
            }

            byScope.keySet().forEach { scope ->
                // Add the library as dependencies to the project
                // TODO: Do this to all models? How do we map IDEA modules to Kobalt projects?
                val scopedDependencies = byScope.get(scope)
                if (scopedDependencies.size() > 0) {
                    addToDependencies(modules[0], project, scopedDependencies, scope)
                }
            }
        }
    }

    private fun addToDependencies(module: Module, project: Project, dependencies: List<DependencyData>, scope: String) {
        val registrar = LibraryTablesRegistrar.getInstance()
        val libraryTable = registrar.getLibraryTable(project)

        with(ApplicationManager.getApplication()) {
            invokeLater {
                runWriteAction {
                    val library = createLibrary(libraryTable, dependencies, scope)

                    if (library != null) {
                        // Add the library to the module
                        val moduleRootManager = ModuleRootManager.getInstance(module)
                        moduleRootManager.modifiableModel.let { moduleModel ->
                            val existing = moduleModel.findLibraryOrderEntry(library)
                            if (existing == null) {
                                moduleModel.addLibraryEntry(library)
                            }
                            moduleModel.commit()
                        }

                        // Update the scope for the library
                        moduleRootManager.modifiableModel.let { moduleModel ->
                            moduleModel.findLibraryOrderEntry(library)?.let {
                                it.scope = toScope(scope)
                            }
                            moduleModel.commit()
                        }
                    } else {
                        error("Couldn't create library for scope $scope")
                    }
                }
            }
        }
    }

    private fun createLibrary(libraryTable: LibraryTable, dependencies: List<DependencyData>, scope: String): Library? {
        var result: Library? = null
        val LIBRARY_NAME = "Kobalt (${toScope(scope)})"
        libraryTable.modifiableModel.let { ltModel ->
            // Delete the old library if there's one
            ltModel.getLibraryByName(LIBRARY_NAME)?.let {
                logInfo("Removing existing library $it")
                ltModel.removeLibrary(it)
            }

            // Create the library
            result = ltModel.createLibrary(LIBRARY_NAME)
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

    private fun toScope(scope: String) =
            when(scope) {
                "provided" -> DependencyScope.PROVIDED
                "runtime" -> DependencyScope.RUNTIME
                "testCompile" -> DependencyScope.TEST
                else -> DependencyScope.COMPILE
            }

    private fun logError(s: String) {
        println("[KobaltProjectComponent] ***** ERROR: $s")
    }

    private fun logInfo(s: String) {
        println("[KobaltProjectComponent] $s")
    }

}
