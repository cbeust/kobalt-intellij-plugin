package icons

import com.intellij.openapi.util.IconLoader

/**
 * @author Dmitry Zhuravlev
 *         Date: 01.05.16
 */
object KobaltIcons {
    @JvmField val Kobalt = load("/icons/kobalt-16x16.png")
    @JvmField val KobaltToolWindow = load("/icons/kobalt-13x13.png")

    private fun load(path: String) = IconLoader.getIcon(path, KobaltIcons::class.java)
}