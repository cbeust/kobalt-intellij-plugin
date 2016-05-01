package com.beust.kobalt.intellij.import

import com.beust.kobalt.intellij.Constants
import com.intellij.externalSystem.JavaProjectData
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.vfs.LocalFileSystem
import icons.KobaltIcons
import java.io.File

/**
 * @author Dmitry Zhuravlev
 *         Date:  27.04.2016
 */
class KobaltProjectImportBuilder(dataManager: ProjectDataManager)
: AbstractExternalProjectImportBuilder<ImportFromKobaltControl>(dataManager, ImportFromKobaltControl(), Constants.KOBALT_SYSTEM_ID) {

    override fun getIcon() = KobaltIcons.Kobalt

    override fun getName() = "Kobalt"

    override fun doPrepare(context: WizardContext) {
        var pathToUse = fileToImport
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(pathToUse)
        if (file != null && !file.isDirectory) {
            pathToUse = file.parent?.parent?.parent?.path   //we should remove "/kobalt/src/Build.kt" to get project root
        }

        val importFromKobaltControl = getControl(context.project)
        importFromKobaltControl.setLinkedProjectPath(pathToUse)
        /*val sdkPair = ExternalSystemJdkUtil.getAvailableJdk(context.project)
        if (sdkPair != null && ExternalSystemJdkUtil.USE_INTERNAL_JAVA != sdkPair.first) {
            importFromKobaltControl.projectSettings.setKobaltJvm(sdkPair.first)
        }*/
    }

    override fun beforeCommit(dataNode: DataNode<ProjectData>, project: Project) {
        val javaProjectNode = ExternalSystemApiUtil.find(dataNode, JavaProjectData.KEY) ?: return

        val externalLanguageLevel = javaProjectNode.data.languageLevel
        val languageLevelExtension = LanguageLevelProjectExtension.getInstance(project)
        if (externalLanguageLevel != languageLevelExtension.languageLevel) {
            languageLevelExtension.languageLevel = externalLanguageLevel
        }
    }

    override fun applyExtraSettings(context: WizardContext) {
        val node = externalProjectNode ?: return

        val javaProjectNode = ExternalSystemApiUtil.find(node, JavaProjectData.KEY)
        if (javaProjectNode != null) {
            val data = javaProjectNode.data
            context.compilerOutputDirectory = data.compileOutputPath
            val version = data.jdkVersion
            val jdk = findJdk(version)
            if (jdk != null) {
                context.projectJdk = jdk
            }
        }
    }

    override fun getExternalProjectConfigToUse(file: File) = if (file.isDirectory) file else file.parentFile;

    private fun findJdk(version: JavaSdkVersion): Sdk? {
        val javaSdk = JavaSdk.getInstance()
        val javaSdks = ProjectJdkTable.getInstance().getSdksOfType(javaSdk)
        var candidate: Sdk? = null
        for (sdk in javaSdks) {
            val v = javaSdk.getVersion(sdk)
            if (v == version) {
                return sdk
            } else if (candidate == null && v != null && version.maxLanguageLevel.isAtLeast(version.maxLanguageLevel)) {
                candidate = sdk
            }
        }
        return candidate
    }

    override fun isSuitableSdkType(sdkType: SdkTypeId?) = sdkType === JavaSdk.getInstance();
}