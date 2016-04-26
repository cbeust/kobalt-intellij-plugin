package com.beust.kobalt.intellij.manager

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.KFiles
import com.beust.kobalt.intellij.resolver.KobaltProjectResolver
import com.beust.kobalt.intellij.settings.*
import com.beust.kobalt.intellij.task.KobaltTaskManager
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.ExternalSystemUiAware
import com.intellij.openapi.externalSystem.service.ui.DefaultExternalSystemUiAware
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Pair
import com.intellij.util.Function
import java.net.URL
import javax.swing.Icon

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltManager : ExternalSystemConfigurableAware, ExternalSystemUiAware, ExternalSystemAutoImportAware, StartupActivity, ExternalSystemManager<
        KobaltProjectSettings,
        KobaltSettingsListener,
        KobaltSettings,
        KobaltLocalSettings,
        KobaltExecutionSettings> {

    companion object {
        internal var LOG = Logger.getInstance("#" + KobaltManager::class.java.name)
    }

    override fun getConfigurable(project: Project) = KobaltConfigurable(project);

    override fun getExternalProjectConfigDescriptor(): FileChooserDescriptor? = null //TODO

    override fun getProjectRepresentationName(targetProjectPath: String, rootProjectPath: String?) =
            ExternalSystemApiUtil.getProjectRepresentationName(targetProjectPath, rootProjectPath);

    override fun getProjectIcon(): Icon? = null //TODO

    override fun getTaskIcon() = DefaultExternalSystemUiAware.INSTANCE.taskIcon;

    override fun getAffectedExternalProjectPath(changedFileOrDirPath: String, project: Project): String? = null //TODO

    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect(project)
        connection.subscribe(KobaltSettings.getInstance(project).changesTopic, object : KobaltSettingsListenerAdapter() {
            override fun onKobaltHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {
                super.onKobaltHomeChange(oldPath, newPath, linkedProjectPath) //TODO
            }
        })
        //TODO
    }

    override fun enhanceLocalProcessing(urls: MutableList<URL>) {
    }

    override fun enhanceRemoteProcessing(parameters: SimpleJavaParameters) {
        //TODO
        parameters.vmParametersList.addProperty(
                ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, Constants.SYSTEM_ID.id)
    }

    override fun getExecutionSettingsProvider(): Function<Pair<Project, String>, KobaltExecutionSettings> =
            Function { pair ->
                KobaltExecutionSettings(KFiles.distributionsDir)
            }

    override fun getTaskManagerClass() = KobaltTaskManager::class.java

    override fun getProjectResolverClass() = KobaltProjectResolver::class.java

    override fun getLocalSettingsProvider(): Function<Project, KobaltLocalSettings> = Function { project ->
        KobaltLocalSettings.getInstance(project)
    }

    override fun getSettingsProvider(): Function<Project, KobaltSettings> = Function { project ->
        KobaltSettings.getInstance(project)
    }

    override fun getSystemId() = Constants.SYSTEM_ID

    override fun getExternalProjectDescriptor() = FileChooserDescriptorFactory.createSingleFileDescriptor(Constants.BUILD_FILE_EXTENSIONS);
}

