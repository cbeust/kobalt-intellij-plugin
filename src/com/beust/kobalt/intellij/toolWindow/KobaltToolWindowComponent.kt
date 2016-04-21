package com.beust.kobalt.intellij.toolWindow

import com.beust.kobalt.intellij.DependenciesProcessor
import com.beust.kobalt.intellij.KobaltProjectComponent
import com.beust.kobalt.intellij.ProjectData
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.util.DisposeAwareRunnable
import org.jdom.Element
import javax.swing.tree.TreeSelectionModel

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */
@State(name = "KobaltToolWindowComponentState", storages = arrayOf(Storage(StoragePathMacros.WORKSPACE_FILE)))
class KobaltToolWindowComponent(project: Project) : AbstractProjectComponent(project), PersistentStateComponent<KobaltToolWindowComponentState> {

    companion object {
        fun getInstance(project: Project) = project.getComponent(KobaltToolWindowComponent::class.java);
    }

    var componentState: KobaltToolWindowComponentState = KobaltToolWindowComponentState()
    lateinit var projectStructure: KobaltProjectsStructure
    lateinit var tree: SimpleTree
    var isInitialized = false

    override fun getState(): KobaltToolWindowComponentState {
        if (isInitialized) writeState()
        return componentState
    }

    override fun loadState(state: KobaltToolWindowComponentState) {
        componentState = state
        if (isInitialized) {
            readState()
        }
    }

    override fun initComponent() {
        initTree()
        ApplicationManager.getApplication().invokeLater(DisposeAwareRunnable.create({
            val dependencyProcessor = DependenciesProcessor()
            projectStructure = KobaltProjectsStructure(myProject, tree)
            myProject.getComponent(KobaltProjectComponent::class.java)?.let { kobaltComponent ->
                dependencyProcessor.run(kobaltComponent, myProject) { projectsData ->
                    projectStructure.update(projectsData)
                    isInitialized = true
                    readState()
                }
            }

        }, myProject), ModalityState.defaultModalityState())
    }

    fun update(projectData: List<ProjectData>) {
        projectStructure.update(projectData)
    }


    private fun initTree() {
        tree = SimpleTree()
        tree.emptyText.clear()
        tree.selectionModel?.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    }

    private fun readState() {
        if (componentState.treeState != null) {
            try {
                val treeState = TreeState()
                treeState.readExternal(componentState.treeState)
                ApplicationManager.getApplication().invokeLater {
                    treeState.applyTo(tree)
                }

            } catch (e: InvalidDataException) {
                KobaltProjectComponent.LOG.info(e)
            }
        }
    }

    private fun writeState() {
        try {
            componentState.treeState = Element("root")
            TreeState.createOn(tree).writeExternal(componentState.treeState)
        } catch (e: WriteExternalException) {
            KobaltProjectComponent.LOG.warn(e)
        }
    }
}