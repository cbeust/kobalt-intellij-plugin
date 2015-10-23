package com.beust.kobalt.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.StatusBarProgress

/**
 * Our main application component, which just sees if our kobalt.jar file needs to be downloaded.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @since 10 23, 2015
 */
public class KobaltApplicationComponent : ApplicationComponent {
    companion object {
        val MIN_KOBALT_VERSION = "0.195"
    }

    override fun getComponentName() = "kobalt.ApplicationComponent"

    override fun initComponent() {
        var progress = StatusBarProgress().apply {
            start()
            text = "Downloading Kobalt $MIN_KOBALT_VERSION"
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            ProgressManager.getInstance().runProcess( {
                DistributionDownloader().install(MIN_KOBALT_VERSION, progress)
            }, progress)
        }
    }

    override fun disposeComponent() {
    }
}
