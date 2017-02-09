package com.beust.kobalt.intellij.resolver

import com.beust.kobalt.intellij.*
import com.beust.kobalt.intellij.server.ServerFacade
import com.beust.kobalt.intellij.server.ServerUtil
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.util.io.FileUtil
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */
class DependenciesProcessor(val kobaltJar: String) {

    companion object {
        val LOG = Logger.getInstance(DependenciesProcessor::class.java)
    }

    private var kobaltWebSocketClient: KobaltWebSocketClient? = null

    private val dependenciesFuture = CompletableFuture<GetDependenciesData?>()

    private val cancelGetDependenciesFuture = CompletableFuture<Boolean>()

    fun resolveDependencies(vmExecutablePath: String, projectPath: String, taskId: ExternalSystemTaskId,
                            listener: ExternalSystemTaskNotificationListener, callback: (GetDependenciesData) -> Unit)
            = sendGetDependenciesWebSocket(vmExecutablePath, projectPath, taskId, listener)?.run { callback.invoke(this) }

    @Deprecated("Substituded with websocket communication. Will be removed in future")
    fun run(vmExecutablePath: String, projectPath: String, callback: (GetDependenciesData) -> Unit) = sendGetDependencies(vmExecutablePath, projectPath)?.run { callback.invoke(this) }


    fun cancelResolveDependencies(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        val kobaltClient = kobaltWebSocketClient ?: return true //already cancelled
        listener.beforeCancel(taskId)
        kobaltClient.sendCommand(Gson().toJson(WebSocketCommand(CancelGetDependenciesCommand.NAME, payload = Gson().toJson(CancelGetDependenciesCommand()))))
        try {
            return cancelGetDependenciesFuture.get()
        } finally {
            listener.onCancel(taskId)
        }
    }

    private fun sendGetDependenciesWebSocket(vmExecutablePath: String, projectPath: String, taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): GetDependenciesData? {
        val buildFile = File(projectPath + File.separator + Constants.BUILD_FILE)
        val buildFilePath = FileUtil.toSystemIndependentName(buildFile.canonicalPath)
        if (!buildFile.exists()) {
            LOG.warn("Couldn't find ${Constants.BUILD_FILE} in $buildFilePath, aborting")
            return null
        }
        if (!ServerUtil.isServerRunning()) {
            ServerUtil.launchServer(vmExecutablePath, kobaltJar)
        }
        LOG.debug("Call GetDependencies for build file $buildFilePath")

        kobaltWebSocketClient = KobaltWebSocketClient(
                port = ServerUtil.findServerPort(),
                url = "/v1/getDependencyGraph?buildFile=$buildFilePath",
                onOpen = { response ->
                    processServerSocketOpen(response, taskId, listener)
                },
                onPong = { payload ->
                    processServerPong(payload)
                },
                onClose = { code, reason ->
                    processSocketClose(code, reason, dependenciesFuture, taskId, listener)
                },
                onFailure = { ex, response ->
                    processFailure(ex, response, dependenciesFuture, taskId, listener)
                },
                onMessage = { body ->
                    processMessageBody(body, dependenciesFuture, taskId, listener)
                })

        return dependenciesFuture.get()
    }

    private fun processServerSocketOpen(response: Response, taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener) {
        LOG.info("Connected to Kobalt server via websocket. response message: ${response.message()} response code: ${response.code()}")
        listener.onStart(taskId)
    }

    private fun processServerPong(payload: Buffer?) {
        LOG.info("WebSocket pong with payload: ${payload?.readUtf8()}")
    }

    private fun processSocketClose(code: Int, reason: String?, dependenciesFuture: CompletableFuture<GetDependenciesData?>, taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener) {
        LOG.info("Server close connection with reason: $reason code: $code")
        listener.onEnd(taskId)
        dependenciesFuture.complete(null)
    }

    private fun processFailure(ex: IOException, response: Response?, dependenciesFuture: CompletableFuture<GetDependenciesData?>, taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener) {
        LOG.warn("WebSocket failure: ${ex.message} response message: ${response?.message()} response code: ${response?.code()}")
        listener.onFailure(taskId, ex)
        dependenciesFuture.completeExceptionally(ex)
    }

    private fun processMessageBody(body: ResponseBody, dependenciesFuture: CompletableFuture<GetDependenciesData?>, taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener) {
        val json = body.string()
        val wsCommand = Gson().fromJson(json, WebSocketCommand::class.java)
        if (wsCommand.errorMessage != null) {
            val errorMessage = "Received error message from server: " + wsCommand.errorMessage
            val processorEx = ProcessorException(errorMessage)
            LOG.warn(errorMessage)
            listener.onFailure(taskId, processorEx)
            dependenciesFuture.completeExceptionally(processorEx)
        } else {
            when (wsCommand.commandName) {
                GetDependenciesData.NAME -> {
                    LOG.info("Received dependencies message from server: " + wsCommand.payload)
                    val dd = Gson().fromJson(wsCommand.payload, GetDependenciesData::class.java)
                    listener.onStatusChange(event("Received dependency data: " + dd.projects.size + " projects", taskId))
                    listener.onSuccess(taskId)
                    kobaltWebSocketClient?.closeSocket()
                    dependenciesFuture.complete(dd)
                }
                ProgressCommand.NAME -> {
                    LOG.info("Received progress message from server: " + wsCommand.payload)
                    val progress = Gson().fromJson(wsCommand.payload, ProgressCommand::class.java)
                    listener.onStatusChange(event(progress.message + (progress.progress ?: ""), taskId))
                }
                CancelGetDependenciesCommand.NAME -> {
                    LOG.info("Received cancel resolve dependencies confirmation from server")
                    cancelGetDependenciesFuture.complete(true)
                }
                else -> {
                    val errorMessage = "Unknown command: ${wsCommand.commandName} json:\n$json"
                    val processorEx = ProcessorException(errorMessage)
                    LOG.error(errorMessage)
                    listener.onFailure(taskId, processorEx)
                    dependenciesFuture.completeExceptionally(processorEx)
                }
            }
        }
    }

    private fun event(text: String, taskId: ExternalSystemTaskId) = ExternalSystemTaskNotificationEvent(taskId, text)

    @Deprecated("Substituded with websocket communication. Will be removed in future")
    private fun sendGetDependencies(vmExecutablePath: String, projectPath: String): GetDependenciesData? {
        val buildFile = File(projectPath + File.separator + Constants.BUILD_FILE)
        if (!buildFile.exists()) {
            LOG.warn("Couldn't find ${Constants.BUILD_FILE} in ${buildFile.canonicalPath}, aborting")
            return null
        }
        if (!ServerUtil.isServerRunning()) {
            ServerUtil.launchServer(vmExecutablePath, kobaltJar)
        }
        LOG.debug("Call GetDependencies for build file ${buildFile.canonicalPath}")
        val response = ServerFacade(ServerUtil.findServerPort()).sendGetDependencies(buildFile.canonicalPath!!)

        if (response.isSuccessful) {
            val dd = response.body()
            if (dd.errorMessage == null) {
                val projects = dd.projects
                LOG.info("Read GetDependencyData, project count: ${projects.size}")
                return dd
            } else {
                LOG.error("getDependencies() encountered an error on the server: " + dd.errorMessage)
            }
        } else if (!response.isSuccessful) {
            LOG.error("Couldn't call getDependencies() on the server: " + response.errorBody().string())
        }
        return null
    }
}