package com.beust.kobalt.intellij.resolver

import com.beust.kobalt.intellij.DependencyData
import com.beust.kobalt.intellij.ProjectData
import com.beust.kobalt.intellij.TaskData
import java.io.File

/**
 * @author Dmitry Zhuravlev
 * *         Date:  28.04.2016
 */
class KobaltServerMock {

    fun run(projectPath: String, callback: (List<ProjectData>) -> Unit) = callback.invoke(mockData)

    val mockData =
            listOf(ProjectData(
                    name = "module1",
                    directory = "module1",
                    dependentProjects = emptyList(),
                    compileDependencies = listOf(
                            DependencyData("org:log4j:1.2.12", "compile", System.getProperty("user.home") + File.separator + "libs${File.separator}log4j-1.2.12.jar")
                    ),
                    testDependencies = listOf(
                            DependencyData("org:junit:4.9", "test", System.getProperty("user.home") + File.separator + "libs${File.separator}junit-4.9.jar")
                    ),
                    sourceDirs = setOf("src/main/java"),
                    testDirs = setOf("src/test/java"),
                    sourceResourceDirs = setOf("main/resources"),
                    testResourceDirs = setOf("test/resources"),
                    tasks = setOf(TaskData("assemble", "Assemble Module", "build"),
                            TaskData("compile", "Compile Module", "build"))),

                    ProjectData(
                            name = "module2",
                            directory = "module2",
                            dependentProjects = listOf("module1"),
                            compileDependencies = listOf(
                                    DependencyData("org:log4j:1.2.12", "compile", System.getProperty("user.home") + File.separator + "libs${File.separator}log4j-1.2.12.jar")
                            ),
                            testDependencies = listOf(
                                    DependencyData("org:junit:4.9", "test", System.getProperty("user.home") + File.separator + "libs${File.separator}junit-4.9.jar")
                            ),
                            sourceDirs = setOf("src/main/java"),
                            testDirs = setOf("src/test/java"),
                            sourceResourceDirs = setOf("main/resources"),
                            testResourceDirs = setOf("test/resources"),
                            tasks = setOf(TaskData("assemble", "Assemble Module", "build"),
                                    TaskData("compile", "Compile Module", "build")))
            )


}

