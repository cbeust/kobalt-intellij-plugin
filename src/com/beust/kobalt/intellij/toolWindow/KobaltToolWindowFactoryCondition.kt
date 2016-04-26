package com.beust.kobalt.intellij.toolWindow

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemToolWindowCondition

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltToolWindowFactoryCondition : AbstractExternalSystemToolWindowCondition(Constants.SYSTEM_ID)