package com.beust.kobalt.intellij.settings

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable
import com.intellij.openapi.project.Project

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltConfigurable(project: Project) : AbstractExternalSystemConfigurable<KobaltProjectSettings, KobaltSettingsListener, KobaltSettings>(project, Constants.SYSTEM_ID) {
    companion object{
        val HELP_TOPIC = "reference.settingsdialog.project.kobalt"
    }

    override fun createProjectSettingsControl(settings: KobaltProjectSettings) = KobaltProjectSettingsControl()

    override fun newProjectSettings() = KobaltProjectSettings()

    override fun createSystemSettingsControl(settings: KobaltSettings) = KobaltSystemSettingsControl()

    override fun getHelpTopic() = HELP_TOPIC

    override fun getId() = helpTopic
}