package com.beust.kobalt.intelli.plugin

import com.intellij.openapi.actionSystem.*

public class SyncBuildFileAction : AnAction("Sync build file") {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { project ->
            project.getComponent(KobaltProjectComponent::class.java).syncBuildFile()
        }
    }
}