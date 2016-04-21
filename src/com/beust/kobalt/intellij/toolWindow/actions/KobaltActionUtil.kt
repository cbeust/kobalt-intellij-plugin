package com.beust.kobalt.intellij.toolWindow.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import java.awt.event.InputEvent

/**
 * @author Dmitry Zhuravlev
 *         Date: 21.04.16
 */
object KobaltActionUtil {
    fun executeAction(actionId: String, e: InputEvent) {
        val actionManager = ActionManager.getInstance()
        val action = actionManager.getAction(actionId)
        if (action != null) {
            val presentation = Presentation()
            val event = AnActionEvent(e, DataManager.getInstance().getDataContext(e.component), "", presentation, actionManager, 0)
            action.update(event)
            if (presentation.isEnabled) {
                action.actionPerformed(event)
            }
        }
    }
}