package com.beust.kobalt.intellij

import com.intellij.openapi.project.Project

interface IKobaltService {
    fun syncBuildFile(project: Project)
}
