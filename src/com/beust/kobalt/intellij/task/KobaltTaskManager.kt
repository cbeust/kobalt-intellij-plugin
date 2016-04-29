package com.beust.kobalt.intellij.task

import com.beust.kobalt.intellij.KobaltApplicationComponent
import com.beust.kobalt.intellij.settings.KobaltExecutionSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.task.AbstractExternalSystemTaskManager
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.util.Key

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltTaskManager : AbstractExternalSystemTaskManager<KobaltExecutionSettings>() {
    override fun executeTasks(id: ExternalSystemTaskId, taskNames: MutableList<String>, projectPath: String,
                              settings: KobaltExecutionSettings?, vmOptions: MutableList<String>,
                              scriptParameters: MutableList<String>, debuggerSetup: String?,
                              listener: ExternalSystemTaskNotificationListener) {

        val parameters = prepareTaskExecutionParameters(projectPath, taskNames)

        CapturingProcessHandler(parameters.toCommandLine()).apply {
            addProcessListener(
                    object : ProcessAdapter() {
                        override fun onTextAvailable(event: ProcessEvent?, outputType: Key<*>?) {
                            if (event != null) {
                                listener.onTaskOutput(id, event.text, true)
                            }
                        }
                    }
            )
        }.runProcess()
    }

    private fun prepareTaskExecutionParameters(projectPath: String, taskNames: MutableList<String>): SimpleJavaParameters {
        val parameters = SimpleJavaParameters().apply {
            workingDirectory = projectPath
            mainClass = "com.beust.kobalt.wrapper.Main"
            classPath.add(KobaltApplicationComponent.kobaltJar.toFile())
            programParametersList.addAll(taskNames)

        }
        return parameters
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        //TODO
        return true
    }

}

fun SimpleJavaParameters.toCommandLine(): GeneralCommandLine {
    return JdkUtil.setupJVMCommandLine("java", this, false) //TODO get path for java from module JDK definition
}
