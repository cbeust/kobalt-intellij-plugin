package com.beust.kobalt.intellij.settings

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltExecutionSettings(val kobaltHome: String, val kobaltVersion:String, val kobaltJar: String, val vmExecutablePath: String)
: ExternalSystemExecutionSettings()