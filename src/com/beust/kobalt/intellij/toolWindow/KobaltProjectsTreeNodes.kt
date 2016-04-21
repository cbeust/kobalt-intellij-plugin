package com.beust.kobalt.intellij.toolWindow

import com.beust.kobalt.intellij.toolWindow.actions.KobaltActionUtil
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleTree
import java.awt.event.InputEvent

/**
 * @author Dmitry Zhuravlev
 *         Date: 18.04.16
 */

sealed class BaseNode(var displayName: String, var parent: BaseNode?) : CachingSimpleNode(parent) {

    abstract val actionId: String?

    override fun getName() = displayName

    override final fun buildChildren() = if (doBuildChildren().isEmpty()) NO_CHILDREN else doBuildChildren().toTypedArray()

    open protected fun doBuildChildren(): List<BaseNode> = emptyList()

    override fun handleDoubleClickOrEnter(tree: SimpleTree?, inputEvent: InputEvent?) {
        val id = actionId
        if (id != null && inputEvent != null) {
            KobaltActionUtil.executeAction(id, inputEvent)
        }
    }

    class TaskNode(displayName: String, parent: BaseNode, override val actionId: String? = "kobalt.RunTask") : BaseNode(displayName, parent)

    class RootNode : BaseNode("", null) {
        override val actionId = null

        val projectNodes = mutableListOf<ProjectNode>()

        fun add(node: ProjectNode) {
            node.parent = this
            projectNodes.add(node)
        }

        fun remove(node: ProjectNode) {
            node.parent = this
            projectNodes.remove(node)
        }

        fun removeAllChildren() = projectNodes.clear()

        override fun doBuildChildren() = projectNodes
    }

    class ProjectNode(displayName: String, parent: BaseNode) : BaseNode(displayName, parent) {

        override val actionId = null

        val taskNodes = listOf(
                TaskNode("clean", this), //TODO better get from server
                TaskNode("compile", this),
                TaskNode("compileTest", this),
                TaskNode("doc", this),
                TaskNode("test", this),
                TaskNode("assemble", this),
                TaskNode("install", this),
                TaskNode("run", this),
                TaskNode("generatePom", this),
                TaskNode("uploadBintray", this),
                TaskNode("uploadGithub", this)
        )

        override fun doBuildChildren() = taskNodes
    }
}


