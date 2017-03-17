package com.beust.kobalt.intellij

import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.diagnostic.Logger

/**
 * Our main application component, which just sees if our kobalt.jar file needs to be downloaded.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @since 10 23, 2015
 */
class KobaltApplicationComponent : ApplicationComponent {
    override fun getComponentName() = "kobalt.ApplicationComponent"

    companion object {
        val LOG = Logger.getInstance(KobaltApplicationComponent::class.java)
    }

    override fun initComponent() {
    }

    override fun disposeComponent() {

    }

}
