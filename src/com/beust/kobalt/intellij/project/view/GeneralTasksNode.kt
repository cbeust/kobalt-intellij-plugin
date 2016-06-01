package com.beust.kobalt.intellij.project.view

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.TaskNode
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import icons.ExternalSystemIcons

/**
 * Class copied from default [TasksNode] implementation for disabling checking if dataNode parent is Project.
 * This will allow to create top-level tasks for the given project.
 * @see com.intellij.openapi.externalSystem.view.TasksNode
 *
 * @author Dmitry Zhuravlev
 *         Date:  31.05.2016
 */
@Order(1)
class GeneralTasksNode
constructor(externalProjectsView: ExternalProjectsView, dataNodes: Collection<DataNode<*>>?) : ExternalSystemNode<Any>(externalProjectsView, null, null) {

    private val myTasksMap = MultiMap<String, TaskNode>()

    init {
        if (dataNodes != null && !dataNodes.isEmpty()) {
            for (dataNode in dataNodes) {
                val data = dataNode.data
                if (data !is TaskData) continue
                if(data.name.contains(":")) continue
                var group = data.group
                if (group == null) group = "other"
                @Suppress("UNCHECKED_CAST")
                myTasksMap.putValue(StringUtil.toLowerCase(group), TaskNode(externalProjectsView, dataNode as DataNode<TaskData>))
            }
        }
    }

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        presentation.setIcon(ExternalSystemIcons.TaskGroup)
    }

    override fun getName(): String {
        return "Tasks"
    }

    override fun isVisible(): Boolean {
        return super.isVisible() && hasChildren()
    }

    @Suppress("UNCHECKED_CAST")
    override fun doBuildChildren(): List<ExternalSystemNode<Any>> {
        val result = ContainerUtil.newArrayList<ExternalSystemNode<Any>>()
        val isGroup = externalProjectsView.groupTasks
        if (isGroup) {
            for (collectionEntry in myTasksMap.entrySet()) {
                val group = ObjectUtils.notNull(collectionEntry.key, "other")
                val tasksGroupNode = object : ExternalSystemNode<Any>(externalProjectsView, null, null) {

                    override fun update(presentation: PresentationData) {
                        super.update(presentation)
                        presentation.setIcon(ExternalSystemIcons.TaskGroup)
                    }

                    override fun getName(): String {
                        return group
                    }

                    override fun isVisible(): Boolean {
                        return super.isVisible() && hasChildren()
                    }

                    override operator fun compareTo(node: ExternalSystemNode<Any>): Int {
                        return if ("other" == group) 1 else super.compareTo(node)
                    }
                }
                tasksGroupNode.addAll(collectionEntry.value)
                result.add(tasksGroupNode)
            }
        } else {
            result.addAll(myTasksMap.values() as Collection<ExternalSystemNode<Any>>)
        }
        return result
    }
}

