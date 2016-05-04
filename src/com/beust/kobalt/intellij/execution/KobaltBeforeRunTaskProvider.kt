package com.beust.kobalt.intellij.execution

import com.beust.kobalt.intellij.Constants
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import icons.KobaltIcons
import javax.swing.Icon

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltBeforeRunTaskProvider(project: Project)
        : ExternalSystemBeforeRunTaskProvider(Constants.KOBALT_SYSTEM_ID,project,ID) {
    companion object{
        val ID = Key.create<ExternalSystemBeforeRunTask>("Kobalt.BeforeRunTask")
    }

    override fun getIcon() = KobaltIcons.Kobalt

    override fun getTaskIcon(task: ExternalSystemBeforeRunTask?): Icon? {
        return super.getTaskIcon(task)
    }

    override fun createTask(configuration: RunConfiguration?) = ExternalSystemBeforeRunTask(ID, Constants.KOBALT_SYSTEM_ID);

}