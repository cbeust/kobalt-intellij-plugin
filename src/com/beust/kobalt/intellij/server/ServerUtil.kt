package com.beust.kobalt.intellij.server

import com.beust.kobalt.intellij.KFiles
import com.beust.kobalt.intellij.KobaltApplicationComponent
import com.beust.kobalt.intellij.MyCapturingProcessHandler
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ShutDownTracker
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ServerUtil {

    companion object {
        private const val PING_SUCCESSFUL_RESPONSE = "ok"
        private val SERVER_FILE = KFiles.homeDir(".kobalt", "kobaltServer.properties")
        private const val KEY_PORT = "port"

        @Volatile private var shuttingDown = false

        @Volatile private var processHandler: CapturingProcessHandler? = null
        @Volatile private var threadPool: ExecutorService? = null

        val LOG = Logger.getInstance(ServerUtil::class.java)

        fun findServerPort(): Int? {
            val file = File(SERVER_FILE)
            if (file.exists()) {
                val p = Properties().apply {
                    FileInputStream(file).use {
                        load(it)
                    }
                }
                return p.getProperty(KEY_PORT).toInt()
            } else {
                return null
            }
        }

        fun waitForServerToStart(): Boolean {
            var attempts = 0
            while (attempts < 7) {
                if (!isServerRunning()) {
                    LOG.debug("     Server is still starting, sleeping a bit")
                    Thread.sleep(1000)
                    attempts++
                } else {
                    LOG.debug("     Server is now running")
                    return true
                }
            }
            LOG.warn("     Server failed to start after $attempts attempts")
            return false
        }

        fun waitForServerToStop(): Boolean {
            var attempts = 0

            while (attempts < 7) {
                if (isServerRunning()) {
                    LOG.debug("     Server is still running, sleeping a bit")
                    Thread.sleep(1000)
                    attempts++
                } else {
                    LOG.debug("    Server stopped")
                    return true
                }
            }
            LOG.warn("     Server failed to stop after $attempts attempts")
            return false
        }

        fun isServerRunning(): Boolean {
            findServerPort()?.let { port ->
                try {
                    val response = ServerFacade(findServerPort()).sendPingCommand()
                    return if(response.isSuccessful) response.body().string() == PING_SUCCESSFUL_RESPONSE else false
                } catch(ex: IOException) {
                    LOG.debug("    Couldn't connect to $port: $ex")
                    // ignore
                }
            }
            return false
        }



        @Synchronized fun stopServer() {
            sendQuitCommand()
            if (processHandler?.isProcessTerminated ?: true) return
            processHandler?.destroyProcess()
            processHandler = null
            threadPool?.shutdown()
            threadPool = null
        }

        @Synchronized fun launchServer(kobaltJar: String) {
            if (shuttingDown) {
                return
            }
            if (ShutDownTracker.isShutdownHookRunning()) {
                shuttingDown = true
                return
            }
            threadPool = Executors.newFixedThreadPool(2)
            LOG.info("Kobalt jar: $kobaltJar")
            if (!File(kobaltJar).exists()) {
                KobaltApplicationComponent.LOG.error("Can't find the jar file",
                        kobaltJar + " can't be found")
                LOG.error(null, "Can't find the jar file", kobaltJar + " can't be found")
            } else {
                val serverExecutionParams = prepareServerExecutionParameters(kobaltJar)
                processHandler = MyCapturingProcessHandler(serverExecutionParams.toCommandLine()).apply {
                    addProcessListener(
                            object : ProcessAdapter() {
                                override fun onTextAvailable(event: ProcessEvent?, outputType: Key<*>?) {
                                    if (event != null) {
                                        LOG.info(event.text)
                                    }
                                }
                            }
                    )
                }
                threadPool?.execute {
                    processHandler?.runProcess()
                }
                waitForServerToStart()
            }
        }

        private fun sendQuitCommand() {
            if (!isServerRunning()) return
            ServerFacade(findServerPort()).sendQuitCommand()
            waitForServerToStop()
        }



        private fun prepareServerExecutionParameters(kobaltJar: String): SimpleJavaParameters {
            val parameters = SimpleJavaParameters().apply {
                mainClass = "com.beust.kobalt.MainKt"
                classPath.add(kobaltJar)
                programParametersList.add("--log", "3")
                programParametersList.add("--force")
                programParametersList.add("--dev")
                programParametersList.add("--server")

            }
            return parameters
        }

        fun SimpleJavaParameters.toCommandLine(): GeneralCommandLine {
            return JdkUtil.setupJVMCommandLine(findJava(), this, false) //TODO get path for java from module JDK definition
        }

        private fun findJava(): String {
            //TODO should use java from project SDK
            val javaHome = System.getProperty("java.home")
            val result = if (javaHome != null) "$javaHome/bin/java" else "java"
            return result
        }
    }
}
