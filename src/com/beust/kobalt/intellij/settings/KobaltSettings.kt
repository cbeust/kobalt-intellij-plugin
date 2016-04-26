package com.beust.kobalt.intellij.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.project.Project

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
@State(name = "KobaltSettings", storages = arrayOf(Storage("kobalt.xml")))
class KobaltSettings(project: Project) : AbstractExternalSystemSettings<KobaltSettings, KobaltProjectSettings, KobaltSettingsListener>(KobaltSettingsListener.TOPIC, project), PersistentStateComponent<KobaltSettingsState> {

    companion object {
        fun getInstance(project: Project) = ServiceManager.getService<KobaltSettings>(project, KobaltSettings::class.java)
    }

    override fun loadState(state: KobaltSettingsState?) {
        if (state != null)
            super.loadState(state)
    }

    override fun getState() = KobaltSettingsState().apply { fillState(this) }

    override fun subscribe(listener: ExternalSystemSettingsListener<KobaltProjectSettings>) {
        project.messageBus.connect(project).subscribe<KobaltSettingsListener>(KobaltSettingsListener.TOPIC,
                DelegatingKobaltSettingsListenerAdapter(listener))
    }

    override fun copyExtraSettingsFrom(settings: KobaltSettings) {
        throw UnsupportedOperationException()
    }

    override fun checkSettings(p0: KobaltProjectSettings, p1: KobaltProjectSettings) {
        throw UnsupportedOperationException()
    }


   /* open class MyState : AbstractExternalSystemSettings.State<KobaltProjectSettings> {
        private val projectSettings: TreeSet<KobaltProjectSettings> = TreeSet()//ContainerUtilRt.newTreeSet<KobaltProjectSettings>()

        @AbstractCollection(surroundWithTag = false, elementTypes = arrayOf(KobaltProjectSettings::class))
        override fun getLinkedExternalProjectsSettings() = projectSettings

        override fun setLinkedExternalProjectsSettings(settings: Set<KobaltProjectSettings>?) {
            settings?.run { projectSettings.addAll(settings) }
        }
    }*/
}