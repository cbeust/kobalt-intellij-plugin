package com.beust.kobalt.intellij;

import com.google.common.collect.ArrayListMultimap
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.StatusBarProgress
import com.intellij.openapi.project.Project
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
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Invoked from the "Sync build file" action: launch a kobalt --server in the background, connect to it
 * and send it a getDependencies() command for the current project. When the answer is received, update
 * the project's libraries and dependencies with that information.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @since 10 23, 2015
 */
class KobaltProjectComponent(val project: Project) : ProjectComponent {
    companion object {
        val LOG = Logger.getInstance(KobaltProjectComponent::class.java)

    }

    override fun getComponentName() = "kobalt.ProjectComponent"
    override fun initComponent() {}
    override fun disposeComponent() {}
    override fun projectOpened() {}
    override fun projectClosed() {}

    var progress = StatusBarProgress()

    fun syncBuildFile() {
        LOG.info("Syncing build file for project $project")

        val version = KobaltApplicationComponent.version!!

        val kobaltJar = findKobaltJar(version)
        with(ProgressManager.getInstance()) {
            val port = findPort()
//            if (! Constants.DEV_MODE) {
                runProcessWithProgressAsynchronously(
                        toBackgroundTask("Kobalt: Launch server", {
                            launchServer(port, version, project.basePath!!, kobaltJar)
                        }), EmptyProgressIndicator())
//            }

            runProcessWithProgressAsynchronously(
                    toBackgroundTask("Kobalt: Get dependencies", {
                        sendGetDependencies(port, project, kobaltJar)
                    }), progress)
        }
    }

    private fun toBackgroundTask(title: String, function: Function0<Unit>): Task.Backgroundable {
        return object: Task.Backgroundable(project, title) {
            override fun run(p0: ProgressIndicator) {
                function.invoke()
            }
        }
    }

    private fun sendGetDependencies(port: Int, project: Project, kobaltJar: Path) {
        logInfo("sendGetDependencies")

        //
        // Display the notification
        //
        val notificationText = "Synchronizing Kobalt build file..."
        progress.text = notificationText
        progress.fraction = 0.25
        val group = NotificationGroup.logOnlyGroup("Kobalt")
        group.createNotification(notificationText, NotificationType.INFORMATION).notify(project)

        progress.fraction = 0.50

        //
        // Connect to the server
        //
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

        progress.fraction = 0.75

        //
        // Send the "getDependencies" command to the server
        //
        if (connected) {
            val outgoing = PrintWriter(socket!!.outputStream, true)
            ApplicationManager.getApplication().runReadAction {
                val buildFiles = FilenameIndex.getFilesByName(project, "Build.kt", GlobalSearchScope.allScope(project))
                buildFiles.forEach {
                    val buildFile = it.viewProvider.virtualFile.canonicalPath
                    val command: String = "{ \"name\":\"getDependencies\", \"buildFile\": \"$buildFile\"}"

                    outgoing.println(command)

                    val ins = BufferedReader(InputStreamReader(socket!!.inputStream))
                    var line = ins.readLine()
                    var done = false
                    while (!done && line != null) {
                        logInfo("Received from server: " + line)
                        val jo = JsonParser().parse(line) as JsonObject
                        if (jo.has("name") && "quit" == jo.get("name").asString) {
                            logInfo("Quitting")
                            done = true
                        } else {
                            val error = jo.get("error")?.asString
                            if (error != null) {
                                error("Could not build: $error")
                                done = true
                            } else {
                                val data = jo.get("data")
                                if (data != null) {
                                    val dataString = data.asString
                                    val dd = Gson().fromJson(dataString, GetDependenciesData::class.java)

                                    logInfo("Read GetDependencyData, project count: ${dd.projects.size}")

                                    dd.projects.forEach { kobaltProject ->
                                        addToDependencies(project, kobaltProject.dependencies, kobaltJar)
                                    }
                                    line = ins.readLine()
                                } else {
                                    error("Did not receive a \"data\" field")
                                }
                            }
                        }
                    }
                }
            }

            outgoing.println(QUIT_COMMAND)
        } else {
            logError("Couldn't connect to server on port $port")
        }

        progress.fraction = 1.0

        //
        // All done, let the user know
        //
        group.createNotification(notificationText + " Done!", NotificationType.INFORMATION).notify(project)
    }

    private val QUIT_COMMAND = "{ \"name\" : \"quit\" }"

    private fun launchServer(port: Int, version: String, directory: String, kobaltJar: Path) {
        logInfo("Kobalt jar: $kobaltJar")
        val args = arrayListOf("java", "-jar", kobaltJar.toFile().absolutePath, "--dev",
                "--server", "--port", port.toString())
        val pb = ProcessBuilder(args)
        pb.directory(File(directory))
        pb.inheritIO()
        pb.environment().put("JAVA_HOME", ProjectJdkTable.getInstance().allJdks[0].homePath)
        logInfo("Launching " + args.joinToString(" "))
        val process = pb.start()
        val errorCode = process.waitFor()
        if (errorCode == 0) {
            logInfo("Server exiting")
        } else {
            logInfo("Server exiting with error")
        }
    }

