package com.beust.kobalt.intellij.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.treeStructure.SimpleTree

/**
 * @author Dmitry Zhuravlev
 *         Date: 16.04.16
 */
class KobaltToolWindowPanel constructor(val project: Project, val tree: SimpleTree) : SimpleToolWindowPanel(true, true), DataProvider, Disposable {

    init {
        val actionManager = ActionManager.getInstance()
        val actionToolbar = actionManager.createActionToolbar("Kobalt Tool Window Toolbar",
                actionManager.getAction("kobalt.ToolWindowToolbar") as DefaultActionGroup,
                true)
        actionToolbar.setTargetComponent(tree)
        setToolbar(actionToolbar.component);
        setContent(ScrollPaneFactory.createScrollPane(tree));
    }

    override fun getData(dataId: String?): Any? {
        if (KobaltDataKeys.KOBALT_TASKS.`is`(dataId)) return extractTasks()
        return super.getData(dataId)
    }

    private fun extractTasks() = KobaltProjectsStructure.getSelectedNodes(tree, BaseNode.TaskNode::class.java).map {node->
        val parentName = node.parent?.name
        if(parentName!=null) "$parentName:${node.name}" else "${node.name}"
    }

    override fun dispose() {
        //TODO
    }

}