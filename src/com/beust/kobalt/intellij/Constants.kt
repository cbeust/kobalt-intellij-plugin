package com.beust.kobalt.intellij

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.jetbrains.annotations.NonNls

class Constants {
    companion object {
        val MIN_KOBALT_VERSION = "0.883"
        /** If true, will launch kobalt found in ~/kotlin/kobalt/libs/kobalt-$DEV_VERSION.jar */
        val DEV_MODE = false
        val DEV_VERSION_INT = 1 + Integer.parseInt(MIN_KOBALT_VERSION.substring(2))
        val DEV_VERSION = "0.$DEV_VERSION_INT"

        @JvmField @NonNls val KOBALT_SYSTEM_ID = ProjectSystemId("KOBALT")

        val BUILD_FILE_NAME = "Build.kt"
        val BUILD_FILE = "kobalt/src/$BUILD_FILE_NAME"
        val BUILD_FILE_EXTENSIONS = "kt"

        val KOBALT_BUILD_DIR_NAME = "kobaltBuild"
        val KOBALT_BUILD_CLASSES_DIR_NAME = "$KOBALT_BUILD_DIR_NAME/classes"
        val KOBALT_BUILD_TEST_CLASSES_DIR_NAME = "$KOBALT_BUILD_DIR_NAME/test-classes"

        val KOBALT_LIBRARY_KIND = "kobalt.library"
    }
}
