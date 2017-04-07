package com.beust.kobalt.intellij.import

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date:  06.05.2016
 */
class KobaltAutoImportAware : ExternalSystemAutoImportAware {
    override fun getAffectedExternalProjectPath(changedFileOrDirPath: String, project: Project): String? {
        if (!changedFileOrDirPath.endsWith(Constants.BUILD_FILE_NAME)) return null
        val file = LocalFileSystem.getInstance().findFileByIoFile(File(changedFileOrDirPath)) ?: return null
        val possibleProjectRootPath = file.parent?.parent?.parent?.path   //we should remove "/kobalt/src/Build.kt" to get project root
        return ExternalSystemApiUtil.getManager(Constants.KOBALT_SYSTEM_ID)!!.getSettingsProvider()
                .`fun`(project).getLinkedProjectsSettings()
                .map { it.externalProjectPath }
                .filter { it == possibleProjectRootPath }
                .firstOrNull()
    }

}