package com.beust.kobalt.intellij.project.wizard

import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.frameworkSupport.KobaltBuildScriptBuilder
import com.beust.kobalt.intellij.settings.KobaltProjectSettings
import com.beust.kobalt.intellij.settings.KobaltProjectSettingsControl
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.util.EditorHelper
import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.*
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.containers.ContainerUtil
import java.io.File
import java.io.IOException

class KobaltModuleBuilder : AbstractExternalModuleBuilder<KobaltProjectSettings>(Constants.KOBALT_SYSTEM_ID, KobaltProjectSettings()) {

    var projectId: ProjectId? = null
    var myParentProject: ProjectData? = null

    private lateinit var myWizardContext: WizardContext
    private lateinit var rootProjectPath: String

    override fun createModule(moduleModel: ModifiableModuleModel): Module {
        LOG.assertTrue(name != null)
        val originModuleFilePath = moduleFilePath
        LOG.assertTrue(originModuleFilePath != null)

        val moduleName = if (projectId == null) name else projectId!!.artifactId
        val moduleFilePath = myWizardContext.projectFileDirectory + "/.idea/modules/" + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION
        ModuleBuilder.deleteModuleFile(moduleFilePath)
        val moduleType = moduleType
        val module = moduleModel.newModule(moduleFilePath, moduleType.id)
        setupModule(module)
        return module
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val contentEntryPath = contentEntryPath
        if (StringUtil.isEmpty(contentEntryPath)) {
            return
        }
        val contentRootDir = File(contentEntryPath)
        FileUtilRt.createDirectory(contentRootDir)
        val modelContentRootDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(contentRootDir) ?: return

        modifiableRootModel.addContentEntry(modelContentRootDir)

        if (myJdk != null) {
            modifiableRootModel.sdk = myJdk
        } else {
            modifiableRootModel.inheritSdk()
        }

        val project = modifiableRootModel.project
        if (myParentProject != null) {
            rootProjectPath = myParentProject!!.linkedExternalProjectPath
        } else {
            rootProjectPath = FileUtil.toCanonicalPath(if (myWizardContext.isCreatingNewProject) project.basePath else modelContentRootDir.path)
        }

        if (projectId != null && contentEntryPath != null) {
            modifiableRootModel.module.putUserData(BUILD_SCRIPT_BUILDER,
                    KobaltBuildScriptBuilder(projectId!!, modelContentRootDir.path))
        }
    }


