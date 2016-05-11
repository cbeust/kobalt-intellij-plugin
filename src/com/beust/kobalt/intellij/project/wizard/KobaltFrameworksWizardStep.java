
package com.beust.kobalt.intellij.project.wizard;

import com.beust.kobalt.intellij.frameworkSupport.KobaltFrameworkSupportProvider;
import com.beust.kobalt.intellij.frameworkSupport.KobaltJavaFrameworkSupportProvider;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.newProjectWizard.AddSupportForFrameworksPanel;
import com.intellij.ide.util.newProjectWizard.impl.FrameworkSupportModelBase;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;


public class KobaltFrameworksWizardStep extends ModuleWizardStep implements Disposable {

  private JPanel myPanel;
  private final AddSupportForFrameworksPanel myFrameworksPanel;
  private JPanel myFrameworksPanelPlaceholder;
  private JPanel myOptionsPanel;
  @SuppressWarnings("unused") private JBLabel myFrameworksLabel;

  public KobaltFrameworksWizardStep(WizardContext context, final KobaltModuleBuilder builder) {

    Project project = context.getProject();
    final LibrariesContainer container = LibrariesContainerFactory.createContainer(context.getProject());
    FrameworkSupportModelBase model = new FrameworkSupportModelBase(project, null, container) {
      @NotNull
      @Override
      public String getBaseDirectoryForLibrariesPath() {
        return StringUtil.notNullize(builder.getContentEntryPath());
      }
    };

    myFrameworksPanel =
      new AddSupportForFrameworksPanel(Collections.<FrameworkSupportInModuleProvider>emptyList(), model, true, null);

    List<FrameworkSupportInModuleProvider> providers = ContainerUtil.newArrayList();
    Collections.addAll(providers, KobaltFrameworkSupportProvider.EP_NAME.getExtensions());

    myFrameworksPanel.setProviders(providers, Collections.emptySet(), Collections.singleton(
            KobaltJavaFrameworkSupportProvider.ID));
    Disposer.register(this, myFrameworksPanel);
    myFrameworksPanelPlaceholder.add(myFrameworksPanel.getMainPanel());

    ModuleBuilder.ModuleConfigurationUpdater configurationUpdater = new ModuleBuilder.ModuleConfigurationUpdater() {
      @Override
      public void update(@NotNull Module module, @NotNull ModifiableRootModel rootModel) {
        myFrameworksPanel.addSupport(module, rootModel);
      }
    };
    builder.addModuleConfigurationUpdater(configurationUpdater);

    ((CardLayout)myOptionsPanel.getLayout()).show(myOptionsPanel, "frameworks card");
  }

  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Override
  public void updateDataModel() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public void disposeUIResources() {
    Disposer.dispose(this);
  }
}
