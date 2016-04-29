package com.beust.kobalt.intellij.manager

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.KFiles
import com.beust.kobalt.intellij.resolver.KobaltProjectResolver
import com.beust.kobalt.intellij.settings.*
import com.beust.kobalt.intellij.task.KobaltTaskManager
import com.google.gson.JsonParser
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.service.ui.DefaultExternalSystemUiAware
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Pair
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtilRt
import okhttp3.OkHttpClient
import okio.Sink
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltManager : DefaultExternalSystemUiAware(), ExternalSystemConfigurableAware, ExternalSystemAutoImportAware, StartupActivity, ExternalSystemManager<
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

    override fun getAffectedExternalProjectPath(changedFileOrDirPath: String, project: Project): String? = null //TODO

    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect(project)
        connection.subscribe(KobaltSettings.getInstance(project).changesTopic, object : KobaltSettingsListenerAdapter() {
            override fun onKobaltHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {
                ensureProjectsRefresh()
            }

            private fun ensureProjectsRefresh() {
                ExternalSystemUtil.refreshProjects(project, Constants.KOBALT_SYSTEM_ID, true)
            }
        })
        //TODO
    }

    override fun enhanceLocalProcessing(urls: MutableList<URL>) {
    }

    override fun enhanceRemoteProcessing(parameters: SimpleJavaParameters) {
        // add Kotlin runtime. This is workaround because at the moment RemoteExternalSystemCommunicationManager have classpath without Kotlin and cannot call ProjectResolver
        val additionalClasspath = mutableListOf<String>()
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(Unit::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(JsonParser::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(Retrofit::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(OkHttpClient::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(Sink::class.java))
        ContainerUtilRt.addIfNotNull(additionalClasspath, PathUtil.getJarPathForClass(GsonConverterFactory::class.java))
        parameters.classPath.addAll(additionalClasspath)
        parameters.vmParametersList.addProperty(
                ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, Constants.KOBALT_SYSTEM_ID.id)
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

    override fun getSystemId() = Constants.KOBALT_SYSTEM_ID

    override fun getExternalProjectDescriptor() = FileChooserDescriptorFactory.createSingleFileDescriptor(Constants.BUILD_FILE_EXTENSIONS);
}

