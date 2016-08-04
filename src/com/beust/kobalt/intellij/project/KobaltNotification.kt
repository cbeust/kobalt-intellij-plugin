package com.beust.kobalt.intellij.project

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

/**
 * @author Dmitry Zhuravlev
 *         Date:  27.04.2016
 */
class KobaltNotification(val project: Project?) {
    companion object {
        private val NOTIFICATION_GROUP = NotificationGroup.balloonGroup("Kobalt Notification Group")
        fun getInstance(project: Project? = null) = when (project) {
            null -> ServiceManager.getService<KobaltNotification>(KobaltNotification::class.java)!!
            else -> ServiceManager.getService<KobaltNotification>(project, KobaltNotification::class.java)!!
        }
    }

    fun showBalloon(title: String,
                    message: String,
                    type: NotificationType,
                    listener: NotificationListener? = null) {
        NOTIFICATION_GROUP.createNotification(title, message, type, listener).notify(project)
    }
}