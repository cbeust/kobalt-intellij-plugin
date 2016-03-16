package com.beust.kobalt.intellij

import com.intellij.execution.ExecutionException
import com.intellij.execution.util.ExecutionErrorDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

class Dialogs {
    companion object {
        fun error(project: Project, title: String, error: String) {
            ApplicationManager.getApplication().invokeLater {
                ExecutionErrorDialog.show(ExecutionException(error), title, project)
            }

        }
    }
}
