package com.beust.kobalt.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.StatusBarProgress
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.Socket
import java.util.*

class ServerUtil {
    companion object {
        fun toBackgroundTask(project: Project?, title: String, function: Function0<Unit>): Task.Backgroundable {
            return object : Task.Backgroundable(project, title) {
                override fun run(p0: ProgressIndicator) {
                    function.invoke()
                }
            }
        }

        fun findAvailablePort(): Int {
            for (i in 1234..65000) {
                if (isPortAvailable(i)) return i
            }
            throw IllegalArgumentException("Couldn't find any port available, something is very wrong")
        }

        private fun isPortAvailable(port: Int): Boolean {
            var s: Socket? = null
            try {
                s = Socket("localhost", port)
                return false
            } catch(ex: IOException) {
                return true
            } finally {
                s?.close()
            }
        }

        val SERVER_FILE = KFiles.homeDir(".kobalt", "kobaltServer.properties")
        val KEY_PORT = "port"
        val KEY_PID = "pid"

        fun serverFileExists() = File(SERVER_FILE).exists()

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

        fun launchServer() {
            maybeDownloadAndInstallKobaltJar()

            val port = findAvailablePort()
            val kobaltJar = KobaltApplicationComponent.kobaltJar
            KobaltApplicationComponent.LOG.info("Kobalt jar: $kobaltJar")
            if (!kobaltJar.toFile().exists()) {
                KobaltApplicationComponent.LOG.error("Can't find the jar file",
                        kobaltJar.toFile().absolutePath + " can't be found")
                Dialogs.error(null, "Can't find the jar file", kobaltJar.toFile().absolutePath + " can't be found")
            } else {
                val args = listOf(findJava(),
//                "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
                        "-jar", kobaltJar.toFile().absolutePath,
                        "--log", "3",
                        "--force",
                        "--dev", "--server", "--port", port.toString())
                val pb = ProcessBuilder(args)
                //            pb.directory(File(directory))
                pb.inheritIO()
                pb.environment().put("JAVA_HOME", ProjectJdkTable.getInstance().allJdks[0].homePath)
                val tempFile = if (Constants.DEV_MODE) {
                    File(KFiles.homeDir(".kobalt", "server.out"))
                } else {
                    createTempFile("kobalt")
                }
                pb.redirectOutput(tempFile)
                DependenciesProcessor.LOG.warn("Launching " + args.joinToString(" "))
                DependenciesProcessor.LOG.warn("Server output in: $tempFile")
                val process = pb.start()
//                val errorCode = process.waitFor()
//                if (errorCode == 0) {
//                    DependenciesProcessor.LOG.info("Server exiting")
//                } else {
//                    DependenciesProcessor.LOG.info("Server exiting with error")
//                }
            }
        }

        fun maybeDownloadAndInstallKobaltJar() {
            if (! Constants.DEV_MODE) {
                val progressText = "Downloading Kobalt ${KobaltApplicationComponent.version}"
                ApplicationManager.getApplication().invokeLater {
                    val progress = StatusBarProgress().apply {
                        text = progressText
                    }

                    ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                            object : Task.Backgroundable(null, "Downloading") {
                                override fun run(progress: ProgressIndicator) {
                                    DistributionDownloader().install(KobaltApplicationComponent.version, progress,
                                            progressText)
                                }

                            }, progress)
                }
            } else {
                KobaltApplicationComponent.LOG.info("DEV_MODE is on, not downloading anything")
            }
        }

        private fun findJava(): String {
            val javaHome = System.getProperty("java.home")
            val result = if (javaHome != null) "$javaHome/bin/java" else "java"
            return result
        }
    }
}
