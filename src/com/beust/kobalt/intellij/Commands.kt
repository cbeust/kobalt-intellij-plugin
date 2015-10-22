package com.beust.kobalt.intellij

class DependencyData(val id: String, val scope: String, val path: String)

class ProjectData( val name: String, val dependencies: List<DependencyData>)

class GetDependenciesData(val projects: List<ProjectData>)

