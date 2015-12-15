package com.beust.kobalt.intellij;

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
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
            Paths.get(System.getProperty("user.home"), "kotlin/kobalt/kobaltBuild/classes")
        } else {
            Paths.get(System.getProperty("user.home"),
                    ".kobalt/wrapper/dist/$version/kobalt/wrapper/kobalt-$version.jar")
        }

    override fun projectOpened() = BuildModule().run(project, kobaltJar)

    override fun getComponentName() = "kobalt.ProjectComponent"
    override fun initComponent() {}
    override fun disposeComponent() {}
    override fun projectClosed() {}

}
