package com.beust.kobalt.intellij

import com.intellij.openapi.project.Project

/**
 * @author Dmitry Zhuravlev
 *         Date:  25.04.2016
 */
object  BuildUtils {
    fun buildFile(project: Project?) = project?.baseDir?.findFileByRelativePath(Constants.BUILD_FILE)
    fun buildFileExist(project: Project?) = buildFile(project)?.exists()?:false
}