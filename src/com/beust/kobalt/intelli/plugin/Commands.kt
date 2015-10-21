package com.beust.kobalt.intelli.plugin

class DependencyData(val id: String, val path: String)

class ProjectData(val name: String, val dependencies: List<DependencyData>,
        val providedDependencies: List<DependencyData>,
        val runtimeDependencies: List<DependencyData>,
        val testDependencies: List<DependencyData>,
        val testProvidedDependencies: List<DependencyData>)

class GetDependenciesData(val projects: List<ProjectData>)

