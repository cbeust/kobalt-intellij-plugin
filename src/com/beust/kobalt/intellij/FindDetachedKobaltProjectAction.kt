package com.beust.kobalt.intellij

import com.beust.kobalt.intellij.notification.showNotificationForUnlinkedkobaltProject
import com.beust.kobalt.intellij.settings.KobaltSettings
import com.beust.kobalt.intellij.toolWindow.actions.KobaltAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * @author Dmitry Zhuravlev
 *         Date:  16.01.2017
 */
class FindDetachedKobaltProjectAction : KobaltAction("Find Kobalt project"){
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let(::showNotificationForUnlinkedkobaltProject)
    }

    override fun isAvailable(e: AnActionEvent) = e.project?.let { project ->
        return KobaltSettings.getInstance(project).linkedProjectsSettings.isEmpty()
    } ?: false
}