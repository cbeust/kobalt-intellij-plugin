package com.beust.kobalt.intellij.task

import com.beust.kobalt.intellij.MyCapturingProcessHandler
import com.beust.kobalt.intellij.settings.KobaltExecutionSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutput
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
    var processOutput: ProcessOutput? = null
    var processHandler: MyCapturingProcessHandler? = null

    override fun executeTasks(id: ExternalSystemTaskId, taskNames: MutableList<String>, projectPath: String,
                              settings: KobaltExecutionSettings?, vmOptions: MutableList<String>,
                              scriptParameters: MutableList<String>, debuggerSetup: String?,
                              listener: ExternalSystemTaskNotificationListener) {

        val kobaltJar = settings?.kobaltJar ?: return
        val vmExecutablePath = settings?.vmExecutablePath ?: return
        val parameters = prepareTaskExecutionParameters(projectPath, kobaltJar, taskNames, scriptParameters, vmOptions, debuggerSetup)
        processHandler = MyCapturingProcessHandler(parameters.toCommandLine(vmExecutablePath)).apply {
             addProcessListener(
                     object : ProcessAdapter() {
                         override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>?) {
                             listener.onTaskOutput(id, event.text, true)
                         }
                     }
             )
        }
        processOutput = processHandler?.runProcess()
    }

    private fun prepareTaskExecutionParameters(projectPath: String, kobaltJar: String, taskNames: MutableList<String>,
                                               scriptParameters: MutableList<String>, vmOptions: MutableList<String>,
                                               debuggerSetup: String?): SimpleJavaParameters {
        val parameters = SimpleJavaParameters().apply {
            workingDirectory = projectPath
            mainClass = "com.beust.kobalt.MainKt"
            classPath.add(kobaltJar)
            vmParametersList.addAll(vmOptions)
            if(debuggerSetup!=null) {
                vmParametersList.addParametersString(debuggerSetup)
            }
            programParametersList.addAll(taskNames)
            programParametersList.addAll(scriptParameters)
        }
        return parameters
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        //TODO
        processHandler?.destroyProcess()
        processOutput?.setCancelled()
        return processOutput?.isCancelled ?: false
    }

}

fun SimpleJavaParameters.toCommandLine(vmExecutablePath:String): GeneralCommandLine {
    return JdkUtil.setupJVMCommandLine(vmExecutablePath, this, false)
}
