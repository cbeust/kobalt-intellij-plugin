package com.beust.kobalt.intellij.settings

import com.beust.kobalt.intellij.BuildUtils
import com.beust.kobalt.intellij.settings.ui.ProjectSettingsUIBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl
import com.intellij.openapi.externalSystem.util.PaintAwarePanel

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltProjectSettingsControl(val settings: KobaltProjectSettings) : AbstractExternalProjectSettingsControl<KobaltProjectSettings>(settings) {

    var uiBuilder = ProjectSettingsUIBuilder(settings)

    override fun applyExtraSettings(settings: KobaltProjectSettings) {
        uiBuilder.applySettings(settings)
        with(ApplicationManager.getApplication()) {
            runWriteAction {
                val kobaltVersion = settings.kobaltVersion()
                val externalProjectPath = settings.externalProjectPath
                if (kobaltVersion != null && externalProjectPath!=null) BuildUtils.updateWrapperVersion(externalProjectPath, kobaltVersion)
            }
        }
    }

    override fun updateInitialSettings() = uiBuilder.applySettings(initialSettings)

    override fun fillExtraControls(content: PaintAwarePanel, indentLevel: Int) {
        uiBuilder.createAndFillControls(content, indentLevel)
    }

    override fun isExtraSettingModified() = uiBuilder.isExtraSettingModified()

    override fun resetExtraSettings(isDefaultModuleCreation: Boolean)  = uiBuilder.reset(isDefaultModuleCreation)

    override fun validate(kobaltProjectSettings: KobaltProjectSettings) = uiBuilder.validate(project, kobaltProjectSettings)

    fun update(linkedProjectPath: String?, isDefaultModuleCreation: Boolean) = uiBuilder.update(linkedProjectPath,isDefaultModuleCreation)

    override fun showUi(show: Boolean) {
        super.showUi(show)
        uiBuilder.showUi(show)
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        uiBuilder.disposeUIResources()
    }
}