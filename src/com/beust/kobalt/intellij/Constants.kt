package com.beust.kobalt.intellij

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.jetbrains.annotations.NonNls

class Constants {
    companion object {
        val MIN_KOBALT_VERSION = "0.798"

        @JvmField @NonNls val KOBALT_SYSTEM_ID = ProjectSystemId("KOBALT")

        /** If true, will launch kobalt found in ~/kotlin/kobalt/libs/kobalt-$DEV_VERSION.jar */
        val DEV_MODE = false
        val DEV_VERSION = MIN_KOBALT_VERSION + 1

        val BUILD_FILE_NAME = "Build.kt"

        val BUILD_FILE = "kobalt/src/$BUILD_FILE_NAME"

        val BUILD_FILE_EXTENSIONS = "kt"

        val KOBALT_LIBRARY_KIND = "kobalt.library"

        val KOBALT_BUILD_DIR_NAME = "kobaltBuild"
        val KOBALT_BUILD_CLASSES_DIR_NAME = "$KOBALT_BUILD_DIR_NAME/classes"
        val KOBALT_BUILD_TEST_CLASSES_DIR_NAME = "$KOBALT_BUILD_DIR_NAME/test-classes"

    }
}
