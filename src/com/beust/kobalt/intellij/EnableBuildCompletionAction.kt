package com.beust.kobalt.intellij;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger

/**
 * Enable Build.kt autocompletion by creating a new library `Build.kt` and a new module that uses it, so
 * that the build file gets compiled as a regular Kotlin source.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @since 1/29, 2015
 */
public class EnableBuildCompletionAction : AnAction("Enable Build autocompletion") {
    companion object {
        val LOG = Logger.getInstance(EnableBuildCompletionAction::class.java)
    }

    override fun actionPerformed(event: AnActionEvent) {
        LOG.debug("Enabling auto completion")
    }
}
