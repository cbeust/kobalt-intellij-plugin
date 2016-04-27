package com.beust.kobalt.intellij

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.StatusBarProgress
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */
class DependenciesProcessor() {

    companion object {
        val LOG = Logger.getInstance(DependenciesProcessor::class.java)
    }


    lateinit var progress: ProgressIndicator

    fun run(component: KobaltProjectComponent, project: Project, process: (List<ProjectData>) -> Unit) {
        KobaltProjectComponent.LOG.info("Syncing build file for project $project")

        with(ProgressManager.getInstance()) {
            progress = StatusBarProgress()
            runProcessWithProgressAsynchronously(
                    ServerUtil.toBackgroundTask(project, "Kobalt: Get dependencies", {
                        sendGetDependencies(project, process)
                    }), progress)
        }
    }

    private fun sendGetDependencies(project: Project, calback: (List<ProjectData>) -> Unit) {
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
        var port: Int? = null
        while (attempts < 5 && !connected) {
            try {
                port = ServerUtil.findServerPort()
                if (port != null) {
                    socket = Socket("localhost", port)
                    connected = true
                }
            } catch(ex: Exception) {
                LOG.warn("Server is not running: " + ex.message)
            }
            if (! connected) {
                LOG.warn("Launching a new server")
                ServerUtil.launchServer()
                Thread.sleep(500)
                attempts++
                LOG.warn("New server launched, trying again")
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
                    LOG.info("Reading next line, ready: " + ins.ready())
                    var line = ins.readLine()
                    LOG.info("... read line $line")
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
                                    calback(dd.projects)
                                    line = ins.readLine()
                                } else {
                                    error("Did not receive a \"data\" field")
                                }
                            }
                        }
                    }
                }
            }
            //
            // All done, let the user know
            //
            group.createNotification(notificationText + " Done!", NotificationType.INFORMATION).notify(project)
        } else {
            Dialogs.error(project, "Error launching the server", "Couldn't connect to server on port $port")
        }

        progress.fraction = 1.0

    }
}