package com.beust.kobalt.intellij.resolver

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.Constants.Companion.BUILD_CLASSES_DIR_NAME
import com.beust.kobalt.intellij.Constants.Companion.BUILD_TEST_CLASSES_DIR_NAME
import com.beust.kobalt.intellij.Constants.Companion.KOBALT_BUILD_CLASSES_DIR_NAME
import com.beust.kobalt.intellij.Constants.Companion.KOBALT_BUILD_DIR_NAME
import com.beust.kobalt.intellij.Constants.Companion.KOBALT_BUILD_TEST_CLASSES_DIR_NAME
import com.beust.kobalt.intellij.Constants.Companion.KOBALT_SYSTEM_ID
import com.beust.kobalt.intellij.DependencyData
import com.beust.kobalt.intellij.GetDependenciesData
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
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltProjectResolver : ExternalSystemProjectResolver<KobaltExecutionSettings> {
    companion object {
        private val LOG = Logger.getInstance("#" + KobaltProjectResolver::class.java.name)
    }

    //    val kobaltServerResponseResolver = KobaltServerMock()  //only for testing purpose
    var kobaltServerResponseResolver: KobaltServerResponseProcessor? = null

    override fun resolveProjectInfo(id: ExternalSystemTaskId, projectPath: String, isPreviewMode: Boolean,
                                    settings: KobaltExecutionSettings?, listener: ExternalSystemTaskNotificationListener):
            DataNode<ProjectData>? {
        if (settings == null) return null
        return doResolveProjectInfo(projectPath, settings, id, listener)
    }

    private fun doResolveProjectInfo(projectPath: String, settings: KobaltExecutionSettings, taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): DataNode<ProjectData> {
        kobaltServerResponseResolver = KobaltServerResponseProcessor(settings.kobaltJar)
        val projectName = File(projectPath).let { projectPathFile -> if (projectPathFile.isDirectory) projectPathFile.name else "Kobalt Project" }
        val projectDataNode = DataNode(ProjectKeys.PROJECT,
                ProjectData(KOBALT_SYSTEM_ID, projectName, projectPath, projectPath), null)
        projectDataNode.createChild(JavaProjectData.KEY, createJavaProjectData(projectPath))
        kobaltServerResponseResolver?.resolveDependencies(settings, projectPath, taskId, listener) { dependenciesData ->
            val projectsData = dependenciesData.projects
            val allTasksData = dependenciesData.allTasks
            val moduleDataMap = projectsData.map { serverData ->
                serverData.name to buildKobaltModuleData(projectPath, serverData)
            }.toMap()

            val nodeMap = moduleDataMap.map { pair ->
                pair.key to buildProjectDataNodes(pair.value, projectDataNode)
            }.toMap()

            projectsData.forEach { serverData ->
                buildDependencyNodes(moduleDataMap, nodeMap, serverData)
            }

            allTasksData.forEach { serverTaskData ->
                val taskData = TaskData(KOBALT_SYSTEM_ID, serverTaskData.name, projectPath, serverTaskData.description)
                        .apply { group = serverTaskData.group }
                projectDataNode.createChild(ProjectKeys.TASK, taskData)
            }
            buildKobaltBuildKtModule(settings, projectPath, dependenciesData, projectDataNode)
        }

        return projectDataNode
    }

    private fun buildKobaltBuildKtModule(executionSettings: KobaltExecutionSettings, projectPath: String, dependenciesData: GetDependenciesData, projectDataNode: DataNode<ProjectData>) {
        val moduleRootPath = FileUtil.toSystemIndependentName(projectPath + File.separator + "kobalt")
        val moduleData = ModuleData(
                Constants.BUILD_FILE_NAME,
                KOBALT_SYSTEM_ID,
                StdModuleTypes.JAVA.id,
                Constants.BUILD_FILE_NAME,
                moduleRootPath,
                projectPath)
                .apply {
                    isInheritProjectCompileOutputPath = false
                    setCompileOutputPath(ExternalSystemSourceType.SOURCE, "$moduleRootPath/out/$BUILD_CLASSES_DIR_NAME")
                    setCompileOutputPath(ExternalSystemSourceType.TEST, "$moduleRootPath/out/$BUILD_TEST_CLASSES_DIR_NAME")
                }
        val contentRoot = ContentRootData(KOBALT_SYSTEM_ID, moduleRootPath)
        populateContentRoot(contentRoot, ExternalSystemSourceType.SOURCE, setOf("src"))

        val moduleDataNode = projectDataNode.createChild(ProjectKeys.MODULE, moduleData)
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot)

        dependenciesData.pluginDependencies?.forEach { pluginDependency ->
            buildLibraryDependenciesNodes(moduleDataNode, moduleData, pluginDependency, DependencyScope.COMPILE)
        }

        buildLibraryDependenciesNodes(moduleDataNode, moduleData,
                DependencyData(
                        id = "com.beust.kobalt:kobalt:jar:${executionSettings.kobaltVersion}",
                        scope = "compile",
                        path = executionSettings.kobaltJar), DependencyScope.COMPILE)
    }

    private fun buildDependencyNodes(dataMap: Map<String, KobaltModuleData>, nodeMap: Map<String,
            DataNode<ModuleData>>, serverData: com.beust.kobalt.intellij.ProjectData) {
        serverData.dependentProjects.forEach { dependant ->
            val ownerData = dataMap[serverData.name]?.moduleData
            val dependantData = dataMap[dependant]?.moduleData
            if (ownerData != null && dependantData != null) {
                val moduleDependencyData = ModuleDependencyData(ownerData, dependantData)
                nodeMap[serverData.name]?.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData)
            }
        }
    }

    private fun buildKobaltModuleData(projectPath: String, serverData: com.beust.kobalt.intellij.ProjectData):
            KobaltModuleData {
        val moduleRootPath = FileUtil.toSystemIndependentName(projectPath + File.separator + serverData.directory)
        val moduleData = ModuleData(
                serverData.name,
                KOBALT_SYSTEM_ID,
                StdModuleTypes.JAVA.id,
                serverData.name,
                moduleRootPath,
                projectPath)
                .apply {
                    isInheritProjectCompileOutputPath = false
                    setCompileOutputPath(ExternalSystemSourceType.SOURCE, "$projectPath/$KOBALT_BUILD_CLASSES_DIR_NAME")
                    setCompileOutputPath(ExternalSystemSourceType.TEST, "$projectPath/$KOBALT_BUILD_TEST_CLASSES_DIR_NAME")
                }

        val contentRoot = ContentRootData(KOBALT_SYSTEM_ID, moduleRootPath)

        populateContentRoot(contentRoot, ExternalSystemSourceType.EXCLUDED, setOf(KOBALT_BUILD_DIR_NAME))
        populateContentRoot(contentRoot, ExternalSystemSourceType.SOURCE, serverData.sourceDirs)
        populateContentRoot(contentRoot, ExternalSystemSourceType.RESOURCE, serverData.sourceResourceDirs)
        populateContentRoot(contentRoot, ExternalSystemSourceType.TEST, serverData.testDirs)
        populateContentRoot(contentRoot, ExternalSystemSourceType.TEST_RESOURCE, serverData.testResourceDirs)

        val tasksData = serverData.tasks.map { serverTaskData ->
            TaskData(KOBALT_SYSTEM_ID, "${moduleData.id}:${serverTaskData.name}", projectPath, serverTaskData.description)
                    .apply { group = serverTaskData.group }
        }
        return KobaltModuleData(moduleData, tasksData, contentRoot, serverData.compileDependencies, serverData.testDependencies)
    }

    private fun buildProjectDataNodes(kobaltModuleData: KobaltModuleData, projectDataNode: DataNode<ProjectData>)
            : DataNode<ModuleData> {
        val (moduleData, tasksData, contentRoot, compileDependencies, testDependencies) = kobaltModuleData
        val moduleDataNode = projectDataNode.createChild(ProjectKeys.MODULE, moduleData)
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot)
        compileDependencies.filter { it.isLatest }.forEach { dependency ->
            buildLibraryDependenciesNodes(moduleDataNode, moduleData, dependency, dependency.scope.toScope())
        }
        testDependencies.filter { it.isLatest }.forEach { dependency ->
            buildLibraryDependenciesNodes(moduleDataNode, moduleData, dependency, DependencyScope.TEST)
        }
        tasksData.forEach { task ->
            moduleDataNode.createChild(ProjectKeys.TASK, task)
        }
        return moduleDataNode
    }

    private fun buildLibraryDependenciesNodes(moduleDataNode: DataNode<ModuleData>, moduleData: ModuleData, serverDepLibrary: DependencyData, depScope: DependencyScope,
                                              libraryDepDataNode: DataNode<LibraryDependencyData>? = null) {
        val libraryFile = File(serverDepLibrary.path)
        val libraryData = LibraryData(KOBALT_SYSTEM_ID, serverDepLibrary.id, !libraryFile.exists())
        libraryData.addPath(LibraryPathType.BINARY, serverDepLibrary.path)

        val libraryDepData = LibraryDependencyData(moduleData, libraryData, LibraryLevel.MODULE).apply {
            scope = depScope
        }
        val currentLibraryDepNode = libraryDepDataNode?.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDepData)
                ?: moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDepData)
        serverDepLibrary.children.filter { it.isLatest }.forEach { library ->
            buildLibraryDependenciesNodes(moduleDataNode, moduleData, library, depScope, currentLibraryDepNode)
        }
    }

    private fun populateContentRoot(contentRoot: ContentRootData, type: ExternalSystemSourceType,
                                    dirs: Set<String>) =
            dirs.forEach { dir ->
                contentRoot.storePath(type, contentRoot.rootPath + File.separator + dir)
            }


    fun createJavaProjectData(projectPath: String): JavaProjectData {
        val javaProjectData = JavaProjectData(KOBALT_SYSTEM_ID, "$projectPath/$KOBALT_BUILD_CLASSES_DIR_NAME")
        return javaProjectData
    }

    override fun cancelTask(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener)
            = kobaltServerResponseResolver?.cancelResolveDependencies(taskId, listener) ?: true

    private data class KobaltModuleData(val moduleData: ModuleData,
                                        val kobaltTasksData: List<TaskData>,
                                        val contentRoot: ContentRootData,
                                        val compileDependencies: List<DependencyData>,
                                        val testDependencies: List<DependencyData>)

    private fun String.toScope(): DependencyScope = when (this) {
        "compile" -> DependencyScope.COMPILE
        "provided" -> DependencyScope.PROVIDED
        "runtime" -> DependencyScope.RUNTIME
        else -> DependencyScope.COMPILE
    }

}