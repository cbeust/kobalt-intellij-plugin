package com.beust.kobalt.intellij.execution

import com.beust.kobalt.intellij.Constants.Companion.KOBALT_SYSTEM_ID
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRunConfigurationProducer
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemTaskLocation
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltRunConfigurationProducer(type: KobaltExternalTaskConfigurationType) : AbstractExternalSystemRunConfigurationProducer(type) {
    override fun setupConfigurationFromContext(configuration: ExternalSystemRunConfiguration?, context: ConfigurationContext?, sourceElement: Ref<PsiElement>?) =
            if ((context?.location as? ExternalSystemTaskLocation)?.taskInfo?.settings?.externalSystemId != KOBALT_SYSTEM_ID)
                false
            else
                super.setupConfigurationFromContext(configuration, context, sourceElement)
}