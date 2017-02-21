package com.beust.kobalt.intellij.config

import com.beust.kobalt.intellij.Constants
import com.intellij.openapi.roots.libraries.LibraryKind
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import icons.KobaltIcons

/**
 * @author Dmitry Zhuravlev
 *         Date:  26.04.2016
 */
class KobaltLibraryPresentationProvider : LibraryPresentationProvider<KobaltLibraryProperties>(KOBALT_KIND) {

    companion object {
        private val KOBALT_KIND = LibraryKind.create(Constants.KOBALT_LIBRARY_KIND)

        private const val KOBALT_LIBRARY_NAME_PATTERN = "(kobalt-(\\d+.\\d+).jar)"
        private const val KOBALT_LIBRARY_VERSION_PATTERN = "(\\d+.\\d+)"
    }

    override fun getDescription(properties: KobaltLibraryProperties): String? {
        val version = properties.version
        return "Kobalt library" + if (version != null) " of version " + version else ":"
    }


    override fun getIcon() = KobaltIcons.Kobalt

    override fun detect(classesRoots: MutableList<VirtualFile>): KobaltLibraryProperties? {
        val kobaltJarVersion = detectKobaltJarVersion(VfsUtilCore.toVirtualFileArray(classesRoots))
        if (kobaltJarVersion.isNotEmpty()) return KobaltLibraryProperties(kobaltJarVersion[0])
        return null
    }

    private fun detectKobaltJarVersion(libraryFiles: Array<out VirtualFile>) = libraryFiles
            .filter { libraryFile -> Regex(KOBALT_LIBRARY_NAME_PATTERN).matches(libraryFile.name) }
            .map { libraryFile -> Regex(KOBALT_LIBRARY_VERSION_PATTERN).find(libraryFile.name)?.value ?: "Not determined" }
}