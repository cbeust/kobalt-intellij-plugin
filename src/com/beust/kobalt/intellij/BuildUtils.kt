package com.beust.kobalt.intellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil

/**
 * @author Dmitry Zhuravlev
 *         Date:  25.04.2016
 */
object BuildUtils {
    fun buildFile(project: Project?) = project?.baseDir?.findFileByRelativePath(Constants.BUILD_FILE)
    fun buildFileExist(project: Project?) = buildFile(project)?.exists() ?: false
    fun updateWrapperVersion(project: Project, version: String)
            = project.baseDir.findFileByRelativePath("kobalt/wrapper/kobalt-wrapper.properties")?.let { wrapperFile ->
        VfsUtil.saveText(wrapperFile, "kobalt.version=$version")
    }
}