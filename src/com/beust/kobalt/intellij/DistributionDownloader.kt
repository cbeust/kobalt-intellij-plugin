package com.beust.kobalt.intellij

import com.beust.kobalt.intellij.project.KobaltNotification
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.templates.github.DownloadUtil
import com.intellij.util.io.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import javax.swing.event.HyperlinkEvent

class KFiles {
    companion object {
        private val log = Logger.getInstance(DistributionDownloader::class.java)
        private fun log(level: Int, s: String) {
            log.info(s)
        }

        private const val KOBALT_DOT_DIR : String = ".kobalt"
        const val KOBALT_DIR: String = "kobalt"

        /** Where all the .zip files are extracted */
        val distributionsDir = homeDir(KOBALT_DOT_DIR, "wrapper", "dist")

        fun kobaltHomeDir(version: String) = FileUtil.toSystemIndependentName(homeDir(KOBALT_DOT_DIR, "wrapper", "dist", "kobalt-$version"))

        fun homeDir(vararg dirs: String) : String = System.getProperty("user.home") +
                File.separator + dirs.toMutableList().joinToString(File.separator)

        fun saveFile(file: File, text: String) {
            file.absoluteFile.parentFile.mkdirs()
            file.writeText(text)
            log(2, "Wrote $file")
        }
    }
}

/**
 * Download and install a new wrapper if requested.
 */
class DistributionDownloader {
    companion object {
        private val log = Logger.getInstance(DistributionDownloader::class.java)
        private fun log(level: Int, s: String) = log.info(s)
        fun warn(s: String) = log.warn(s)
        const val RELEASE_URL = "https://api.github.com/repos/cbeust/kobalt/releases"
        const val RETRY_DOWNLOAD = "retry.download"
        fun downloadAndInstallKobaltJarSynchronously(project: Project?, kobaltVersion: String, canBeCanceled: Boolean) {
            val progressText = "Downloading Kobalt $kobaltVersion"
            ProgressManager.getInstance().run(
                    object : Task.Modal(project, "Downloading", canBeCanceled) {
                        override fun run(progress: ProgressIndicator) {
                            try {
                                DistributionDownloader().install(kobaltVersion, progress,
                                        progressText, {}, {})
                            } catch(e: IOException) {
                                KobaltNotification.getInstance(project).run {
                                    showDownloadFailedNotification(e.message, project, { downloadAndInstallKobaltJarSynchronously(project, kobaltVersion, canBeCanceled) })
                                }
                            }
                        }
                    }
            )
        }

        fun maybeDownloadAndInstallKobaltJar(project: Project, onSuccessDownload: (String) -> Unit, onKobaltJarPresent: (String) -> Unit) {
            if (!Constants.DEV_MODE) {
                val latestKobaltVersion = KobaltProjectComponent.getInstance(project).latestKobaltVersion
                val progressText = "Downloading Kobalt $latestKobaltVersion"
                ApplicationManager.getApplication().invokeLater {
                    val downloadTask = object : Task.Backgroundable(null, "Downloading") {
                        override fun run(progress: ProgressIndicator) {
                            try {
                                DistributionDownloader().install(latestKobaltVersion, progress,
                                        progressText, onSuccessDownload, onKobaltJarPresent)
                            } catch(e: IOException) {
                                showDownloadFailedNotification(e.message, project, { maybeDownloadAndInstallKobaltJar(project, onSuccessDownload, onKobaltJarPresent) })
                            }
                        }
                    }
                    val progress = BackgroundableProcessIndicator(downloadTask).apply {
                        text = progressText
                    }
                    ProgressManager.getInstance().runProcessWithProgressAsynchronously(downloadTask, progress)
                }
            } else {
                KobaltApplicationComponent.LOG.info("DEV_MODE is on, not downloading anything")
            }
        }

        inline private fun showDownloadFailedNotification(message:String?, project: Project?, crossinline download: () -> Unit) {
            KobaltNotification.getInstance(project).run {
                showBalloon("Download failed", (message ?: "Cannot download new Kobalt version")+" <a href=$RETRY_DOWNLOAD>Retry download</a>", NotificationType.ERROR,object : NotificationListener.Adapter(){
                    override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                        notification.expire()
                        if (RETRY_DOWNLOAD == e.description) {
                            download()
                        }
                    }
                })
            }
        }

    }

    val FILE_NAME = "kobalt"

    /**
     * Install a new version if requested in .kobalt/wrapper/kobalt-wrapper.properties
     *
     * @return the path to the Kobalt jar file
     */
    fun install(version: String, progress: ProgressIndicator?, progressText: String?, onSuccessDownload: (String)->Unit, onSuccessInstall: (String)->Unit) : Path {
        val fileName = "$FILE_NAME-$version.zip"
        File(KFiles.distributionsDir).mkdirs()
        val localZipFile = Paths.get(KFiles.distributionsDir, fileName)
        val zipOutputDir = KFiles.distributionsDir
        val kobaltJarFile = Paths.get(zipOutputDir, "kobalt-$version/kobalt/wrapper/$FILE_NAME-$version.jar")
        if (!Files.exists(localZipFile) || !Files.exists(kobaltJarFile)) {
            //
            // Either the .zip or the .jar is missing, downloading it
            //
            log(1, "Downloading $fileName")
            download(version, localZipFile.toFile(), progress, progressText)
            //
            // Extract all the zip files
            //
            val zipFile = ZipFile(localZipFile.toFile())
            val outputDirectory = File(KFiles.distributionsDir)
            outputDirectory.mkdirs()
            try {
                ZipUtil.extract(zipFile, outputDirectory, null)
            }catch(e:IOException){
                log.warn("Error while unzipping $zipFile to $outputDirectory : $e")
            }
            LocalFileSystem.getInstance().refreshIoFiles(listOf(kobaltJarFile.toFile()), true, false){
                onSuccessDownload(version)
                onSuccessInstall(version)
            }
        } else {
            log(1, "$localZipFile already present, no need to download it")
            onSuccessInstall(version)
        }

        return kobaltJarFile
    }

    private fun download(version: String, file: File, progress: ProgressIndicator?, progressText: String?) {
        var fileUrl = "http://beust.com/kobalt/kobalt-$version.zip"
        if (progress != null&& progressText!=null) {
            progress.text = progressText
        }

        DownloadUtil.downloadContentToFile(progress, fileUrl, file)

        if (!file.exists()) {
            log.debug(file.toString() + " downloaded, extracting it")
        } else {
            log.debug(file.toString() + " already exists, extracting it")
        }
    }
}
