package com.beust.kobalt.intelli.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages

public class TextBoxes : AnAction("Text boxes") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT_CONTEXT)
        val txt= Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon())
        Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information",
                Messages.getInformationIcon())
    }
}