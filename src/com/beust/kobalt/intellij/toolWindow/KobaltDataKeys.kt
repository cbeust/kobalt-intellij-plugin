package com.beust.kobalt.intellij.toolWindow

import com.intellij.openapi.actionSystem.DataKey

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */
object KobaltDataKeys {
    val KOBALT_TASKS = DataKey.create<List<String>>("KOBALT_TASKS");
}