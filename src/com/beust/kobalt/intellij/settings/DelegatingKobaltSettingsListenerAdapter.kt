package com.beust.kobalt.intellij.settings

import com.intellij.openapi.externalSystem.settings.DelegatingExternalSystemSettingsListener
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class DelegatingKobaltSettingsListenerAdapter(delegate: ExternalSystemSettingsListener<KobaltProjectSettings>) : DelegatingExternalSystemSettingsListener<KobaltProjectSettings>(delegate), KobaltSettingsListener {
    override fun onKobaltHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {
    }
}