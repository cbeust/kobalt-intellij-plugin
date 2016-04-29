package com.beust.kobalt.intellij

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.StatusBarProgress
import com.intellij.openapi.project.Project
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit

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

    interface Api {
        @GET("/v0/getDependencies")
        fun getDependencies(@Query("buildFile") buildFile: String) : Call<GetDependenciesData>
    }

    private fun getDependencies(project: Project, serverInfo: ServerInfo,
            callback: (List<ProjectData>) -> Unit) {
        val service = Retrofit.Builder()
                .client(OkHttpClient.Builder()
                        .connectTimeout(3, TimeUnit.MINUTES)
                        .readTimeout(3, TimeUnit.MINUTES)
                        .build())
                .baseUrl("http://localhost:${serverInfo.port}")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Api::class.java)
        val buildFile = project.baseDir.findFileByRelativePath(Constants.BUILD_FILE)
        if (buildFile == null) {
            LOG.warn("Couldn't find ${Constants.BUILD_FILE}, aborting")
        } else {
            val response = service.getDependencies(buildFile.canonicalPath!!).execute()

            if (response.isSuccessful) {
                val dd = response.body()
                val projects = dd.projects
                callback(projects)
                LOG.info("Read GetDependencyData, project count: ${projects.size}")

                callback(projects)
            } else if (! response.isSuccessful) {
                LOG.error("Couldn't call getDependencies() on the server: " + response.errorBody().toString())
            }
        }
    }

    class ServerInfo(val port: Int?, val connected: Boolean, val input: InputStream?, val output: OutputStream?)

    private fun connectToServer(): ServerInfo {
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
                    LOG.warn("Located server port from the file: $port")
                    Socket("localhost", port).use { socket ->
                        connected = true
                    }
                } else {
                    LOG.warn("Couldn't find " + ServerUtil.SERVER_FILE)
                }

            } catch(ex: Exception) {
                LOG.warn("Server is not running: " + ex.message)
            }
            if (! connected) {
                LOG.warn("Launching a new server")
                ServerUtil.launchServer()
                Thread.sleep(3000)
                attempts++
                LOG.warn("New server launched, trying again")
            }
        }

        if (connected) LOG.warn("Connected to server on port $port")
        else LOG.warn("Couldn't connect to server")

        return ServerInfo(port, connected, socket?.inputStream, socket?.outputStream)
    }

    private fun sendGetDependencies(project: Project, callback: (List<ProjectData>) -> Unit) {
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

        val serverInfo : ServerInfo = connectToServer()

        progress.fraction = 0.75

        //
        // Send the "getDependencies" command to the server
        //
        if (serverInfo.connected) {
            getDependencies(project, serverInfo, callback)
        } else {
            Dialogs.error(project, "Error launching the server", "Couldn't connect to server on port" +
                    " ${serverInfo.port}")
        }

        progress.fraction = 1.0

        //
        // All done, let the user know
        //
        group.createNotification(notificationText + " Done!", NotificationType.INFORMATION).notify(project)
    }
}