package com.beust.kobalt.intellij.settings

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl
import com.intellij.openapi.externalSystem.util.PaintAwarePanel

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltProjectSettingsControl : AbstractExternalProjectSettingsControl<KobaltProjectSettings>(KobaltProjectSettings()) { //TODO build proper settings

    override fun applyExtraSettings(settings: KobaltProjectSettings) {
        //TODO
    }

    override fun fillExtraControls(content: PaintAwarePanel, indentLevel: Int) {
       //TODO
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
}