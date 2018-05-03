package com.beust.kobalt.intellij.configurator

import com.beust.kobalt.intellij.BuildUtils
import com.beust.kobalt.intellij.Constants.Companion.KOBALT_SYSTEM_ID
import com.intellij.ide.actions.OpenFileAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.vfs.WritingAccessProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.01.2017
 */
class KotlinKobaltProjectConfigurator : KotlinProjectConfigurator {
    override val name = "kobalt"
    override val presentableText = "Kobalt"
    override val targetPlatform = JvmPlatform

    override fun addLibraryDependency(module: Module, element: PsiElement, library: ExternalLibraryDescriptor, libraryJarDescriptors: List<LibraryJarDescriptor>) {
        //noop
    }

    override fun changeCoroutineConfiguration(module: Module, state: LanguageFeature.State) {
        //noop
    }

    override fun updateLanguageVersion(module: Module, languageVersion: String?, apiVersion: String?, requiredStdlibVersion: ApiVersion, forTests: Boolean) {
        //noop
    }

    override fun configure(project: Project, excludeModules: Collection<Module>) {
        val dialog = ConfigureDialogWithModulesAndVersion(project, this, excludeModules, "1.0.6")

        dialog.show()
        if (!dialog.isOK) return

        WriteCommandAction.runWriteCommandAction(project) {
            val collector = createConfigureKotlinNotificationCollector(project)
            val buildFile = BuildUtils.buildFile(project)?.toPsiFile(project)
            var configuredAtLeastInOneModule = false
            if (buildFile != null && buildFile is KtFile && canConfigureFile(buildFile)) {
                for (module in dialog.modulesToConfigure) {
                    if(!hasKotlinRuntimeInDependencies(module, buildFile)) {
                        changeBuildFile(buildFile, dialog.kotlinVersion, module, collector)
                        configuredAtLeastInOneModule = true
                    }
                }
                OpenFileAction.openFile(buildFile.virtualFile, project)
            }
            if(configuredAtLeastInOneModule) collector.showNotification()
        }
    }

    private fun changeBuildFile(buildFile: KtFile, kotlinVersion: String, module: Module, collector: NotificationMessageCollector) {
        val projectInitializerExpr = getProjectInitializersMapByName(buildFile)[module.name]
        val dependenciesBlock = projectInitializerExpr?.findCallExpressionByName("dependencies")?.findLambdaBlockExpression()
        dependenciesBlock?.apply {
            KtPsiFactory(this).run {
                add(createNewLine())
                add(createExpression(kotlinStdLibDependencyTemplate(kotlinVersion)))
                collector.addMessage("Dependencies block in ${buildFile.virtualFile.path} was modified")
            }
        }
        collector.addMessage(buildFile.virtualFile.path + " was modified")
    }

    override fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus {
        val module = moduleSourceRootGroup.baseModule
        if (!isKobaltModule(module))
            return ConfigureKotlinStatus.NON_APPLICABLE
        val project = module.project
        val psi = BuildUtils.buildFile(project)?.toPsiFile(project)
        if (psi == null
                || !psi.isValid
                || psi !is KtFile
                || psi.virtualFile == null) {
            return ConfigureKotlinStatus.BROKEN
        }
        if (hasKotlinJvmRuntimeInScope(module) && hasKotlinRuntimeInDependencies(module, psi)) {
            return ConfigureKotlinStatus.CONFIGURED
        }
        return ConfigureKotlinStatus.CAN_BE_CONFIGURED
    }


    companion object {
        const val KOTLIN_STD_LIB_DEFINITION = "org.jetbrains.kotlin:kotlin-stdlib"

        private fun kotlinStdLibDependencyTemplate(kotlinVersion: String)
                = """compile("$KOTLIN_STD_LIB_DEFINITION:$kotlinVersion")"""
    }

    private fun hasKotlinRuntimeInDependencies(module: Module, kotlinBuildFile: KtFile): Boolean {
        val projectInitializerExpr = getProjectInitializersMapByName(kotlinBuildFile)[module.name]
        val dependenciesBlock = projectInitializerExpr?.findCallExpressionByName("dependencies")?.findLambdaBlockExpression()
        return dependenciesBlock?.children?.firstOrNull { dependency -> dependency.text?.contains(KOTLIN_STD_LIB_DEFINITION) ?: false } != null
    }

    private fun canConfigureFile(file: PsiFile) = WritingAccessProvider.isPotentiallyWritable(file.virtualFile, null)

    private fun getProjectInitializersMapByName(psi: KtFile) = psi.declarations.filter { it is KtProperty }
            .map { declaration ->
                val property = declaration as KtProperty
                val initializer = property.initializer
                return@map if ((initializer as? KtCallExpression)?.calleeExpression?.text == "project") {
                    val moduleFacts = initializer.lambdaArguments.firstOrNull()
                            ?.getLambdaExpression()?.bodyExpression
                            ?.children?.filter { it is KtBinaryExpression } as List<KtBinaryExpression>?

                    val moduleName = (moduleFacts?.firstOrNull { it.left?.text == "name" }
                            ?.right as? KtStringTemplateExpression?)?.plainContent

                    if (moduleName != null) moduleName to initializer else null
                } else null
            }.filterNotNull().toMap()

    private fun KtCallExpression.findLambdaBlockExpression() = lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression

    private fun KtCallExpression.findCallExpressionByName(name: String)
            = (lambdaArguments.firstOrNull()
            ?.getLambdaExpression()?.bodyExpression
            ?.children?.filter { it is KtCallExpression } as List<KtCallExpression>?)
            ?.firstOrNull { it.calleeExpression?.text == name }

    private fun isKobaltModule(module: Module) = ExternalSystemApiUtil.isExternalSystemAwareModule(KOBALT_SYSTEM_ID, module)

}