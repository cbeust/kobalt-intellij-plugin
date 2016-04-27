package com.beust.kobalt.intellij.settings

import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl
import com.intellij.openapi.externalSystem.util.PaintAwarePanel

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltSystemSettingsControl(val settings: KobaltSettings) : ExternalSystemSettingsControl<KobaltSettings> {
    override fun isModified(): Boolean {
        //TODO
        return false
    }

    override fun showUi(p0: Boolean) {
        //TODO
    }

    override fun reset() {
        //TODO
    }

    override fun disposeUIResources() {
        //TODO
    }

    override fun apply(settings: KobaltSettings) {
        //TODO
    }

    override fun fillUi(panel: PaintAwarePanel, indentLevel: Int) {
        //TODO
    }

    override fun validate(settings: KobaltSettings): Boolean {
        //TODO
        return true
    }
}