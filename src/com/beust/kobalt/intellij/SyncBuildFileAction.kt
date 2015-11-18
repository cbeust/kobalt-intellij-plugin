package com.beust.kobalt.intellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

public class SyncBuildFileAction : AnAction("Sync build file") {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { project ->
            project.getComponent(KobaltProjectComponent::class.java)?.syncBuildFile()
        }
    }
}