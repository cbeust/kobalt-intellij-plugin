package com.beust.kobalt.intellij.import

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.KobaltProjectComponent
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

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

    override fun getAffectedExternalProjectFiles(projectPath: String?, project: Project): MutableList<File> {
        val files = mutableListOf<File>()
        ModuleManager.getInstance(project).findModuleByName(KobaltProjectComponent.BUILD_MODULE_NAME)?.let { buildModule ->
            ModuleRootManager.getInstance(buildModule).contentRoots.forEach { files += collectKotlinSourceFiles(it.path) }
        }
        return files
    }

    private fun collectKotlinSourceFiles(path: String) = Files.walk(Paths.get(path))
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".kt") }
            .map(Path::toFile)
            .collect(Collectors.toList())
}