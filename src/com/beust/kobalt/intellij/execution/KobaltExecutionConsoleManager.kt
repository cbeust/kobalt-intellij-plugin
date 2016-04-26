package com.beust.kobalt.intellij.execution

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.service.execution.DefaultExternalSystemExecutionConsoleManager

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltExecutionConsoleManager : DefaultExternalSystemExecutionConsoleManager() {
    override fun getExternalSystemId() = Constants.SYSTEM_ID
}