package com.beust.kobalt.intellij

import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider

class KobaltProjectImportProvider(builder: KobaltProjectImportBuilder)
    : AbstractExternalProjectImportProvider(builder, KOBALT_SYSTEM_ID) {

}
