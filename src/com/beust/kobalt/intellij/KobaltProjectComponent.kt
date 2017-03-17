package com.beust.kobalt.intellij;

import com.beust.kobalt.intellij.Constants.Companion.MIN_KOBALT_VERSION
import com.beust.kobalt.intellij.server.ServerUtil
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class KobaltProjectComponent(val project: Project) : ProjectComponent {
    companion object {
        val LOG = Logger.getInstance(KobaltProjectComponent::class.java)


        val KOBALT_JAR = "kobalt.jar"
        val BUILD_MODULE_NAME = "Build.kt"
        val BUILD_IML_NAME = BUILD_MODULE_NAME + ".iml"

        fun getInstance(project: Project) = project.getComponent(KobaltProjectComponent::class.java)
    }

    val latestKobaltVersion: String by lazy {
        BuildUtils.latestKobaltVersionOrDefault(MIN_KOBALT_VERSION)
    }

    override fun projectOpened() {
    }

    override fun getComponentName() = "kobalt.ProjectComponent"

    override fun initComponent() {}
    override fun disposeComponent() {
    }
    override fun projectClosed() {
        ServerUtil.stopServer()
    }

}
