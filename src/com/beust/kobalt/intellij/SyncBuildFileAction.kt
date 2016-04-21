package com.beust.kobalt.intellij

import com.beust.kobalt.intellij.toolWindow.KobaltToolWindowComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger

/**
 * Invoked from the "Sync build file" action: launch a kobalt --server in the background, connect to it
 * and send it a getDependencies() command for the current project. When the answer is received, update
 * the project's libraries and dependencies with that information.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @since 10 23, 2015
 */
class SyncBuildFileAction : AnAction("Sync build file") {
    companion object {
        val LOG = Logger.getInstance(SyncBuildFileAction::class.java)
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { project ->
            project.getComponent(KobaltProjectComponent::class.java)?.let {
                DependenciesProcessor().run(it, project) { projectsData ->
                    Modules.configureModules(project, projectsData)
                    KobaltToolWindowComponent.getInstance(project).update(projectsData)
                }
            }
        }
    }

    //code below moved to DependenciesProcessor

    /*lateinit var progress: ProgressIndicator

    fun run(component: KobaltProjectComponent, project: Project) {
        KobaltProjectComponent.LOG.info("Syncing build file for project $project")

        with(ProgressManager.getInstance()) {
            val port = findPort()
            //            if (! Constants.DEV_MODE) {
            runProcessWithProgressAsynchronously(
                    toBackgroundTask(project, "Kobalt: Launch server", {
                        launchServer(project, port, project.basePath!!, component.kobaltJar)
                    }), EmptyProgressIndicator())
            //            }

            progress = StatusBarProgress()
            runProcessWithProgressAsynchronously(
                    toBackgroundTask(project, "Kobalt: Get dependencies", {
                        sendGetDependencies(port, project)
                    }), progress)
        }
    }

    private fun toBackgroundTask(project: Project, title: String, function: Function0<Unit>): Task.Backgroundable {
        return object: Task.Backgroundable(project, title) {
            override fun run(p0: ProgressIndicator) {
                function.invoke()
            }
        }
    }

    private fun findPort() : Int {
        for (i in 1234..65000) {
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

    private fun sendGetDependencies(port: Int, project: Project) {
        LOG.info("sendGetDependencies")

        //
        // Display the notification
        //
        val notificationText = "Synchronizing the Kobalt build file..."
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
                LOG.warn("Server not started yet, sleeping a bit")
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
                val buildFile = project.baseDir.findFileByRelativePath(Constants.BUILD_FILE)
                if (buildFile == null) {
                    LOG.warn("Couldn't find ${Constants.BUILD_FILE}, aborting")
                } else {
                    val command: String = "{ \"name\":\"getDependencies\", \"buildFile\": \"${buildFile.canonicalPath}\"}"

                    outgoing.println(command)

                    val ins = BufferedReader(InputStreamReader(socket!!.inputStream))
                    var line = ins.readLine()
                    var done = false
                    while (!done && line != null) {
                        LOG.info("Received from server: " + line)
                        val jo = JsonParser().parse(line) as JsonObject
                        if (jo.has("name") && "quit" == jo.get("name").asString) {
                            LOG.info("Quitting")
                            done = true
                        } else {
                            val error = jo.get("error")?.asString
                            if (error != null) {
                                Dialogs.error(project, "Error while building", error)
                                done = true
                            } else {
                                val data = jo.get("data")
                                if (data != null) {
                                    val dataString = data.asString
                                    val dd = Gson().fromJson(dataString, GetDependenciesData::class.java)

                                    LOG.info("Read GetDependencyData, project count: ${dd.projects.size}")

                                    Modules.configureModules(project, dd.projects)
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
            Dialogs.error(project, "Error launching the server", "Couldn't connect to server on port $port")
        }

        progress.fraction = 1.0

        //
        // All done, let the user know
        //
        group.createNotification(notificationText + " Done!", NotificationType.INFORMATION).notify(project)
    }


    private fun launchServer(project: Project, port: Int, directory: String, kobaltJar: Path) {
        LOG.info("Kobalt jar: $kobaltJar")
        if (! kobaltJar.toFile().exists()) {
            Dialogs.error(project, "Can't find the jar file", kobaltJar.toFile().absolutePath + " can't be found")
        } else {
            val args = listOf(findJava(),
                    //                "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
                    "-jar", kobaltJar.toFile().absolutePath,
                    "--dev", "--server", "--port", port.toString())
            val pb = ProcessBuilder(args)
            pb.directory(File(directory))
            pb.inheritIO()
            pb.environment().put("JAVA_HOME", ProjectJdkTable.getInstance().allJdks[0].homePath)
            val tempFile = createTempFile("kobalt")
            pb.redirectOutput(tempFile)
            LOG.warn("Launching " + args.joinToString(" "))
            LOG.warn("Server output in: $tempFile")
            val process = pb.start()
            val errorCode = process.waitFor()
            if (errorCode == 0) {
                LOG.info("Server exiting")
            } else {
                LOG.info("Server exiting with error")
            }
        }
    }

    private val QUIT_COMMAND = "{ \"name\" : \"quit\" }"

    private fun findJava() : String {
        val javaHome = System.getProperty("java.home")
        val result = if (javaHome != null) "$javaHome/bin/java" else "java"
        return result
    }
*/
}
