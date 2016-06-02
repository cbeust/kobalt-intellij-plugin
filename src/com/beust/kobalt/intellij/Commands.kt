package com.beust.kobalt.intellij

//
// The classes in this file are mapped from the JSON coming back from the server
//

data class DependencyData(val id: String, val scope: String, val path: String) {
    override fun toString() = id
}

data class TaskData(val name: String, val description: String, val group: String?) {
    override fun toString() = name
}

data class ProjectData(val name: String, val directory: String,
        val dependentProjects: List<String>,
        val compileDependencies: List<DependencyData>,
        val testDependencies: List<DependencyData>, val sourceDirs: Set<String>, val testDirs: Set<String>,
        val sourceResourceDirs: Set<String>, val testResourceDirs: Set<String>,
        val tasks: Collection<TaskData>)

data class GetDependenciesData(val projects: List<ProjectData>, val allTasks: Collection<TaskData>,
        val errorMessage: String?)

data class PingResponse(val result: String)

data class TemplateData(val pluginName: String, val templates: List<String>)

data class TemplatesData(val templates: List<TemplateData>)

