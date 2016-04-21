package com.beust.kobalt.intellij.execution

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.DefaultJavaProgramRunner

/**
 * @author Dmitry Zhuravlev
 *         Date: 19.04.16
 */
class KobaltTaskRunner : DefaultJavaProgramRunner() {
    companion object{
        const val EXECUTOR_NAME ="Run task"
    }

    override fun canRun(executorId: String, profile: RunProfile) =
            DefaultRunExecutor.EXECUTOR_ID == executorId && profile.name == EXECUTOR_NAME
}