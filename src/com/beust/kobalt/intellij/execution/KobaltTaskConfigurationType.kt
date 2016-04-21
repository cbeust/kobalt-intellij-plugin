package com.beust.kobalt.intellij.execution

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.DefaultJavaProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

/**
 * @author Dmitry Zhuravlev
 *         Date: 19.04.16
 */
class KobaltTaskConfigurationType : ConfigurationType {

    override fun getIcon() = AllIcons.Actions.Execute

    override fun getConfigurationTypeDescription() = "Kobalt build"

    override fun getId() = "KobaltRunTaskConfiguration"

    override fun getDisplayName() = "Kobalt"

    val configurationFactory = object: ConfigurationFactory(this){
        override fun createTemplateConfiguration(project: Project) = KobaltTaskRunConfiguration(project)
    }

    override fun getConfigurationFactories() = arrayOf(configurationFactory)

    companion object {

        fun runConfiguration(project: Project, tasks:List<String>) {
            val type = ConfigurationTypeUtil.findConfigurationType<KobaltTaskConfigurationType>(KobaltTaskConfigurationType::class.java)
            val runner = DefaultJavaProgramRunner.getInstance();
            val executor = DefaultRunExecutor.getRunExecutorInstance()
            val settings = RunManager.getInstance(project).createRunConfiguration("Kobalt $tasks", type.configurationFactory)
            val configuration = settings.configuration as KobaltTaskRunConfiguration
            configuration.tasks = tasks

            runner?.execute(ExecutionEnvironment(executor, runner, settings, project))
        }

   }


}