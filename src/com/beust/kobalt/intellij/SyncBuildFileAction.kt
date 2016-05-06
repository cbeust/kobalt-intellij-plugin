package com.beust.kobalt.intellij

//import com.beust.kobalt.intellij.toolWindow.KobaltToolWindowComponent
import com.beust.kobalt.intellij.toolWindow.actions.KobaltAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager

/**
 * Invoked from the "Sync build file" action: launch a kobalt --server in the background, connect to it
 * and send it a getDependencies() command for the current project. When the answer is received, update
 * the project's libraries and dependencies with that information.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @since 10 23, 2015
 */
@Deprecated("Instead this tool window will be used")
class SyncBuildFileAction : KobaltAction("Sync build file") {
    companion object {
        val LOG = Logger.getInstance(SyncBuildFileAction::class.java)
    }

    override fun isAvailable(e: AnActionEvent) = BuildUtils.buildFileExist(e.project)

    override fun actionPerformed(event: AnActionEvent) {
        FileDocumentManager.getInstance().saveAllDocuments()
        event.project?.let { project ->
            project.getComponent(KobaltProjectComponent::class.java)?.let {
/*                DependenciesProcessor().run(it, project) { projectsData ->
                    LOG.info("$projectsData")
                    //                    Modules.configureModules(project, projectsData)
//                    KobaltToolWindowComponent.getInstance(project).update(projectsData)
//                    ApplicationManager.getApplication().invokeLater { BuildModule().run(project,
//                            KobaltApplicationComponent.kobaltJar) }
                }*/
            }
        }
    }
}
