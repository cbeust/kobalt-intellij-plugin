package com.beust.kobalt.intellij

import com.beust.kobalt.intellij.server.ServerFacade
import com.beust.kobalt.intellij.server.ServerUtil
import com.intellij.openapi.diagnostic.Logger
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */
class DependenciesProcessor(val kobaltJar: String) {

    companion object {
        val LOG = Logger.getInstance(DependenciesProcessor::class.java)
    }

    fun run(projectPath: String, callback: (GetDependenciesData) -> Unit) = sendGetDependencies(projectPath)?.run { callback.invoke(this) }


    private fun sendGetDependencies(projectPath: String): GetDependenciesData? {
        val buildFile = File(projectPath + File.separator + Constants.BUILD_FILE)
        if (!buildFile.exists()) {
            LOG.warn("Couldn't find ${Constants.BUILD_FILE} in ${buildFile.canonicalPath}, aborting")
            return null
        }
        if (!ServerUtil.isServerRunning()) {
            ServerUtil.launchServer(kobaltJar)
        }
        LOG.debug("Call GetDependencies for build file ${buildFile.canonicalPath}")
        val response = ServerFacade(ServerUtil.findServerPort()).sendGetDependencies(buildFile.canonicalPath!!)

        if (response.isSuccessful) {
            val dd = response.body()
            if (dd.errorMessage == null) {
                val projects = dd.projects
                LOG.info("Read GetDependencyData, project count: ${projects.size}")
                return dd
            } else {
                LOG.error("getDependencies() encountered an error on the server: " + dd.errorMessage)
            }
        } else if (!response.isSuccessful) {
            LOG.error("Couldn't call getDependencies() on the server: " + response.errorBody().string())
        }
        return null
    }
}