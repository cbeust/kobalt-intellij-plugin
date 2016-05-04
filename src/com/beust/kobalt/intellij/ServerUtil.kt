package com.beust.kobalt.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
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
import java.nio.file.Path
import java.util.*

//TODO stop server should be implemented
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

        val LOG = Logger.getInstance(DependenciesProcessor::class.java)

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

        fun launchServer() {
            maybeDownloadAndInstallKobaltJar { downloadedFilePath ->
                val kobaltJar = downloadedFilePath
                KobaltApplicationComponent.LOG.info("Kobalt jar: $kobaltJar")
                if (!kobaltJar.toFile().exists()) {
                    KobaltApplicationComponent.LOG.error("Can't find the jar file",
                            kobaltJar.toFile().absolutePath + " can't be found")
                    Dialogs.error(null, "Can't find the jar file", kobaltJar.toFile().absolutePath + " can't be found")
                } else {
                    val args = listOf(findJava(),
                            //                "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
                            "-jar", kobaltJar.toFile().absolutePath,
                            "--log", "2",
                            "--force",
                            "--dev", "--server")
                    //                KobaltApplicationComponent.threadPool.submit(Callable {
                    //                    val cl = GeneralCommandLine(args)
                    //                    LOG.warn("Launching " + args.joinToString(" "))
                    //                    val output = ExecUtil.execAndGetOutput(cl)
                    //                    LOG.warn("Server process exiting with code " + output.exitCode)
                    //                    output.stderrLines.forEach { LOG.warn("    E: ")}
                    //                    output.stdoutLines.forEach { LOG.warn("    O: ")}
                    //                })

                    val pb = ProcessBuilder(args)
                    //            pb.directory(File(directory))
                    pb.inheritIO()
                    pb.environment().put("JAVA_HOME", ProjectJdkTable.getInstance().allJdks[0].homePath)
                    val tempFile = if (Constants.DEV_MODE) {
                        File(KFiles.homeDir(".kobalt", "server.out"))
                    } else {
                        createTempFile("kobalt")
                    }
                    //                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    pb.redirectOutput(tempFile)
                    DependenciesProcessor.LOG.warn("Launching " + args.joinToString(" "))
                    DependenciesProcessor.LOG.warn("Server output in: $tempFile")
                    val process = pb.start()
                    waitForServerToStart()
                    ////                val errorCode = process.waitFor()
                    ////                if (errorCode == 0) {
                    ////                    DependenciesProcessor.LOG.info("Server exiting")
                    ////                } else {
                    ////                    DependenciesProcessor.LOG.info("Server exiting with error")
                    ////                }
                }
            }
        }

        fun launchServerNew() {
            maybeDownloadAndInstallKobaltJarNew()
            //TODO refactor
            val kobaltJar = KobaltApplicationComponent.kobaltJar.get()
            KobaltApplicationComponent.LOG.info("Kobalt jar: $kobaltJar")
            if (!kobaltJar.toFile().exists()) {
                KobaltApplicationComponent.LOG.error("Can't find the jar file",
                        kobaltJar.toFile().absolutePath + " can't be found")
                DependenciesProcessorNew.LOG.error(null, "Can't find the jar file", kobaltJar.toFile().absolutePath + " can't be found")
            } else {
                val args = listOf(findJava(),
//                "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
                        "-jar", kobaltJar.toFile().absolutePath,
                        "--log", "3",
                        "--force",
                        "--dev", "--server")
                val pb = ProcessBuilder(args)
                //            pb.directory(File(directory))
                pb.inheritIO()
//                pb.environment().put("JAVA_HOME", ProjectJdkTable.getInstance().allJdks[0].homePath)
                val tempFile = if (Constants.DEV_MODE) {
                    File(KFiles.homeDir(".kobalt", "server.out"))
                } else {
                    createTempFile("kobalt")
                }
                pb.redirectOutput(tempFile)
                DependenciesProcessorNew.LOG.warn("Launching " + args.joinToString(" "))
                DependenciesProcessorNew.LOG.warn("Server output in: $tempFile")
                val process = pb.start()
//                val errorCode = process.waitFor()
//                if (errorCode == 0) {
//                    DependenciesProcessor.LOG.info("Server exiting")
//                } else {
//                    DependenciesProcessor.LOG.info("Server exiting with error")
//                }
            }
        }

        fun maybeDownloadAndInstallKobaltJar(onSuccess:(Path)->Unit) {
            if (! Constants.DEV_MODE) {
                val progressText = "Downloading Kobalt ${KobaltApplicationComponent.version}"
                ApplicationManager.getApplication().invokeLater {
                    val progress = StatusBarProgress().apply {
                        text = progressText
                    }

                    ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                            object : Task.Backgroundable(null, "Downloading") {
                                override fun run(progress: ProgressIndicator) {
                                    onSuccess(DistributionDownloader().install(KobaltApplicationComponent.version, progress,
                                            progressText,{onSuccess()}))
                                }

                            }, progress)
                }
            } else {
                KobaltApplicationComponent.LOG.info("DEV_MODE is on, not downloading anything")
            }
        }

        fun maybeDownloadAndInstallKobaltJarNew() {
            if (! Constants.DEV_MODE) {
                val progressText = "Downloading Kobalt ${KobaltApplicationComponent.version}"
                    val progress = StatusBarProgress().apply {
                        text = progressText
                    }

//                    ProgressManager.getInstance().runProcessWithProgressAsynchronously(
//                            object : Task.Backgroundable(null, "Downloading") {
//                                override fun run(progress: ProgressIndicator) {
                                    DistributionDownloader().install(KobaltApplicationComponent.version, progress,
                                            progressText,{})
//                                }
//
//                            }, progress)
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
