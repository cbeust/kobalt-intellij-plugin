package com.beust.kobalt.intellij

//
// The classes in this file are mapped from the JSON coming back from the server
//

class DependencyData(val id: String, val scope: String, val path: String)

class ProjectData( val name: String, val dependencies: List<DependencyData>)

class GetDependenciesData(val projects: List<ProjectData>)

