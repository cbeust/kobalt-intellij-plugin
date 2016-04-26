package com.beust.kobalt.intellij.data

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.module.StdModuleTypes

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltModuleData(
         id: String,
         externalName: String,
         internalName: String,
         moduleFileDirectoryPath: String,
         externalConfigPath: String
) : ModuleData(id, Constants.KOBALT_SYSTEM_ID, StdModuleTypes.JAVA.id, externalName, internalName, moduleFileDirectoryPath, externalConfigPath) {

    val KEY = Key.create<KobaltModuleData>(KobaltModuleData::class.java, ProjectKeys.MODULE.processingWeight + 1)

}