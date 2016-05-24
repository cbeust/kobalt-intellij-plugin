package com.beust.kobalt.intellij.settings

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltProjectSettings(var kobaltHome: String? = null, var autoDownloadKobalt: Boolean? = null) : ExternalProjectSettings() {

    fun kobaltVersion(): String? {
        val home = kobaltHome
        if (home == null || home.isBlank()) return null
        return home.substring(home.lastIndexOf("-") + 1, home.length)
    }

    override fun clone() = KobaltProjectSettings().apply {
        this@KobaltProjectSettings.copyTo(this)
        this@apply.kobaltHome = this@KobaltProjectSettings.kobaltHome
        this@apply.autoDownloadKobalt = this@KobaltProjectSettings.autoDownloadKobalt
    }
}