package com.beust.kobalt.intellij

import com.beust.kobalt.intellij.settings.KobaltProjectSettings
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.io.HttpRequests
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * @author Dmitry Zhuravlev
 *         Date:  25.04.2016
 */
object BuildUtils {

    fun buildFile(project: Project?) = project?.baseDir?.findFileByRelativePath(Constants.BUILD_FILE)

    fun buildFileExist(project: Project?) = buildFile(project)?.exists() ?: false

    fun kobaltVersion(project: Project) = kobaltProjectSettings(project)?.kobaltVersion()

    fun kobaltProjectSettings(project: Project): KobaltProjectSettings? =
            ExternalSystemApiUtil.getSettings(project, Constants.KOBALT_SYSTEM_ID).getLinkedProjectsSettings().let { settings ->
                settings.firstOrNull { it is KobaltProjectSettings } as? KobaltProjectSettings
            }

    fun findKobaltJar(version: String) =
            if (Constants.DEV_MODE) {
                Paths.get(System.getProperty("user.home"), "kotlin/kobalt/kobaltBuild/libs/kobalt-$version.jar")
            } else {
                Paths.get(System.getProperty("user.home"),
                        ".kobalt/wrapper/dist/kobalt-$version/kobalt/wrapper/kobalt-$version.jar")
            }

    fun updateWrapperVersion(externalProjectPath: String, version: String)
            = VfsUtil.findFileByIoFile(File(externalProjectPath), true)?.findFileByRelativePath("kobalt/wrapper/kobalt-wrapper.properties")?.let { wrapperFile ->
        VfsUtil.saveText(wrapperFile, "kobalt.version=$version")
    }

    fun updateWrapperVersion(project: Project, version: String)
            = project.baseDir.findFileByRelativePath("kobalt/wrapper/kobalt-wrapper.properties")?.let { wrapperFile ->
        VfsUtil.saveText(wrapperFile, "kobalt.version=$version")
    }

    fun latestKobaltVersionRequest(): Future<String> {
        val callable = Callable<String> {
            if (Constants.DEV_MODE) Constants.DEV_VERSION
            else {
                var result = Constants.MIN_KOBALT_VERSION
                try {
                    result = HttpRequests.request(DistributionDownloader.RELEASE_URL)
                            .productNameAsUserAgent().connect { request ->
                        var version: String = Constants.MIN_KOBALT_VERSION
                        @Suppress("UNCHECKED_CAST")
                        val reader = BufferedReader(InputStreamReader(request.inputStream))
                        val jo = JsonParser().parse(reader) as JsonArray
                        if (jo.size() > 0) {
                            var versionName = (jo.get(0) as JsonObject).get("name").asString
                            if (versionName == null || versionName.isBlank()) {
                                versionName = (jo.get(0) as JsonObject).get("tag_name").asString
                            }
                            if (versionName != null) {
                                version = versionName
                            }
                        }
                        version
                    }
                } catch(ex: IOException) {
                    DistributionDownloader.warn(
                            "Couldn't load the release URL: ${DistributionDownloader.RELEASE_URL}")
                }
                result
            }
        }
        return Executors.newFixedThreadPool(1).submit(callable)
    }

    fun latestKobaltVersionOrDefault(default:String): String {
        try {
            return latestKobaltVersionRequest().get(20, TimeUnit.SECONDS)
        } catch(ex: Exception) {
            return default
        }
    }
}