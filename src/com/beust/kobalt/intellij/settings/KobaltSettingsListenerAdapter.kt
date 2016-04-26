package com.beust.kobalt.intellij.settings

import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
abstract  class KobaltSettingsListenerAdapter : ExternalSystemSettingsListenerAdapter<KobaltProjectSettings>(), KobaltSettingsListener {
    override fun onKobaltHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {

    }
}