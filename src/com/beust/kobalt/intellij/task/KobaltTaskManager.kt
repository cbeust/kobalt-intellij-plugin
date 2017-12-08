package com.beust.kobalt.intellij.task

import com.beust.kobalt.intellij.MyCapturingProcessHandler
import com.beust.kobalt.intellij.settings.KobaltExecutionSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.util.Key
import com.intellij.util.containers.ContainerUtil

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltTaskManager : ExternalSystemTaskManager<KobaltExecutionSettings> {
    private var processHandler: MyCapturingProcessHandler? = null
    private val canceledTasks = ContainerUtil.newConcurrentSet<ExternalSystemTaskId>()

    override fun executeTasks(id: ExternalSystemTaskId, taskNames: MutableList<String>, projectPath: String,
                              settings: KobaltExecutionSettings?, jvmAgentSetup: String?,
                              listener: ExternalSystemTaskNotificationListener) {
        if (canceledTasks.contains(id)) {
            canceledTasks.remove(id)
            return
        }
        val kobaltJar = settings?.kobaltJar ?: return
        val vmExecutablePath = settings.vmExecutablePath
        val parameters = prepareTaskExecutionParameters(projectPath, kobaltJar, taskNames, settings.arguments, settings.vmOptions, jvmAgentSetup)
        processHandler = MyCapturingProcessHandler(parameters.toCommandLine(vmExecutablePath)).apply {
            addProcessListener(
                    object : ProcessAdapter() {
                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            listener.onTaskOutput(id, event.text, true)
                        }

                        override fun processTerminated(event: ProcessEvent) {
                            if (event.exitCode != 0 && canceledTasks.contains(id)) {
                                listener.onTaskOutput(id, "Kobalt task execution canceled.\n", false)
                                listener.onCancel(id)
                                canceledTasks.remove(id)
                            }
                        }
                    }
            )
        }
        processHandler?.runProcess()
    }

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        processHandler?.destroyProcess()
        canceledTasks.add(id)
        listener.onTaskOutput(id, "Canceling Kobalt task execution...\n", false)
        listener.beforeCancel(id)
        return true
    }

    private fun prepareTaskExecutionParameters(projectPath: String, kobaltJar: String, taskNames: MutableList<String>,
                                               scriptParameters: MutableList<String>, vmOptions: Set<String>, jvmAgentSetup: String?
    ): SimpleJavaParameters = SimpleJavaParameters().apply {
        workingDirectory = projectPath
        mainClass = "com.beust.kobalt.MainKt"
        classPath.add(kobaltJar)
        vmParametersList.addAll(vmOptions.toList())
        if (jvmAgentSetup != null) {
            vmParametersList.addParametersString(jvmAgentSetup)
        }
        programParametersList.addAll(taskNames)
        programParametersList.addAll(scriptParameters)
    }

}

fun SimpleJavaParameters.toCommandLine(vmExecutablePath: String): GeneralCommandLine {
    return JdkUtil.setupJVMCommandLine(vmExecutablePath, this, false)
}
