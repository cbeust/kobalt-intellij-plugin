package com.beust.kobalt.intellij.task

import com.beust.kobalt.intellij.BuildUtils
import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.NotificationSource
import java.lang.Exception

/**
 * @author Dmitry Zhuravlev
 *         Date:  13.02.2017
 */
class KobaltTaskNotificationListenerAdapter : ExternalSystemTaskNotificationListenerAdapter() {
    override fun onFailure(taskId: ExternalSystemTaskId, e: Exception) {
        if (Constants.KOBALT_SYSTEM_ID.id == taskId.projectSystemId.id && taskId.type == ExternalSystemTaskType.RESOLVE_PROJECT) {
            showFailMessage(e.message ?: "", taskId)
        }
    }
}

private fun showFailMessage(msg: String, taskId: ExternalSystemTaskId) {
    val project = taskId.findProject()
    if (project != null) {
        val buildFilePath = BuildUtils.buildFile(project)?.path
        val (line, column) = tryParseBuildFileLineAndColumn(msg)
        val notification = NotificationData(
                buildFilePath ?: "Kobalt problem", msg, NotificationCategory.ERROR, NotificationSource.PROJECT_SYNC, buildFilePath, line, column, false)
        ExternalSystemNotificationManager.getInstance(project).showNotification(taskId.projectSystemId, notification)
    }
}

private fun tryParseBuildFileLineAndColumn(msg: String): Pair<Int, Int> {
    val lineColumnStart = ".kt:"
    val lineColumnStartIndex = msg.indexOf(lineColumnStart)
    if (lineColumnStartIndex == -1) return Pair(-1, -1)
    val lineColumnEndIndex = lineColumnStartIndex + msg.substring(lineColumnStartIndex).indexOf(" ")
    if (lineColumnEndIndex == -1) return Pair(-1, -1)
    val lineAndColumnWithColonDelimiter = msg.substring(lineColumnStartIndex + lineColumnStart.length, lineColumnEndIndex)
    val line = try {
        lineAndColumnWithColonDelimiter.split(":").getOrNull(0)?.toInt() ?: -1
    } catch (e: NumberFormatException ) {
        -1
    }
    val column = try {
        lineAndColumnWithColonDelimiter.split(":").getOrNull(1)?.toInt() ?: -1
    } catch (e: NumberFormatException ) {
        -1
    }
    return Pair(line, column)
}