    override fun setupModule(module: Module) {
        super.setupModule(module)

        var buildScriptFile: VirtualFile? = null
        val buildScriptBuilder = getBuildScriptBuilder(module)
        try {
            if (buildScriptBuilder != null) {
                buildScriptFile = setupKobaltBuildFile(buildScriptBuilder.contentRootDir, buildScriptBuilder)
            }
        } catch (e: IOException) {
            LOG.warn("Unexpected exception on applying frameworks templates", e)
        }

        val project = module.project
        if (myWizardContext.isCreatingNewProject) {
            externalProjectSettings.externalProjectPath = rootProjectPath
            val settings = ExternalSystemApiUtil.getSettings(project, Constants.KOBALT_SYSTEM_ID)
            project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, java.lang.Boolean.TRUE)
            //noinspection unchecked
            settings.linkProject(externalProjectSettings)
        } else {
            FileDocumentManager.getInstance().saveAllDocuments()
            val kobaltProjectSettings = externalProjectSettings
            val finalBuildScriptFile = buildScriptFile
            val runnable = Runnable {
                if (myParentProject == null) {
                    kobaltProjectSettings.externalProjectPath = rootProjectPath
                    val settings = ExternalSystemApiUtil.getSettings(project, Constants.KOBALT_SYSTEM_ID)
                    //noinspection unchecked
                    settings.linkProject(kobaltProjectSettings)
                }

                ExternalSystemUtil.refreshProject(
                        project, Constants.KOBALT_SYSTEM_ID, rootProjectPath, false,
                        ProgressExecutionMode.IN_BACKGROUND_ASYNC)

                reformatAndOpenBuildFileInEditor(finalBuildScriptFile, project)
            }

            // execute when current dialog is closed
            ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL, runnable)
        }
        ApplicationManager.getApplication().invokeLater { reformatAndOpenBuildFileInEditor(buildScriptFile, project) }
    }

    private fun reformatAndOpenBuildFileInEditor(buildScriptFile: VirtualFile?, project: Project) {
        buildScriptFile?.let {
            PsiManager.getInstance(project).findFile(buildScriptFile)?.let {
                ReformatCodeProcessor(it, false).run()
                EditorHelper.openInEditor(it)
            }
        }
    }

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        myWizardContext = wizardContext
        return arrayOf(KobaltModuleWizardStep(this, wizardContext), ExternalModuleSettingsStep(
                wizardContext, this, KobaltProjectSettingsControl(externalProjectSettings)))
    }


    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable)
            = KobaltFrameworksWizardStep(context, this).apply { Disposer.register(parentDisposable, this) }


    override fun isSuitableSdkType(sdk: SdkTypeId?): Boolean {
        return sdk is JavaSdkType
    }

    override fun getParentGroup(): String {
        return JavaModuleType.BUILD_TOOLS_GROUP
    }

    override fun getWeight(): Int {
        return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT
    }

    override fun getModuleType(): ModuleType<*> {
        return StdModuleTypes.JAVA
    }

    private fun setupKobaltBuildFile(modelContentRootDir: String, scriptBuilder: KobaltBuildScriptBuilder): VirtualFile? {
        val file = getOrCreateExternalProjectConfigFile(modelContentRootDir, Constants.BUILD_FILE)

        if (file != null) {
            val attributes = ContainerUtil.newHashMap<String, String>()
            if (projectId != null) {
                attributes.put(TEMPLATE_ATTRIBUTE_MODULE_VERSION, projectId!!.version)
                attributes.put(TEMPLATE_ATTRIBUTE_MODULE_GROUP, projectId!!.groupId)
                attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, projectId!!.artifactId)
                attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, projectId!!.artifactId)
                attributes.put(TEMPLATE_ATTRIBUTE_MODULE_BODY, scriptBuilder.buildBody())
            }
            saveFile(file, DEFAULT_TEMPLATE_KOBALT_BUILD, attributes)
        }
        return file
    }

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep? {
        if (settingsStep is ProjectSettingsStep) {
            if (projectId != null) {
                settingsStep.moduleNameField.text = projectId!!.artifactId
                settingsStep.setModuleName(projectId!!.artifactId)
            }
            settingsStep.bindModuleSettings()
        }
        return super.modifySettingsStep(settingsStep)
    }

    companion object {

        private val LOG = Logger.getInstance(KobaltModuleBuilder::class.java)

        private val DEFAULT_TEMPLATE_KOBALT_BUILD = "Kobalt Build Script.kobalt"

        private val TEMPLATE_ATTRIBUTE_MODULE_PATH = "MODULE_PATH"
        private val TEMPLATE_ATTRIBUTE_MODULE_NAME = "MODULE_NAME"
        private val TEMPLATE_ATTRIBUTE_MODULE_GROUP = "MODULE_GROUP"
        private val TEMPLATE_ATTRIBUTE_MODULE_VERSION = "MODULE_VERSION"
        private val TEMPLATE_ATTRIBUTE_MODULE_BODY = "MODULE_BODY"
        private val BUILD_SCRIPT_BUILDER = Key.create<KobaltBuildScriptBuilder>("kobalt.module.buildScriptBuilder")


        private fun saveFile(file: VirtualFile, templateName: String, templateAttributes: Map<String, String>?) {
            val manager = FileTemplateManager.getDefaultInstance()
            val template = manager.getInternalTemplate(templateName)
            try {
                VfsUtil.saveText(file, if (templateAttributes != null) template.getText(templateAttributes) else template.text)
            } catch (e: IOException) {
                LOG.warn(String.format("Unexpected exception on applying template %s config", Constants.KOBALT_SYSTEM_ID.readableName), e)
                throw ConfigurationException(
                        e.message, String.format("Can't apply %s template config text", Constants.KOBALT_SYSTEM_ID.readableName))
            }

        }

        private fun getOrCreateExternalProjectConfigFile(parent: String, fileName: String): VirtualFile? {
            val file = File(parent, fileName)
            FileUtilRt.createIfNotExists(file)
            return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        }


        fun getBuildScriptBuilder(module: Module?): KobaltBuildScriptBuilder? {
            return module?.getUserData(BUILD_SCRIPT_BUILDER)
        }
    }


}
