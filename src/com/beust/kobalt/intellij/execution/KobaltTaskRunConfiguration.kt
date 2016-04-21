package com.beust.kobalt.intellij.execution

import com.beust.kobalt.intellij.KobaltProjectComponent
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager

/**
 * @author Dmitry Zhuravlev
 *         Date: 19.04.16
 */
class KobaltTaskRunConfiguration(project: Project, var tasks: List<String> = emptyList()) : LocatableConfigurationBase(project,
        findConfigurationType<KobaltTaskConfigurationType>(KobaltTaskConfigurationType::class.java).configurationFactory,
        "TODO NAME") {

    val kobaltComponent = project.getComponent(KobaltProjectComponent::class.java)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        throw UnsupportedOperationException()
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment) = object : JavaCommandLineState(env) {
        override fun createJavaParameters() = createJavaParametersForTaskRun(kobaltComponent)

    }

    fun createJavaParametersForTaskRun(kobaltComponent: KobaltProjectComponent) = JavaParameters().apply {
        configureByProject(project, JavaParameters.JDK_ONLY, ProjectRootManager.getInstance(project).projectSdk);
        workingDirectory = project.basePath
        mainClass = "com.beust.kobalt.wrapper.Main"
        classPath.add(kobaltComponent.kobaltJar.toFile())
        for (task in tasks)
            programParametersList.add(task)
    }


}