package icons

import com.intellij.openapi.util.IconLoader

/**
 * @author Dmitry Zhuravlev
 *         Date:  13.05.2016
 */
object OtherIcons {
    @JvmField val Kotlin = load("/icons/kotlin.png")

    private fun load(path: String) = IconLoader.getIcon(path, OtherIcons::class.java)
}