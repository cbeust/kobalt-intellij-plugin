package com.beust.kobalt.intellij

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.jetbrains.annotations.NonNls

class Constants {
    companion object {

        @JvmField @NonNls val KOBALT_SYSTEM_ID = ProjectSystemId("KOBALT")

        val MIN_KOBALT_VERSION = "0.751"

        /** If true, will launch kobalt found in ~/kotlin/kobalt/libs/kobalt-$DEV_VERSION.jar */
        val DEV_MODE = false
        val DEV_VERSION = "0.752"

        val BUILD_FILE_NAME = "Build.kt"

        val BUILD_FILE = "kobalt/src/$BUILD_FILE_NAME"

        val BUILD_FILE_EXTENSIONS = "kt"

        val KOBALT_LIBRARY_KIND = "kobalt.library"
    }
}
