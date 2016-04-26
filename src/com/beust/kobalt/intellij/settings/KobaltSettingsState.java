package com.beust.kobalt.intellij.settings;

import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.xmlb.annotations.AbstractCollection;

import java.util.Set;

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
public class KobaltSettingsState implements AbstractExternalSystemSettings.State<KobaltProjectSettings> {

    private Set<KobaltProjectSettings> projectSettings = ContainerUtilRt.newTreeSet();

    @AbstractCollection(surroundWithTag = false, elementTypes = {KobaltProjectSettings.class})
    @Override
    public Set<KobaltProjectSettings> getLinkedExternalProjectsSettings() {
        return projectSettings;
    }

    @Override
    public void setLinkedExternalProjectsSettings(Set<KobaltProjectSettings> settings) {
        if (settings != null) {
            projectSettings.addAll(settings);
        }
    }
}
