package com.beust.kobalt.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.templates.github.DownloadUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

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
        val kobaltHomeDir = FileUtil.toSystemIndependentName(homeDir(KOBALT_DOT_DIR, "wrapper", "dist", "kobalt-${KobaltApplicationComponent.version}"))
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


        fun maybeDownloadAndInstallKobaltJar(onSuccess: (Path) -> Unit) {
            if (!Constants.DEV_MODE) {
                val progressText = "Downloading Kobalt ${KobaltApplicationComponent.version}"
                ApplicationManager.getApplication().invokeLater {
                    val downloadTask = object : Task.Backgroundable(null, "Downloading") {
                        override fun run(progress: ProgressIndicator) {
                            onSuccess(DistributionDownloader().install(KobaltApplicationComponent.version, progress,
                                    progressText, { onSuccess() }))
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

        fun maybeDownloadAndInstallKobaltJarSilently() : Path? {
            if (!Constants.DEV_MODE) {
                return DistributionDownloader().install(KobaltApplicationComponent.version, null, null, {})
            } else {
                KobaltApplicationComponent.LOG.info("DEV_MODE is on, not downloading anything")
                return null
            }
        }
    }

    val FILE_NAME = "kobalt"

    /**
     * Install a new version if requested in .kobalt/wrapper/kobalt-wrapper.properties
     *
     * @return the path to the Kobalt jar file
     */
    fun install(version: String, progress: ProgressIndicator?, progressText: String?, onSuccessInstall: (Path)->Unit) : Path {
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
        } else {
            log(1, "$localZipFile already present, no need to download it")
        }


        if (Files.exists(localZipFile)) {
            //
            // Extract all the zip files
            //
            val zipFile = ZipFile(localZipFile.toFile())
            val entries = zipFile.entries()
            val outputDirectory = File(KFiles.distributionsDir)
            outputDirectory.mkdirs()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryFile = File(entry.name)
                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    val dest = Paths.get(zipOutputDir, entryFile.path)
                    log(2, "  Writing ${entry.name} to $dest")
                    try {
                        Files.createDirectories(dest.parent)
                        Files.copy(zipFile.getInputStream(entry),
                                dest,
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    } catch(ex: IOException) {
                        log.error("Error while copying $entry to $dest: ${ex.message}", ex)
                    }
                }
            }
            log(2, "$localZipFile extracted")
        } else {
            log(1, "Something went wrong: $localZipFile should exist but can't be found")
        }
        LocalFileSystem.getInstance().refreshIoFiles(listOf(kobaltJarFile.toFile()), true, false){
            onSuccessInstall(kobaltJarFile)
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
