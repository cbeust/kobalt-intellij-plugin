package com.beust.kobalt.intellij.settings.ui

import com.beust.kobalt.intellij.KFiles
import com.intellij.openapi.externalSystem.model.settings.LocationSettingType
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil
import com.intellij.openapi.externalSystem.util.PaintAwarePanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel

/**
 * @author Dmitry Zhuravlev
 *         Date: 27.04.16
 */
class ProjectSettingsUIBuilder {

    lateinit private var myKobaltHomeLabel: JBLabel
    lateinit private var myKobaltHomePathField: TextFieldWithBrowseButton

    fun createAndFillControls(content: PaintAwarePanel, indentLevel: Int) {
        addKobaltHomeComponents(content, indentLevel)
    }

    fun addKobaltHomeComponents(content: PaintAwarePanel, indentLevel: Int): ProjectSettingsUIBuilder {
        myKobaltHomeLabel = JBLabel("Kobalt home:")
        myKobaltHomePathField = TextFieldWithBrowseButton()
        myKobaltHomePathField.text = KFiles.kobaltHomeDir
        myKobaltHomePathField.textField.foreground = LocationSettingType.DEDUCED.color
        content.add(myKobaltHomeLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel))
        content.add(myKobaltHomePathField, ExternalSystemUiUtil.getFillLineConstraints(0))
        return this
    }

    fun showUi(show: Boolean) {
        ExternalSystemUiUtil.showUi(this, show)
    }

    fun disposeUIResources() {
        ExternalSystemUiUtil.disposeUi(this)
    }
}