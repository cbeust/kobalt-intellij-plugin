package com.beust.kobalt.intellij.project

import com.beust.kobalt.intellij.*
import com.beust.kobalt.intellij.import.KobaltProjectImportBuilder
import com.beust.kobalt.intellij.import.KobaltProjectImportProvider
import com.beust.kobalt.intellij.server.ServerUtil
import com.beust.kobalt.intellij.settings.KobaltSettings
import com.intellij.ide.actions.ImportModuleAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.newProjectWizard.AddModuleWizard
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.startup.StartupManager
import javax.swing.event.HyperlinkEvent

/**
 * @author Dmitry Zhuravlev
 *         Date:  27.04.2016
 */
class KobaltStartupActivity : StartupActivity {
    companion object {
        internal var SHOW_UNLINKED_KOBALT_POPUP = "show.inlinked.kobalt.project.popup"
        internal var IMPORT_EVENT_DESCRIPTION = "import"
        internal val DO_NOT_SHOW_EVENT_DESCRIPTION = "do.not.show"

        internal var DOWNLOAD_EVENT_DESCRIPTION = "download"
    }

    override fun runActivity(project: Project) {
        showNotificationForUnlinkedkobaltProject(project)
        showNotificationAboutNewKobaltVersion(project)
    }

    private fun showNotificationAboutNewKobaltVersion(project: Project) {
        val currentKobaltVersion = BuildUtils.kobaltVersion(project)?:return
        if (BuildUtils.buildFileExist(project)) {
            if (BuildUtils.kobaltProjectSettings(project)?.autoDownloadKobalt ?: false) {
                downloadAndInstallKobalt(project)
            } else if(KobaltApplicationComponent.latestKobaltVersion > currentKobaltVersion){
                val message = """<a href=$DOWNLOAD_EVENT_DESCRIPTION>Download and apply</a> new version of Kobalt."""
                KobaltNotification.getInstance(project).showBalloon(
                        "New Kobalt version available",
                        message, NotificationType.INFORMATION, object : NotificationListener.Adapter() {
                    override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                        notification.expire()
                        if (DOWNLOAD_EVENT_DESCRIPTION == e.description) {
                            downloadAndInstallKobalt(project)
                        }
                    }
                })
            }
        }
    }

    private fun downloadAndInstallKobalt(project: Project) {
        DistributionDownloader.maybeDownloadAndInstallKobaltJar(
                onSuccessDownload = { installedVersion ->
                    ServerUtil.stopServer()
                    BuildUtils.updateWrapperVersion(project, installedVersion)
                },
                onKobaltJarPresent = { installedVersion ->
                    with(StartupManager.getInstance(project)) {
                        runWhenProjectIsInitialized {
                            BuildModule().run(project, BuildUtils.findKobaltJar(installedVersion))
                            BuildUtils.kobaltProjectSettings(project)?.kobaltHome = KFiles.kobaltHomeDir(installedVersion)
                        }
                    }
                })
    }

    private fun showNotificationForUnlinkedkobaltProject(project: Project) {
        if (!PropertiesComponent.getInstance(project).getBoolean(SHOW_UNLINKED_KOBALT_POPUP, true)
                || !KobaltSettings.getInstance(project).linkedProjectsSettings.isEmpty()
                || project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) === java.lang.Boolean.TRUE
                || project.baseDir == null) {
            return
        }

        val kobaltBuildFile = BuildUtils.buildFile(project)
        if (kobaltBuildFile != null && kobaltBuildFile.exists()) {
            val message = """<a href=$IMPORT_EVENT_DESCRIPTION>Import Kobalt project</a>, this will also enable Kobalt Tool Window.
                             <br>
                             Don't want to see the message for the project again: <a href=$DO_NOT_SHOW_EVENT_DESCRIPTION>press here</a>."""

            KobaltNotification.getInstance(project).showBalloon(
                    "Detached Kobalt project found",
                    message, NotificationType.INFORMATION, object : NotificationListener.Adapter() {
                override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                    notification.expire()
                    if (IMPORT_EVENT_DESCRIPTION == e.description) {
                        val projectDataManager = ServiceManager.getService(ProjectDataManager::class.java)
                        val kobaltProjectImportBuilder = KobaltProjectImportBuilder(projectDataManager)
                        val kobaltProjectImportProvider = KobaltProjectImportProvider(kobaltProjectImportBuilder)
                        val wizard = AddModuleWizard(null, kobaltBuildFile.path, kobaltProjectImportProvider)
                        if (wizard.stepCount <= 0 || wizard.showAndGet()) {
                            ImportModuleAction.createFromWizard(project, wizard)
                        }
                    } else if (DO_NOT_SHOW_EVENT_DESCRIPTION == e.description) {
                        PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_KOBALT_POPUP, false, true)
                    }
                }
            })
        }
    }
}

