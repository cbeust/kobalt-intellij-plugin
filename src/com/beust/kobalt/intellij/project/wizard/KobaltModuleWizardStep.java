
package com.beust.kobalt.intellij.project.wizard;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;


public class KobaltModuleWizardStep extends ModuleWizardStep {

  private static final String DEFAULT_VERSION = "1.0-SNAPSHOT";

  @Nullable
  private final Project myProjectOrNull;
  @NotNull
  private final KobaltModuleBuilder myBuilder;
  @NotNull
  private final WizardContext myContext;

  private JPanel myMainPanel;

  private JTextField myGroupIdField;
  private JTextField myArtifactIdField;
  private JTextField myVersionField;

  public KobaltModuleWizardStep(@NotNull KobaltModuleBuilder builder, @NotNull WizardContext context) {
    myProjectOrNull = context.getProject();
    myBuilder = builder;
    myContext = context;
    initComponents();
  }

  private void initComponents() {
    ActionListener updatingListener = e -> updateComponents();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myGroupIdField;
  }

  @Override
  public void onStepLeaving() {
    saveSettings();
  }


  private void saveSettings() {
  }

  private static boolean getSavedValue(String key, boolean defaultValue) {
    return getSavedValue(key, String.valueOf(defaultValue)).equals(String.valueOf(true));
  }

  private static String getSavedValue(String key, String defaultValue) {
    String value = PropertiesComponent.getInstance().getValue(key);
    return value == null ? defaultValue : value;
  }

  private static void saveValue(String key, boolean value) {
    saveValue(key, String.valueOf(value));
  }

  private static void saveValue(String key, String value) {
    PropertiesComponent.getInstance().setValue(key, value);
  }

  public JComponent getComponent() {
    return myMainPanel;
  }

  @Override
  public boolean validate() throws ConfigurationException {
    if (StringUtil.isEmptyOrSpaces(myArtifactIdField.getText())) {
      ApplicationManager.getApplication().invokeLater(
              () -> IdeFocusManager.getInstance(myProjectOrNull).requestFocus(myArtifactIdField, true));
      throw new ConfigurationException("Please, specify artifactId");
    }

    return true;
  }

  @Override
  public void updateStep() {
    ProjectId projectId = myBuilder.getProjectId();

    if (projectId == null) {
      setTestIfEmpty(myArtifactIdField, myBuilder.getName());
    }
    else {
      setTestIfEmpty(myArtifactIdField, projectId.getArtifactId());
      setTestIfEmpty(myGroupIdField, projectId.getGroupId());
      setTestIfEmpty(myVersionField, projectId.getVersion());
    }

    updateComponents();
  }


  private void updateComponents() {
      myContext.putUserData(ExternalModuleSettingsStep.SKIP_STEP_KEY, Boolean.FALSE);
      myGroupIdField.setEnabled(true);
      myVersionField.setEnabled(true);

      setTestIfEmpty(myArtifactIdField, myBuilder.getName());
      setTestIfEmpty(myGroupIdField, "");
      setTestIfEmpty(myVersionField, DEFAULT_VERSION);
  }

  @Override
  public void updateDataModel() {
    myContext.setProjectBuilder(myBuilder);

    myBuilder.setProjectId(new ProjectId(myGroupIdField.getText(),
                                         myArtifactIdField.getText(),
                                         myVersionField.getText()));

    if (StringUtil.isNotEmpty(myBuilder.getProjectId().getArtifactId())) {
      myContext.setProjectName(myBuilder.getProjectId().getArtifactId());
    }
      if (myProjectOrNull != null) {
        myContext.setProjectFileDirectory(myProjectOrNull.getBaseDir().getPath() + '/' + myContext.getProjectName());
      }
  }

  private static void setTestIfEmpty(@NotNull JTextField field, @Nullable String text) {
    if (StringUtil.isEmpty(field.getText())) {
      field.setText(StringUtil.notNullize(text));
    }
  }
}

