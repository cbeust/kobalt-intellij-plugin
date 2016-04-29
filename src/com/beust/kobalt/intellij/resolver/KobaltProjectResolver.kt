package com.beust.kobalt.intellij.resolver

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.Constants.Companion.KOBALT_SYSTEM_ID
import com.beust.kobalt.intellij.settings.KobaltExecutionSettings
import com.intellij.externalSystem.JavaProjectData
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.io.FileUtil
import com.intellij.pom.java.LanguageLevel
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltProjectResolver : ExternalSystemProjectResolver<KobaltExecutionSettings> {
    companion object {
        private val LOG = Logger.getInstance("#" + KobaltProjectResolver::class.java.name)
    }

    var serverMock = KobaltServerMock()  //FIXME only for testing purpose

    override fun resolveProjectInfo(id: ExternalSystemTaskId, projectPath: String, isPreviewMode: Boolean, settings: KobaltExecutionSettings?, listener: ExternalSystemTaskNotificationListener): DataNode<ProjectData>? {
        val projectDataNode = DataNode(ProjectKeys.PROJECT, ProjectData(KOBALT_SYSTEM_ID, "Kobalt Project", projectPath, projectPath), null)
        projectDataNode.createChild(JavaProjectData.KEY, createJavaProjectData(projectPath))
        val moduleDataMap = serverMock.mockData.map { serverData ->
            serverData.name to buildKobaltModuleData(projectPath, serverData)
        }.toMap()

        val nodeMap = moduleDataMap.map {pair->
            pair.key to buildProjectDataNodes(pair.value, projectDataNode)
        }.toMap()

        serverMock.mockData.map { serverData ->
            buildDependencyNodes(moduleDataMap, nodeMap, serverData)
        }
        return projectDataNode
    }

    private fun buildDependencyNodes(dataMap: Map<String, KobaltModuleData>, nodeMap: Map<String, DataNode<ModuleData>>, serverData: com.beust.kobalt.intellij.ProjectData) {
        serverData.dependentProjects.forEach { dependant ->
            val ownerData = dataMap[serverData.name]?.moduleData
            val dependantData = dataMap[dependant]?.moduleData
            if (ownerData != null && dependantData != null) {
                val moduleDependencyData = ModuleDependencyData(ownerData, dependantData)
                nodeMap[serverData.name]?.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData)
            }
        }
    }

    private fun buildKobaltModuleData(projectPath: String, serverData: com.beust.kobalt.intellij.ProjectData): KobaltModuleData {
        val moduleRootPath = FileUtil.toSystemIndependentName(projectPath + File.separator + serverData.directory)
        val moduleData = ModuleData(
                serverData.name,
                KOBALT_SYSTEM_ID,
                StdModuleTypes.JAVA.id,
                serverData.name,
                moduleRootPath,
                "$projectPath/${Constants.BUILD_FILE}")

        val contentRoot = ContentRootData(KOBALT_SYSTEM_ID, moduleRootPath)

        populateContentRoot(contentRoot, ExternalSystemSourceType.SOURCE, serverData.sourceDirs)
        populateContentRoot(contentRoot, ExternalSystemSourceType.RESOURCE, serverData.sourceResourceDirs)
        populateContentRoot(contentRoot, ExternalSystemSourceType.TEST, serverData.testDirs)
        populateContentRoot(contentRoot, ExternalSystemSourceType.TEST_RESOURCE, serverData.testResourceDirs)

        val compileLibraries = serverData.compileDependencies.map { serverCompileLibrary ->
            val libraryData = LibraryData(KOBALT_SYSTEM_ID, serverCompileLibrary.id)
            libraryData.addPath(LibraryPathType.BINARY, serverCompileLibrary.path)
            LibraryDependencyData(moduleData, libraryData, LibraryLevel.MODULE).apply { scope = DependencyScope.COMPILE }
        }

        val testLibraries = serverData.testDependencies.map { serverLibrary ->
            val libraryData = LibraryData(KOBALT_SYSTEM_ID, serverLibrary.id)
            libraryData.addPath(LibraryPathType.BINARY, serverLibrary.path)
            LibraryDependencyData(moduleData, libraryData, LibraryLevel.MODULE).apply { scope = DependencyScope.TEST }
        }

        val tasksData = serverData.tasks.map { serverTaskDataName ->
            TaskData(KOBALT_SYSTEM_ID, serverTaskDataName, projectPath, "Kobalt Task")
        }
        return KobaltModuleData(moduleData, tasksData, contentRoot, compileLibraries, testLibraries)
    }

    private fun buildProjectDataNodes(kobaltModuleData: KobaltModuleData, projectDataNode: DataNode<ProjectData>): DataNode<ModuleData> {
        val(moduleData, tasksData, contentRoot, compileDependencies, testDependencies) = kobaltModuleData
        val moduleDataNode = projectDataNode.createChild(ProjectKeys.MODULE, moduleData);
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot)
        compileDependencies.forEach { dependency ->
            moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, dependency)
        }
        testDependencies.forEach { dependency ->
            moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, dependency)
        }
        tasksData.map { task ->
            moduleDataNode.createChild(ProjectKeys.TASK, task)
        }
        return moduleDataNode
    }

    private fun populateContentRoot(contentRoot: ContentRootData, type: ExternalSystemSourceType, dirs: Set<String>) =
            dirs.forEach { dir ->
                contentRoot.storePath(type, contentRoot.rootPath + File.separator + dir)
            }


    fun createJavaProjectData(projectPath: String): JavaProjectData {
        val javaProjectData = JavaProjectData(KOBALT_SYSTEM_ID, projectPath + "/build/classes")
        var resolvedLanguageLevel: LanguageLevel? = null

        if (resolvedLanguageLevel != null) {
            javaProjectData.languageLevel = resolvedLanguageLevel
        }
        return javaProjectData
    }

    override fun cancelTask(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        return true //TODO
    }

    private data class KobaltModuleData(val moduleData: ModuleData,
                                val kobaltTasksData: List<TaskData>,
                                val contentRoot: ContentRootData,
                                val compileDependencies:List<LibraryDependencyData>,
                                val testDependencies:List<LibraryDependencyData>)
}