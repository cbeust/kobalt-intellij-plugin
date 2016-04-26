package com.beust.kobalt.intellij.execution

import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRuntimeConfigurationProducer

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltRuntimeConfigurationProducer(type: KobaltExternalTaskConfigurationType) : AbstractExternalSystemRuntimeConfigurationProducer(type)