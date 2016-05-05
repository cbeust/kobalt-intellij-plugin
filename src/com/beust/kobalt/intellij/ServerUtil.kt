package com.beust.kobalt.intellij

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.Socket
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


        fun launchServer(kobaltJar: String) {
            KobaltApplicationComponent.LOG.info("Kobalt jar: $kobaltJar")
            if (!File(kobaltJar).exists()) {
                KobaltApplicationComponent.LOG.error("Can't find the jar file",
                        kobaltJar+ " can't be found")
                DependenciesProcessor.LOG.error(null, "Can't find the jar file", kobaltJar + " can't be found")
            } else {
                val args = listOf(findJava(),
//                "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
                        "-jar", kobaltJar,
                        "--log", "3",
                        "--force",
                        "--dev", "--server")
                val pb = ProcessBuilder(args) //TODO think about different way to execute process because in some cases it doesn't closed properly
                //            pb.directory(File(directory))
                pb.inheritIO()
//                pb.environment().put("JAVA_HOME", ProjectJdkTable.getInstance().allJdks[0].homePath)
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

        private fun findJava(): String {
            //TODO should use java from project SDK
            val javaHome = System.getProperty("java.home")
            val result = if (javaHome != null) "$javaHome/bin/java" else "java"
            return result
        }
    }
}
