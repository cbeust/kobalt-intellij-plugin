package com.beust.kobalt.intellij.import

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider
import com.intellij.openapi.vfs.VirtualFile

/**
 * @author Dmitry Zhuravlev
 *         Date:  27.04.2016
 */
class KobaltProjectImportProvider(builder: KobaltProjectImportBuilder) : AbstractExternalProjectImportProvider(builder, Constants.KOBALT_SYSTEM_ID) {

    override fun canImportFromFile(file: VirtualFile?) = file?.name == Constants.BUILD_FILE_NAME

    override fun getFileSample() = "<b>Kobalt</b> build script (Build.kt)"
}