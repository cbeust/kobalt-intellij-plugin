package com.beust.kobalt.intellij.toolWindow

import com.beust.kobalt.intellij.ProjectData
import com.beust.kobalt.intellij.toolWindow.BaseNode.ModuleNode
import com.beust.kobalt.intellij.toolWindow.BaseNode.RootNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.ui.treeStructure.SimpleTreeBuilder
import com.intellij.ui.treeStructure.SimpleTreeStructure
import javax.swing.tree.DefaultTreeModel

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */

class KobaltProjectsStructure(val project: Project, tree: SimpleTree) : SimpleTreeStructure() {
    val rootNode = RootNode()
    var treeBuilder: SimpleTreeBuilder


    init {
        customizeTree(tree)
        treeBuilder = SimpleTreeBuilder(tree, tree.model as DefaultTreeModel, this, null)
        Disposer.register(project, treeBuilder)
        treeBuilder.initRoot()
        treeBuilder.expand(rootNode, null)
    }

    private fun customizeTree(tree: SimpleTree) {
        tree.isRootVisible = false
        tree.showsRootHandles = true
    }

    fun update(projectData: List<ProjectData>) {
        if (projectData.isNotEmpty()) {
            rootNode.cleanUpCache()
            rootNode.removeAllChildren()
        }
        projectData.map {
            ModuleNode(it.name, rootNode, it.tasks.sortedBy { a -> a.name })
        }.forEach { node ->
            rootNode.add(node)
        }
        update(true)
    }

    fun update(rebuild:Boolean) = treeBuilder.updateFromRoot(rebuild)

    companion object {

        fun <T : BaseNode> getSelectedNodes(tree: SimpleTree, nodeClass: Class<T>) =
                getSelectedNodes(tree).filter { node -> nodeClass.isInstance(node) }.map { it as T }

        private fun getSelectedNodes(tree: SimpleTree): List<SimpleNode> =
                tree.selectionPaths?.map { treePath -> tree.getNodeFor(treePath) } ?: emptyList()


    }

    override fun getRootElement() = rootNode
}