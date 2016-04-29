package com.beust.kobalt.intellij.settings

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltProjectSettings(var kobaltHome: String? = null) : ExternalProjectSettings() {
    override fun clone() = KobaltProjectSettings().apply {
        this@KobaltProjectSettings.copyTo(this)
        this@apply.kobaltHome = this@KobaltProjectSettings.kobaltHome
    }
}