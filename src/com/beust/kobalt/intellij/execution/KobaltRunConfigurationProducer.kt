package com.beust.kobalt.intellij.execution

import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRunConfigurationProducer

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltRunConfigurationProducer(type: KobaltExternalTaskConfigurationType) : AbstractExternalSystemRunConfigurationProducer(type)