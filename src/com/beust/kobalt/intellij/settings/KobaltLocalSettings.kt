package com.beust.kobalt.intellij.settings

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.components.*
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings
import com.intellij.openapi.project.Project

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
@State(name = "KobaltLocalSettings", storages = arrayOf(Storage(StoragePathMacros.WORKSPACE_FILE)))
class KobaltLocalSettings(project: Project) : AbstractExternalSystemLocalSettings(Constants.SYSTEM_ID, project), PersistentStateComponent<AbstractExternalSystemLocalSettings.State> {
    companion object {
        fun getInstance(project: Project) = ServiceManager.getService<KobaltLocalSettings>(project, KobaltLocalSettings::class.java)
    }

    override fun getState() = State().apply { fillState(this) }
}