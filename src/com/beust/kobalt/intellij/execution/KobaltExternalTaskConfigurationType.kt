package com.beust.kobalt.intellij.execution

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltExternalTaskConfigurationType : AbstractExternalSystemTaskConfigurationType(Constants.KOBALT_SYSTEM_ID) {
    companion object{
        fun getInstance() = ExternalSystemUtil.findConfigurationType(Constants.KOBALT_SYSTEM_ID) as KobaltExternalTaskConfigurationType
    }
}