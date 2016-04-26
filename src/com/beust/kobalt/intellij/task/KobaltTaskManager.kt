package com.beust.kobalt.intellij.task

import com.beust.kobalt.intellij.settings.KobaltExecutionSettings
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.task.AbstractExternalSystemTaskManager

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltTaskManager : AbstractExternalSystemTaskManager<KobaltExecutionSettings>() {
    override fun executeTasks(id: ExternalSystemTaskId, taskNames: MutableList<String>, projectPath: String,
                              settings: KobaltExecutionSettings?, vmOptions: MutableList<String>,
                              scriptParameters: MutableList<String>, debuggerSetup: String?,
                              listener: ExternalSystemTaskNotificationListener) {
        //TODO
        println("Executed tasks: $taskNames")
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        //TODO
        return true
    }

}