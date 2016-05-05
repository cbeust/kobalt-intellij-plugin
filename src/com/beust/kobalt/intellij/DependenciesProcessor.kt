package com.beust.kobalt.intellij

import com.intellij.openapi.diagnostic.Logger
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */
class DependenciesProcessor(val kobaltJar: String) {

    companion object {
        val LOG = Logger.getInstance(DependenciesProcessor::class.java)
    }

    interface Api {
        @GET("/v0/getDependencies")
        fun getDependencies(@Query("buildFile") buildFile: String): Call<GetDependenciesData>
    }

    fun run(projectPath: String, callback: (List<ProjectData>) -> Unit) = callback.invoke(sendGetDependencies(projectPath))


    private fun getDependencies(projectPath: String, serverInfo: ServerInfo):List<ProjectData> {
        val service = Retrofit.Builder()
                .client(OkHttpClient.Builder()
                        .connectTimeout(3, TimeUnit.MINUTES)
                        .readTimeout(3, TimeUnit.MINUTES)
                        .build())
                .baseUrl("http://localhost:${serverInfo.port}")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Api::class.java)
        val buildFile = File(projectPath + File.separator + Constants.BUILD_FILE)
        if (!buildFile.exists()) {
            LOG.warn("Couldn't find ${Constants.BUILD_FILE} in ${buildFile.canonicalPath}, aborting")
        } else {
            val response = service.getDependencies(buildFile.canonicalPath!!).execute()

            if (response.isSuccessful) {
                val dd = response.body()
                val projects = dd.projects
                LOG.info("Read GetDependencyData, project count: ${projects.size}")
                return projects
            } else if (!response.isSuccessful) {
                LOG.error("Couldn't call getDependencies() on the server: " + response.errorBody().toString())
            }
        }
        return emptyList()
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
                    socket = Socket("localhost", port)
                    connected = true
                } else {
                    LOG.warn("Couldn't find " + ServerUtil.SERVER_FILE)
                }

            } catch(ex: Exception) {
                LOG.warn("Server is not running: " + ex.message)
            }
            if (!connected) {
                LOG.warn("Launching a new server")
                ServerUtil.launchServer(kobaltJar)
                Thread.sleep(3000)
                attempts++
                LOG.warn("New server launched, trying again")
            }
        }

        if (connected) LOG.warn("Connected to server on port $port")
        else LOG.warn("Couldn't connect to server")

        return ServerInfo(port, connected, socket?.inputStream, socket?.outputStream)
    }

    private fun sendGetDependencies(projectPath: String):List<ProjectData> {
        LOG.info("sendGetDependencies")

        val serverInfo: ServerInfo = connectToServer()

        //
        // Send the "getDependencies" command to the server
        //
        if (serverInfo.connected) {
            return getDependencies(projectPath, serverInfo)
        } else {
            LOG.error(projectPath, "Error launching the server", "Couldn't connect to server on port" +
                    " ${serverInfo.port}")
            //            Dialogs.error(projectPath, "Error launching the server", "Couldn't connect to server on port" +
            //                    " ${serverInfo.port}")
        }
        return emptyList()
        //
        // All done, let the user know
        //
    }
}