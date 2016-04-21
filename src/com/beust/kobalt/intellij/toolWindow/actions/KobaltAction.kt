package com.beust.kobalt.intellij.toolWindow.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */
abstract class KobaltAction(text: String) : AnAction(text), DumbAware {
    override fun update(e: AnActionEvent?) {
        super.update(e)
        val p = e!!.presentation
        p.isEnabled = isAvailable(e)
        p.isVisible = isVisible(e)
    }

   open protected fun isAvailable(e: AnActionEvent) = true

    open protected fun isVisible(e: AnActionEvent) = true
}