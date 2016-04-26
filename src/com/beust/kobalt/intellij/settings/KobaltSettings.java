package com.beust.kobalt.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author Dmitry Zhuravlev
 *         Date: 26.04.16
 */
@State(name = "KobaltSettings", storages = @Storage("kobalt.xml"))
public class KobaltSettings extends AbstractExternalSystemSettings<KobaltSettings, KobaltProjectSettings, KobaltSettingsListener>
        implements PersistentStateComponent<KobaltSettings.KobaltSettingsState> {

    private final KobaltSystemSettings systemSettings;

    public KobaltSettings(@NotNull Project project) {
        super(KobaltSettingsListener.Companion.getTOPIC(), project);
        systemSettings = KobaltSystemSettings.Companion.getInstance();
    }

    @NotNull
    public static KobaltSettings getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, KobaltSettings.class);
    }

    @Override
    public void subscribe(@NotNull ExternalSystemSettingsListener<KobaltProjectSettings> listener) {
        getProject().getMessageBus().connect(getProject()).subscribe(KobaltSettingsListener.Companion.getTOPIC(),
                new DelegatingKobaltSettingsListenerAdapter(listener));
    }

    @Override
    protected void copyExtraSettingsFrom(@NotNull KobaltSettings settings) {
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public KobaltSettings.KobaltSettingsState getState() {
        KobaltSettingsState state = new KobaltSettingsState();
        fillState(state);
        return state;
    }

    @Override
    public void loadState(KobaltSettingsState state) {
        super.loadState(state);
    }




    @Override
    protected void checkSettings(@NotNull KobaltProjectSettings old, @NotNull KobaltProjectSettings current) {

    }

 
    public static class KobaltSettingsState implements State<KobaltProjectSettings> {

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
}
