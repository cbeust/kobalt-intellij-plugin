package com.beust.kobalt.intellij

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportBuilder
import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import java.io.File
import javax.swing.Icon

val KOBALT_SYSTEM_ID = ProjectSystemId("KOBALT")

class KobaltProjectImportBuilder(dataManager: ProjectDataManager, topic: Topic<KobaltListener>, project: Project)
        : AbstractExternalProjectImportBuilder<ImportFromKobaltControl>(dataManager,
                ImportFromKobaltControl(topic, project), KOBALT_SYSTEM_ID) {
    override fun doPrepare(context: WizardContext) {
        throw UnsupportedOperationException()
    }

    override fun applyExtraSettings(context: WizardContext) {
        throw UnsupportedOperationException()
    }

    override fun beforeCommit(dataNode: DataNode<ProjectData>, project: Project) {
        throw UnsupportedOperationException()
    }

    override fun getExternalProjectConfigToUse(file: File): File {
        throw UnsupportedOperationException()
    }

    override fun getName(): String {
        throw UnsupportedOperationException()
    }

    override fun getIcon(): Icon? {
        throw UnsupportedOperationException()
    }

}

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

class ImportFromKobaltControl(topic: Topic<KobaltListener>, project: Project)
: AbstractImportFromExternalSystemControl<
        KobaltExternalProjectSettings,
        KobaltListener,
        KobaltExternalSystemSettings>(
            KOBALT_SYSTEM_ID,
            KobaltExternalSystemSettings(topic, project),
            KobaltExternalProjectSettings()) {
    override fun createProjectSettingsControl(settings: KobaltExternalProjectSettings)
            : ExternalSystemSettingsControl<KobaltExternalProjectSettings> {
        throw UnsupportedOperationException()
    }

    override fun createSystemSettingsControl(settings: KobaltExternalSystemSettings)
            : ExternalSystemSettingsControl<KobaltExternalSystemSettings>? {
        throw UnsupportedOperationException()
    }

    override fun onLinkedProjectPathChange(path: String) {
        throw UnsupportedOperationException()
    }
}

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

