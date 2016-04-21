package com.beust.kobalt.intellij.toolWindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * @author Dmitry Zhuravlev
 *         Date: 16.04.16
 */
class KobaltToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = KobaltToolWindowPanel(project, KobaltToolWindowComponent.getInstance(project).tree);
        val contentManager = toolWindow.contentManager;
        val content = contentManager.factory.createContent(toolWindowPanel, null, false);
        contentManager.addContent(content);
        Disposer.register(project, toolWindowPanel);
    }
}