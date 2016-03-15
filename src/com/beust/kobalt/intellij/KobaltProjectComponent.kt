package com.beust.kobalt.intellij;

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.StatusBarProgress
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.nio.file.Paths

class KobaltProjectComponent(val project: Project) : ProjectComponent {
    companion object {
        val LOG = Logger.getInstance(KobaltProjectComponent::class.java)

        val KOBALT_JAR = "kobalt.jar"
        val BUILD_MODULE_NAME = "Build.kt"
        val BUILD_IML_NAME = BUILD_MODULE_NAME + ".iml"
    }

    internal val kobaltJar: Path by lazy {
        findKobaltJar(KobaltApplicationComponent.version)
    }

    private fun findKobaltJar(version: String) =
        if (Constants.DEV_MODE) {
            Paths.get(System.getProperty("user.home"), "kotlin/kobalt/kobaltBuild/libs/kobalt-$version.jar")
        } else {
            Paths.get(System.getProperty("user.home"),
                    ".kobalt/wrapper/dist/kobalt-$version/kobalt/wrapper/kobalt-$version.jar")
        }

    override fun projectOpened() = BuildModule().run(project, kobaltJar)

    override fun getComponentName() = "kobalt.ProjectComponent"

    override fun initComponent() {
        if (! Constants.DEV_MODE) {
            val progressText = "Downloading Kobalt ${KobaltApplicationComponent.version}"
            ApplicationManager.getApplication().invokeLater {
                val progress = StatusBarProgress().apply {
                    text = progressText
                }

                ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                        object : Task.Backgroundable(project, "Downloading") {
                            override fun run(progress: ProgressIndicator) {
                                DistributionDownloader().install(KobaltApplicationComponent.version, progress,
                                        progressText)
                            }

                        }, progress)
            }
        } else {
            KobaltApplicationComponent.LOG.info("DEV_MODE is on, not downloading anything")
        }
    }

    override fun disposeComponent() {}
    override fun projectClosed() {}

}
