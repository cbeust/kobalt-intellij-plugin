package com.beust.kobalt.intellij

class Constants {
    companion object {
        val MIN_KOBALT_VERSION = "0.636"

        /** If true, will launch kobalt found in ~/kotlin/kobalt/libs/kobalt-$DEV_VERSION.jar */
        val DEV_MODE = false
        val DEV_VERSION = "KOBALT_VERSION"

        val BUILD_FILE = "kobalt/src/Build.kt"
    }
}
