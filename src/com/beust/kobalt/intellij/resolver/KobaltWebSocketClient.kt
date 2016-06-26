package com.beust.kobalt.intellij.resolver

import com.intellij.openapi.diagnostic.Logger
import okhttp3.*
import okhttp3.ws.WebSocket
import okhttp3.ws.WebSocketCall
import okhttp3.ws.WebSocketListener
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * @author Dmitry Zhuravlev
 *         Date: 16.06.16
 */
class KobaltWebSocketClient(val port: Int, val host: String = "localhost", val url: String,
                            val onOpen: (Response) -> Unit = { r -> },
                            val onPong: (Buffer?) -> Unit = { b -> },
                            val onClose: (Int, String?) -> Unit = { c, r -> },
                            val onFailure: (IOException, Response?) -> Unit = { e, r -> },
                            val onMessage: (ResponseBody) -> Unit = { b -> },
                            val readTimeoutInHours: Long = 1) {

    private var socket: WebSocket? = null

    companion object {
        private val LOG = Logger.getInstance("#" + KobaltWebSocketClient::class.java.name)
    }

    init {
        val request = Request.Builder()
                .url("ws://$host:$port$url")
                .build()
        val okHttpClient = OkHttpClient.Builder().readTimeout(readTimeoutInHours, TimeUnit.HOURS).build()
        WebSocketCall.create(okHttpClient, request).enqueue(object : WebSocketListener {
            override fun onOpen(ws: WebSocket, response: Response) {
                socket = ws
                this@KobaltWebSocketClient.onOpen(response)
            }

            override fun onPong(payload: Buffer?) {
                this@KobaltWebSocketClient.onPong(payload)
            }

            override fun onClose(code: Int, reason: String?) {
                this@KobaltWebSocketClient.onClose(code, reason)
            }

            override fun onFailure(ex: IOException, response: Response?) {
                this@KobaltWebSocketClient.onFailure(ex, response)
            }

            override fun onMessage(body: ResponseBody) {
                this@KobaltWebSocketClient.onMessage(body)
            }
        })
    }

    fun sendCommand(jsonAsString: String){
        socket?.sendMessage(RequestBody.create(WebSocket.TEXT, jsonAsString))
    }

    fun closeSocket(){
        socket?.close(1000, "Disconnect")
    }

}