    private fun findKobaltJar(version: String) =
        if (Constants.DEV_MODE) {
            Paths.get(System.getProperty("user.home"), "kotlin/kobalt/kobaltBuild/classes")
        } else {
            Paths.get(System.getProperty("user.home"),
                    ".kobalt/wrapper/dist/$version/kobalt/wrapper/kobalt-$version.jar")
        }

    private fun addToDependencies(project: Project, dependencies: List<DependencyData>, kobaltJar: Path) {
        val modules = ModuleManager.getInstance(project).modules
        val registrar = LibraryTablesRegistrar.getInstance()
        val libraryTable = registrar.getLibraryTable(project)

        with(ApplicationManager.getApplication()) {
            invokeLater {
                runWriteAction {
                    deleteLibrariesAndContentEntries(modules, libraryTable)
                    addDependencies(modules, dependencies, kobaltJar, libraryTable)
                }
            }
        }
    }

    private fun addDependencies(modules: Array<Module>, dependencies: List<DependencyData>, kobaltJar: Path,
            libraryTable: LibraryTable) {
        //
        // Finally, add all the dependencies received from the server
        //
        val byScope = ArrayListMultimap.create<String, DependencyData>()
        dependencies.forEach {
            byScope.put(it.scope, it)
        }

        modules.forEach { module ->
            //
            // Add kobalt.jar
            //
            val kobaltDependency = DependencyData("", "compile", kobaltJar.toFile().absolutePath)
            val kobaltLibrary= createLibrary(libraryTable, arrayListOf(kobaltDependency), "compile",
                    "kobalt.jar")
            addLibrary(kobaltLibrary, module, "compile")

            //
            // Add each dependency per scope
            //
            byScope.keySet().forEach { scope ->
                // Add the library as dependencies to the project
                // TODO: Do this to all models? How do we map IDEA modules to Kobalt projects?
                val scopedDependencies = byScope.get(scope)
                if (scopedDependencies.size > 0) {
                    val libraryName = "kobalt (${toScope(scope)})"
                    val library = createLibrary(libraryTable, scopedDependencies, scope, libraryName)
                    addLibrary(library, module, scope)
                }
            }
        }
    }

    /**
     * Delete all the Kobalt libraries and their ContentEntries.
     */
    private fun deleteLibrariesAndContentEntries(modules: Array<Module>, libraryTable: LibraryTable) {
        fun isKobalt(name: String) = name.toLowerCase().startsWith("kobalt")

        //
        // Delete all the kobalt libraries
        //
        libraryTable.modifiableModel.libraries.filter {
            isKobalt(it.name!!)
        }.map {
            val library = libraryTable.getLibraryByName(it.name!!)
            if (library != null) {
                libraryTable.removeLibrary(library)
            } else {
                LOG.error("Couldn't find library: " + it.name!!)
            }
        }
        // ... and their OrderEntries
        modules.forEach { module ->
            with (ModuleRootManager.getInstance(module).modifiableModel) {
                orderEntries.forEach { ce ->
                    if (isKobalt(ce.presentableName)) {
                        removeOrderEntry(ce)
                    }
                }
                commit()
            }
        }
    }

    private fun addLibrary(library: Library?, module: Module, scope: String) {
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

    private fun createLibrary(libraryTable: LibraryTable, dependencies: List<DependencyData>, scope: String,
            libraryName: String): Library? {
        var result: Library? = null
        libraryTable.modifiableModel.let { ltModel ->
            // Delete the old library if there's one
            ltModel.getLibraryByName(libraryName)?.let {
                logInfo("Removing existing library $it")
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

    private fun toScope(scope: String) =
            when(scope) {
                "provided" -> DependencyScope.PROVIDED
                "runtime" -> DependencyScope.RUNTIME
                "testCompile" -> DependencyScope.TEST
                else -> DependencyScope.COMPILE
            }

    private fun error(message: String) {
        logError(message)
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog("Could not build: $message", "Kobalt error")
        }
    }

    private fun logError(s: String, e: Throwable? = null) {
        LOG.error(s, e)
    }

    private fun logInfo(s: String) {
        LOG.info(s)
    }

    private fun findPort() : Int {
        if (Constants.DEV_MODE) return 1234
        else for (i in 1234..65000) {
            if (isPortAvailable(i)) return i
        }
        throw IllegalArgumentException("Couldn't find any port available, something is very wrong")
    }

    private fun isPortAvailable(port: Int) : Boolean {
        var s : Socket? = null
        try {
            s = Socket("localhost", port)
            return false
        } catch(ex: IOException) {
            return true
        } finally {
            s?.close()
        }
    }
}
