package com.beust.kobalt.intellij.resolver;

import com.beust.kobalt.intellij.Constants;
import com.beust.kobalt.intellij.settings.KobaltExecutionSettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Zhuravlev
 *         Date: 26.04.16
 */
public class KobaltProjectResolver implements ExternalSystemProjectResolver<KobaltExecutionSettings> {
    private static final Logger LOG = Logger.getInstance("#" + KobaltProjectResolver.class.getName());
    @Nullable
    @Override
    public DataNode<ProjectData> resolveProjectInfo(@NotNull ExternalSystemTaskId id, @NotNull String projectPath, boolean isPreviewMode, @Nullable KobaltExecutionSettings settings, @NotNull ExternalSystemTaskNotificationListener listener) throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
        //TODO
        LOG.info("Project resolved");
        return new DataNode<>(ProjectKeys.PROJECT, new ProjectData(Constants.KOBALT_SYSTEM_ID,"TODO external name","TODO ide project path path","TODO linked external path"), null);
    }

    @Override
    public boolean cancelTask(@NotNull ExternalSystemTaskId taskId, @NotNull ExternalSystemTaskNotificationListener listener) {
        //TODO
        return true;
    }
}
