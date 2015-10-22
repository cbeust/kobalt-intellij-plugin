package com.beust.kobalt.intellij

import com.intellij.openapi.project.Project
import java.util.concurrent.Executors

class KobaltService: IKobaltService {
    val executor = Executors.newFixedThreadPool(2)
    val port = 1234
    init {
    }
    override fun syncBuildFile(project: Project) {
        println("SYNCING BUILD FILE")
    }
}