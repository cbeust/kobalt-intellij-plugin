package com.beust.kobalt.intellij

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
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
    }

    val FILE_NAME = "kobalt"

    /**
     * Install a new version if requested in .kobalt/wrapper/kobalt-wrapper.properties
     *
     * @return the path to the Kobalt jar file
     */
    fun install(version: String, progress: ProgressIndicator, progressText: String) : Path {
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
            download(version, fileName, localZipFile.toFile(), progress, progressText)
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

        return kobaltJarFile
    }

    private fun download(version: String, fn: String, file: File, progress: ProgressIndicator, progressText: String) {
        var fileUrl = "http://beust.com/kobalt/kobalt-$version.zip"

        var done = false
        var httpConn: HttpURLConnection? = null
        var responseCode = 0
        var url: URL? = null
        while (!done) {
            url = URL(fileUrl)
            httpConn = url.openConnection() as HttpURLConnection
            responseCode = httpConn.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                fileUrl = httpConn.getHeaderField("Location")
            } else {
                done = true
            }
        }

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK && httpConn != null) {
            var fileName = ""
            val disposition = httpConn.getHeaderField("Content-Disposition")
            val contentType = httpConn.contentType
            val cl = if (httpConn.contentLength > 0) httpConn.contentLength else 25000000
            val contentLength = cl.toDouble()

            if (disposition != null) {
                // extracts file name from header field
                val index = disposition.indexOf("filename=")
                if (index > 0) {
                    fileName = disposition.substring(index + 10, disposition.length - 1)
                }
            } else {
                // extracts file name from URL
                fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1, fileUrl.length)
            }

            log(2, "Content-Type = " + contentType)
            log(2, "Content-Disposition = " + disposition!!)
            log(2, "Content-Length = " + contentLength)
            log(2, "fileName = " + fileName)

            // opens input stream from the HTTP connection
            val inputStream = httpConn.inputStream

            // opens an output stream to save into file
            val outputStream = FileOutputStream(file)

            var bytesSoFar: Double = 0.0
            val buffer = ByteArray(100000)
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesSoFar += bytesRead.toLong()
                if (bytesRead > 0) {
                    val fraction = bytesSoFar / contentLength
                    progress.fraction = fraction
                    progress.text = progressText
                    log.info("\rDownloading $url $fraction%")
                }
                bytesRead = inputStream.read(buffer)
            }
            log.debug("\n")

            outputStream.close()
            inputStream.close()

            log(1, "Downloaded " + fileUrl)
        } else {
            error("No file to download. Server replied HTTP code: " + responseCode)
        }
        httpConn.disconnect()

        if (!file.exists()) {
            log.debug(file.toString() + " downloaded, extracting it")
        } else {
            log.debug(file.toString() + " already exists, extracting it")
        }
    }
}
