package com.beust.kobalt.intellij.execution

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.impl.DefaultJavaProgramRunner

/**
 * @author Dmitry Zhuravlev
 *         Date: 19.04.16
 */
class KobaltTaskRunner : DefaultJavaProgramRunner() {
    companion object{
        const val RUNNER_ID ="RUN_KOBALT_TASK_RUNNER"
    }

    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile) =
            RUNNER_ID == executorId && profile.name == KobaltTaskRunConfiguration.CONFIGURATION_NAME
}