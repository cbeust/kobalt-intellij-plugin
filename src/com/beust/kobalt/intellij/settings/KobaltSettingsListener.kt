package com.beust.kobalt.intellij.settings

import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.util.messages.Topic

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
interface KobaltSettingsListener : ExternalSystemSettingsListener<KobaltProjectSettings> {
companion object{
    val TOPIC = Topic.create<KobaltSettingsListener>("Kobalt-specific settings", KobaltSettingsListener::class.java)
}
    fun onKobaltHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String)
}