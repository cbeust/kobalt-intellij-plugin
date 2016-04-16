package com.beust.kobalt.intellij

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.project.Project

class KobaltProjectDataService : AbstractProjectDataService<KobaltProjectData, Project>() {
    override fun getTargetDataKey() = KobaltProjectData.KEY

    override fun importData(toImport: Collection<DataNode<KobaltProjectData>>,
            projectData: ProjectData?,
            project: Project,
            modelsProvider: IdeModifiableModelsProvider) {
        println("IMPORTING")
    }

}

class KobaltProjectData(owner: ProjectSystemId) : AbstractExternalEntityData(owner) {
    companion object {
        val KEY: Key<KobaltProjectData>
                = Key.create(KobaltProjectData::class.java, ProjectKeys.PROJECT.processingWeight + 1)
    }

}