package com.beust.kobalt.intellij.project

import com.beust.kobalt.intellij.notification.showNotificationAboutNewKobaltVersion
import com.beust.kobalt.intellij.notification.showNotificationForUnlinkedkobaltProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

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
}

