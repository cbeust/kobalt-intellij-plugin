package com.beust.kobalt.intellij.settings

import com.beust.kobalt.intellij.settings.ui.ProjectSettingsUIBuilder
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl
import com.intellij.openapi.externalSystem.util.PaintAwarePanel

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltProjectSettingsControl(val settings: KobaltProjectSettings) : AbstractExternalProjectSettingsControl<KobaltProjectSettings>(settings) {

    var uiBuilder = ProjectSettingsUIBuilder()

    override fun applyExtraSettings(settings: KobaltProjectSettings) {
        //TODO
    }

    override fun fillExtraControls(content: PaintAwarePanel, indentLevel: Int) {
        uiBuilder.createAndFillControls(content, indentLevel)
    }

    override fun isExtraSettingModified(): Boolean {
        //TODO
        return false
    }

    override fun resetExtraSettings(p0: Boolean) {
        //TODO
    }

    override fun validate(p0: KobaltProjectSettings): Boolean {
        //TODO
        return true
    }

    fun update(linkedProjectPath: String?, isDefaultModuleCreation: Boolean) {
         //TODO
    }

    override fun showUi(show: Boolean) {
        super.showUi(show)
        uiBuilder.showUi(show)
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        uiBuilder.disposeUIResources()
    }
}