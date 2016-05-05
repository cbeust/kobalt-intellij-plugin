package com.beust.kobalt.intellij

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ShutDownTracker
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.Socket
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ServerUtil {
    companion object {
        fun toBackgroundTask(project: Project?, title: String, function: Function0<Unit>): Task.Backgroundable {
            return object : Task.Backgroundable(project, title) {
                override fun run(p0: ProgressIndicator) {
                    function.invoke()
                }
            }
        }
        val SERVER_FILE = KFiles.homeDir(".kobalt", "kobaltServer.properties")
        val KEY_PORT = "port"

        @Volatile private var shuttingDown = false

        @Volatile private var  processHandler : CapturingProcessHandler? = null
        @Volatile private var threadPool: ExecutorService? = null

        val LOG = Logger.getInstance(DependenciesProcessor::class.java)

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

        private fun waitForServerToStart() : Boolean {
            var attempts = 0

            while (attempts < 7) {
                findServerPort()?.let { port ->
                    try {
                        Socket("localhost", port).use {
                            LOG.warn("     Server is now running")
                            return true
                        }
                    } catch(ex: IOException) {
                        LOG.warn("    Couldn't connect to $port: $ex")
                        // ignore
                    }
                }
                LOG.warn("     Server is still starting, sleeping a bit")
                Thread.sleep(1000)
                attempts++
            }
            LOG.warn("     Couldn't start server after multiple attempts")
            return false
        }

        fun stopServer(){
            if (processHandler?.isProcessTerminated ?: true) return
            processHandler?.destroyProcess()
            processHandler = null
            threadPool?.shutdown()
            threadPool = null
        }

        fun launchServer(kobaltJar: String) {
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
                        kobaltJar+ " can't be found")
                LOG.error(null, "Can't find the jar file", kobaltJar + " can't be found")
            } else {
                val serverExecutionParams = prepareServerExecutionParameters(kobaltJar)
                processHandler = CapturingProcessHandler(serverExecutionParams.toCommandLine()).apply {
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

            }
        }

        private fun prepareServerExecutionParameters(kobaltJar:String): SimpleJavaParameters {
            val parameters = SimpleJavaParameters().apply {
                mainClass = "com.beust.kobalt.MainKt"
                classPath.add(kobaltJar)
                programParametersList.add("--log","3")
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
