package com.beust.kobalt.intellij.execution

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.service.execution.DefaultExternalSystemExecutionConsoleManager

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltExecutionConsoleManager : DefaultExternalSystemExecutionConsoleManager() {
    override fun getExternalSystemId() = Constants.KOBALT_SYSTEM_ID
    override fun isApplicableFor(task: ExternalSystemTask) = task.id.projectSystemId == Constants.KOBALT_SYSTEM_ID
}