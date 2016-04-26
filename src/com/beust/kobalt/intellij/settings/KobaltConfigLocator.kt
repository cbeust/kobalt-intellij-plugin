package com.beust.kobalt.intellij.settings

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltConfigLocator : ExternalSystemConfigLocator {

    override fun getTargetExternalSystemId() = Constants.SYSTEM_ID

    override fun adjust(configPath: VirtualFile): VirtualFile? {
        if (!configPath.isDirectory) {
            return configPath
        }

        val result = configPath.findChild(Constants.BUILD_FILE_NAME)
        if (result != null) {
            return result
        }

        for (child in configPath.getChildren()) {
            val name = child.name
            if (!name.endsWith(Constants.BUILD_FILE_EXTENSIONS)) {
                continue
            }
            if ( !child.isDirectory) {
                return child
            }
        }
        return null
    }

    override fun findAll(externalProjectSettings: ExternalProjectSettings): MutableList<VirtualFile> {
        val list = ContainerUtil.newArrayList<VirtualFile>()
        for (path in externalProjectSettings.modules) {
            val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(path))
            if (vFile != null) {
                for (child in vFile.children) {
                    val name = child.name
                    if (!child.isDirectory && name.endsWith(Constants.BUILD_FILE_EXTENSIONS)) {
                        list.add(child)
                    }
                }
            }
        }
        return list
    }
}