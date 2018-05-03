package com.beust.kobalt.intellij.config

import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.util.Comparing

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltLibraryProperties(val version: String?) : LibraryProperties<KobaltLibraryProperties>() {

    override fun getState() = null

    override fun loadState(properties: KobaltLibraryProperties) {
    }

    override fun hashCode() = if (version != null) version.hashCode() else 0;


    override fun equals(obj: Any?) = obj is KobaltLibraryProperties && Comparing.equal(version, obj.version);
}