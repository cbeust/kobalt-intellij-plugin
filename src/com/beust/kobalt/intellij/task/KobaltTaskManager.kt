package com.beust.kobalt.intellij.task

import com.beust.kobalt.intellij.KobaltApplicationComponent
import com.beust.kobalt.intellij.settings.KobaltExecutionSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.task.AbstractExternalSystemTaskManager
import com.intellij.openapi.projectRoots.JdkUtil

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltTaskManager : AbstractExternalSystemTaskManager<KobaltExecutionSettings>() {
    override fun executeTasks(id: ExternalSystemTaskId, taskNames: MutableList<String>, projectPath: String,
                              settings: KobaltExecutionSettings?, vmOptions: MutableList<String>,
                              scriptParameters: MutableList<String>, debuggerSetup: String?,
                              listener: ExternalSystemTaskNotificationListener) {
        val parameters = SimpleJavaParameters().apply {
            workingDirectory = projectPath
            mainClass = "com.beust.kobalt.wrapper.Main"
            classPath.add(KobaltApplicationComponent.kobaltJar.toFile())
            programParametersList.addAll(taskNames)

        }
        val out = ExecUtil.execAndGetOutput(parameters.toCommandLine())
        listener.onTaskOutput(id, out.stdout, true)
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        //TODO
        return true
    }

}

fun SimpleJavaParameters.toCommandLine(): GeneralCommandLine {
    return JdkUtil.setupJVMCommandLine("java", this, false) //TODO get path for java from module JDK definition
}
