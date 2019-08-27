package com.beust.kobalt.intellij

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup

class KobaltToolsGroup : DefaultActionGroup() {
    override fun update(e: AnActionEvent) {
        val p = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isVisible = p != null
    }
}
