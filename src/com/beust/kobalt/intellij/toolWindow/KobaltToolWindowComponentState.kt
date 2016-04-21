package com.beust.kobalt.intellij.toolWindow

import com.intellij.util.xmlb.annotations.Tag
import org.jdom.Element

/**
 * @author Dmitry Zhuravlev
 *         Date: 20.04.16
 */
class KobaltToolWindowComponentState {
    @Tag("treeState")
    var treeState: Element? = null
}