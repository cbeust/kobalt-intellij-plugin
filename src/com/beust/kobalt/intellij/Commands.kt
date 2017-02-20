package com.beust.kobalt.intellij

//
// The classes in this file are mapped from the JSON coming back from the server
//

data class DependencyData(val id: String, val scope: String, val path: String, var isLatest: Boolean = true,
        val children: List<DependencyData> = emptyList()) {
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
                               val pluginDependencies: List<DependencyData>?, val errorMessage: String?){
    companion object {
        val NAME = "GetDependencies"
    }
}

class ProgressCommand(val progress: Int? = null, val message: String? = null) {
    companion object {
        val NAME = "ProgressCommand"
    }
}

class CancelGetDependenciesCommand {
    companion object {
        val NAME = "CancelGetDependenciesCommand"
    }
}

class WebSocketCommand(val commandName: String, val errorMessage: String? = null, val payload: String)

data class PingResponse(val result: String)

data class TemplateData(val pluginName: String, val templates: List<String>)

data class TemplatesData(val templates: List<TemplateData>)

