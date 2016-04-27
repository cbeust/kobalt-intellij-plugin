package com.beust.kobalt.intellij.resolver

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.settings.KobaltExecutionSettings
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
//TODO IDEA external API cannot call Kotlin code for some reason. Temporary use java implementation of KobaltProjectResolver
class KobaltProjectResolverDelegate : ExternalSystemProjectResolver<KobaltExecutionSettings> {
    override fun resolveProjectInfo(id: ExternalSystemTaskId, projectPath: String, isPreviewMode: Boolean, settings: KobaltExecutionSettings?, listener: ExternalSystemTaskNotificationListener): DataNode<ProjectData>? {
       //TODO
        println("Project resolved")
        return DataNode(ProjectKeys.PROJECT, ProjectData(Constants.KOBALT_SYSTEM_ID,"TODO external name","TODO ide project path path","TODO linked external path"), null)
    }

    override fun cancelTask(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        return true //TODO
    }
}