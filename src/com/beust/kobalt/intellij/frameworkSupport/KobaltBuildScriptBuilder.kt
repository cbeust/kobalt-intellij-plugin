package com.beust.kobalt.intellij.frameworkSupport

import com.intellij.openapi.externalSystem.model.project.ProjectId

/**
 * @author Dmitry Zhuravlev
 *         Date:  12.05.2016
 */
class KobaltBuildScriptBuilder(val projectId: ProjectId, val contentRootDir: String) {

    val dependencies = mutableSetOf<String>()
    val dependenciesTest = mutableSetOf<String>()
    val sourceDirectories = mutableSetOf<String>()
    val sourceTestDirectories = mutableSetOf<String>()

    fun addDependency(dependency: String) = apply { dependencies += dependency }
    fun addDependencyTest(dependency: String) = apply { dependenciesTest += dependency }
    fun addSourceDirectory(directory: String) = apply { sourceDirectories += directory }
    fun addSourceTestDirectory(directory: String) = apply { sourceTestDirectories += directory }

    fun buildBody() = """
         ${dependenciesBody()}
         ${dependenciesTestBody()}
         ${sourceDirectoriesBody()}
         ${sourceDirectoriesTestBody()}
         ${assempbleBody()}
    """

    fun dependenciesBody() = """
       dependencies {
            ${dependencies.build()}
       }
    """

    fun dependenciesTestBody() = """
       dependenciesTest {
            ${dependenciesTest.build()}
       }
    """

    fun sourceDirectoriesBody() = """
       sourceDirectories {
            ${sourceDirectories.build()}
       }
    """

    fun sourceDirectoriesTestBody() = """
       sourceDirectoriesTest {
            ${sourceDirectories.build()}
       }
    """

    fun assempbleBody() = """
       assemble {
         jar {
         }
        }
    """

    private fun MutableSet<String>.build() = joinToString("\n", transform = { it.trim() })
}