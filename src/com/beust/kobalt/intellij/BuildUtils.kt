package com.beust.kobalt.intellij

import com.intellij.openapi.project.Project

/**
 * @author Dmitry Zhuravlev
 *         Date:  25.04.2016
 */
object  BuildUtils {
    fun buildFileExist(project: Project?) = project?.baseDir?.findFileByRelativePath(Constants.BUILD_FILE)!=null
}