package com.beust.kobalt.intellij

import com.beust.kobalt.intellij.settings.KobaltProjectSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.io.File
import java.nio.file.Paths

/**
 * @author Dmitry Zhuravlev
 *         Date:  25.04.2016
 */
object BuildUtils {

    fun buildFile(project: Project?) = project?.baseDir?.findFileByRelativePath(Constants.BUILD_FILE)

    fun buildFileExist(project: Project?) = buildFile(project)?.exists() ?: false

    fun kobaltVersion(project: Project) = ExternalSystemApiUtil.getSettings(project, Constants.KOBALT_SYSTEM_ID).getLinkedProjectsSettings().let { settings ->
        val iterator = settings.iterator()
        if (iterator.hasNext()) {
            val elem = iterator.next()
            if (elem is KobaltProjectSettings) {
                return@let elem.kobaltVersion()
            }
        }
        null
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
}