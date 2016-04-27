package com.beust.kobalt.intellij

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

val KOBALT_SYSTEM_ID = ProjectSystemId("KOBALT")

// must extend AbstractImportFromExternalSystemControl<
//   ExternalProjectSettings,
//   ExternalSystemSettingsListener<ProjectSettings>,
//   AbstractExternalSystemSettings<SystemSettings, ProjectSettings, L>>


//KOBALT_SYSTEM_ID,
//KobaltExternalSystemSettings,
//KobaltSystemSettings(Topic<KobaltListener>(), ProjectManager.getInstance().getDefaultProject(),
//ImportFromKobaltControl.getInitialProjectSettings()),
//KobaltExternalProjectSettings(),
//true

// AbstractExternalProjectSettingsControl
//class ImportFromKobaltControl : AbstractExternalProjectSettingsControl<KobaltExternalProjectSettings>()



class KobaltExternalProjectSettings : ExternalProjectSettings() {
    var home: String? = null

    override fun clone(): ExternalProjectSettings {
        throw UnsupportedOperationException()
    }

}

class KobaltListener : ExternalSystemSettingsListener<KobaltExternalProjectSettings> {
    override fun onProjectsUnlinked(linkedProjectPaths: MutableSet<String>) {
        throw UnsupportedOperationException()
    }

    override fun onProjectRenamed(oldName: String, newName: String) {
        throw UnsupportedOperationException()
    }

    override fun onBulkChangeEnd() {
        throw UnsupportedOperationException()
    }

    override fun onProjectsLinked(settings: MutableCollection<KobaltExternalProjectSettings>) {
        throw UnsupportedOperationException()
    }

    override fun onUseAutoImportChange(currentValue: Boolean, linkedProjectPath: String) {
        throw UnsupportedOperationException()
    }

    override fun onBulkChangeStart() {
        throw UnsupportedOperationException()
    }

}

interface MyState {
}

// Must extend AbstractExternalSystemSettings<SystemSettings, ProjectSettings, L>>
// public class GradleSettings extends AbstractExternalSystemSettings<GradleSettings, GradleProjectSettings, GradleSettingsListener>

class KobaltExternalSystemSettings(topic: Topic<KobaltListener>, project: Project)
    : AbstractExternalSystemSettings<
        KobaltExternalSystemSettings,
        KobaltExternalProjectSettings,
        KobaltListener>(topic, project)
{
    override fun subscribe(listener: ExternalSystemSettingsListener<KobaltExternalProjectSettings>) {
        throw UnsupportedOperationException()
    }

    override fun copyExtraSettingsFrom(settings: KobaltExternalSystemSettings) {
        throw UnsupportedOperationException()
    }

    override fun checkSettings(old: KobaltExternalProjectSettings, current: KobaltExternalProjectSettings) {
        throw UnsupportedOperationException()
    }

}

