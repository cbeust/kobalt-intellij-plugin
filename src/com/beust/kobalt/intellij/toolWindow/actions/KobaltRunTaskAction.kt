package com.beust.kobalt.intellij.toolWindow.actions

import com.beust.kobalt.intellij.execution.KobaltTaskConfigurationType
import com.beust.kobalt.intellij.toolWindow.KobaltDataKeys
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */
class KobaltRunTaskAction : KobaltAction("Run task") {

    override fun isAvailable(e: AnActionEvent) =
            KobaltDataKeys.KOBALT_TASKS.getData(e.dataContext)?.isNotEmpty() ?: false

    override fun actionPerformed(event: AnActionEvent) {
        val tasks = KobaltDataKeys.KOBALT_TASKS.getData(event.dataContext) ?: return
        KobaltTaskConfigurationType.runConfiguration(event.project!!, tasks)
    }
}