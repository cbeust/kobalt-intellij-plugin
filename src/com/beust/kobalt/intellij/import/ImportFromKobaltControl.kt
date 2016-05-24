package com.beust.kobalt.intellij.import

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.KFiles
import com.beust.kobalt.intellij.settings.*
import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl
import com.intellij.openapi.project.ProjectManager

/**
 * @author Dmitry Zhuravlev
 *         Date:  27.04.2016
 */
class ImportFromKobaltControl :
        AbstractImportFromExternalSystemControl<KobaltProjectSettings, KobaltSettingsListener, KobaltSettings>(
                Constants.KOBALT_SYSTEM_ID,
                KobaltSettings(ProjectManager.getInstance().defaultProject),
                getInitialProjectSettings(),
                true
        ) {

companion object{
    private fun getInitialProjectSettings(): KobaltProjectSettings {
        val kobaltHome = KFiles.kobaltHomeDir
        val result = KobaltProjectSettings(
                autoDownloadKobalt = true,
                kobaltHome = if(!kobaltHome.isNullOrEmpty()) kobaltHome else null
        )
        return result
    }
}

    override fun onLinkedProjectPathChange(path: String)= (projectSettingsControl as KobaltProjectSettingsControl).update(path, false)

    override fun createProjectSettingsControl(settings: KobaltProjectSettings) = KobaltProjectSettingsControl(settings);

    override fun createSystemSettingsControl(settings: KobaltSettings) = KobaltSystemSettingsControl(settings)
}