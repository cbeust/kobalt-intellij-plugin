package com.beust.kobalt.intellij.project.view

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ExternalSystemViewContributor
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap

/**
 * Copied from [ExternalSystemViewDefaultContributor].
 * Enable adding of [GeneralTasksNode].
 * @see com.intellij.openapi.externalSystem.view.ExternalSystemViewDefaultContributor
 *
 * @author Dmitry Zhuravlev
 *         Date:  31.05.2016
 */
class KobaltViewContributor : ExternalSystemViewContributor() {
    override fun getKeys() = listOf(ProjectKeys.TASK)

    override fun getSystemId() = Constants.KOBALT_SYSTEM_ID

    override fun createNodes(externalProjectsView: ExternalProjectsView, dataNodes: MultiMap<Key<*>, DataNode<*>>): MutableList<ExternalSystemNode<*>> {
        val result = SmartList<ExternalSystemNode<*>>()
//         add tasks
        val tasksNodes = dataNodes.get(ProjectKeys.TASK)
        if (!tasksNodes.isEmpty()) {
            val tasksNode = GeneralTasksNode(externalProjectsView, tasksNodes)
            if (externalProjectsView.useTasksNode()) {
                result.add(tasksNode)
            } else {
                ContainerUtil.addAll<ExternalSystemNode<*>, ExternalSystemNode<*>, List<ExternalSystemNode<*>>>(result, *tasksNode.children)
            }
        }
        return result
    }
}