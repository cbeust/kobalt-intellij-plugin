package com.beust.kobalt.intellij

import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider
import com.intellij.openapi.vfs.VirtualFile

class KobaltProjectImportProvider(builder: KobaltProjectImportBuilder)
    : AbstractExternalProjectImportProvider(builder, KOBALT_SYSTEM_ID) {

    override fun canImportFromFile(file: VirtualFile) = file.name == "Build.kt"

    override fun getFileSample() = "<b>Kobalt</b> build script (Build.kt)"
}
