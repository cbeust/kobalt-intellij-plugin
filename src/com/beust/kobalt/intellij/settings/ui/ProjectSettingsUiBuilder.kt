package com.beust.kobalt.intellij.settings.ui

import com.beust.kobalt.intellij.BuildUtils.latestKobaltVersionOrDefault
import com.beust.kobalt.intellij.Constants
import com.beust.kobalt.intellij.Constants.Companion.MIN_KOBALT_VERSION
import com.beust.kobalt.intellij.DistributionDownloader
import com.beust.kobalt.intellij.KFiles
import com.beust.kobalt.intellij.settings.KobaltProjectSettings
import com.intellij.openapi.externalSystem.model.settings.LocationSettingType
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil
import com.intellij.openapi.externalSystem.util.PaintAwarePanel
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.io.File
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * @author Dmitry Zhuravlev
 *         Date: 27.04.16
 */
class ProjectSettingsUIBuilder(val initialSettings: KobaltProjectSettings) {

    lateinit private var myKobaltHomeLabel: JBLabel
    lateinit private var myKobaltProfilesLabel: JBLabel
    lateinit var myKobaltHomePathField: TextFieldWithBrowseButton
    lateinit var myKobaltProfilesField: JBTextField
    lateinit var myKobaltAutoDownloadBox: JBCheckBox
    lateinit var myKobaltDownloadSourcesBox: JBCheckBox

    fun createAndFillControls(content: PaintAwarePanel, indentLevel: Int) {
        addKobaltHomeComponents(content, indentLevel)
    }

    fun applySettings(settings: KobaltProjectSettings) {
        FileUtil.toCanonicalPath(myKobaltHomePathField.text).let { kobaltHomePath ->
            if (kobaltHomePath.isNotEmpty()) {
                settings.kobaltHome = kobaltHomePath
            }
        }
        settings.autoDownloadKobalt = myKobaltAutoDownloadBox.isSelected
        settings.downloadSources = myKobaltDownloadSourcesBox.isSelected
        settings.profiles = myKobaltProfilesField.text
    }

    fun validate(project: Project?, kobaltProjectSettings: KobaltProjectSettings): Boolean {
        if (myKobaltAutoDownloadBox.isSelected) {
            DistributionDownloader.downloadAndInstallKobaltJarSynchronously(project, latestKobaltVersionOrDefault(MIN_KOBALT_VERSION), false)
        }
        val kobaltHome = myKobaltHomePathField.text
        if (kobaltHome == null || !File(kobaltHome).exists()) {
            DelayedBalloonInfo(MessageType.ERROR, LocationSettingType.UNKNOWN, 0).run()
            throw ConfigurationException("Kobalt location is incorrect!")
        }
        return true
    }

    fun isExtraSettingModified() =
            initialSettings.kobaltHome != FileUtil.toCanonicalPath(myKobaltHomePathField.text)
                    || initialSettings.autoDownloadKobalt != myKobaltAutoDownloadBox.isSelected
                    || initialSettings.downloadSources != myKobaltDownloadSourcesBox.isSelected
                    || initialSettings.profiles != null && initialSettings.profiles != myKobaltProfilesField.text
                    || initialSettings.profiles == null && myKobaltProfilesField.text.isNotBlank()



    fun reset(defaultModuleCreation: Boolean) {
        val kobaltHome = initialSettings.kobaltHome
        myKobaltHomePathField.text = kobaltHome ?: KFiles.kobaltHomeDir(latestKobaltVersionOrDefault(MIN_KOBALT_VERSION))
        myKobaltHomePathField.textField.foreground = LocationSettingType.DEDUCED.color
        myKobaltAutoDownloadBox.isSelected = initialSettings.autoDownloadKobalt ?: true
        myKobaltDownloadSourcesBox.isSelected = initialSettings.downloadSources ?: false
        myKobaltProfilesField.text = initialSettings.profiles ?: ""
    }


    fun update(linkedProjectPath: String?, defaultModuleCreation: Boolean) {
        //TODO
    }


    fun addKobaltHomeComponents(content: PaintAwarePanel, indentLevel: Int): ProjectSettingsUIBuilder {
        myKobaltHomeLabel = JBLabel("Kobalt home:")
        myKobaltHomePathField = TextFieldWithBrowseButton()
        myKobaltProfilesLabel = JBLabel("Active profiles:")
        myKobaltProfilesField = JBTextField().apply { toolTipText = "Profiles names separated by a comma" }
        myKobaltAutoDownloadBox = JBCheckBox("Always download and apply new versions of Kobalt")
        myKobaltDownloadSourcesBox = JBCheckBox("Always download sources and javadoc for dependencies")
        myKobaltAutoDownloadBox.addItemListener({
            myKobaltHomePathField.isEditable = !myKobaltAutoDownloadBox.isSelected
            myKobaltHomePathField.isEnabled = !myKobaltAutoDownloadBox.isSelected
        })
        myKobaltHomePathField.addBrowseFolderListener("", "Kobalt home:",
                null, FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT, false)
        myKobaltHomePathField.textField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                myKobaltHomePathField.textField.foreground = LocationSettingType.EXPLICIT_CORRECT.color
            }

            override fun removeUpdate(e: DocumentEvent) {
                myKobaltHomePathField.textField.foreground = LocationSettingType.EXPLICIT_CORRECT.color
            }

            override fun changedUpdate(e: DocumentEvent) {
            }
        })
        content.add(myKobaltHomeLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(myKobaltHomePathField, ExternalSystemUiUtil.getFillLineConstraints(0))
        content.add(myKobaltProfilesLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(myKobaltProfilesField, ExternalSystemUiUtil.getFillLineConstraints(0))
        content.add(myKobaltAutoDownloadBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        content.add(myKobaltDownloadSourcesBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel))
        return this
    }

    fun showUi(show: Boolean) {
        ExternalSystemUiUtil.showUi(this, show)
    }

    fun disposeUIResources() {
        ExternalSystemUiUtil.disposeUi(this)
    }

    private inner class DelayedBalloonInfo internal constructor(private val myMessageType: MessageType, settingType: LocationSettingType, delayMillis: Long) : Runnable {
        private val myText: String
        private val myTriggerTime: Long

        init {
            myText = settingType.getDescription(Constants.KOBALT_SYSTEM_ID)
            myTriggerTime = System.currentTimeMillis() + delayMillis
        }

        override fun run() {
            val diff = myTriggerTime - System.currentTimeMillis()
            if (diff > 0) {
                return
            }
            if (!myKobaltHomePathField.isShowing) {
                // Don't schedule the balloon if the configurable is hidden.
                return
            }
            ExternalSystemUiUtil.showBalloon(myKobaltHomePathField, myMessageType, myText)
        }
    }


}