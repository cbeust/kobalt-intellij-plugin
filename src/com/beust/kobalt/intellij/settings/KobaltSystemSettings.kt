package com.beust.kobalt.intellij.settings

import com.beust.kobalt.intellij.MyState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "KobaltSystemSettings", storages = arrayOf(Storage("gradle.settings.xml")))
class KobaltSystemSettings : PersistentStateComponent<MyState> {
   companion object{
       fun getInstance() = ServiceManager.getService<KobaltSystemSettings>(KobaltSystemSettings::class.java)
   }
    override fun loadState(state: MyState?) {
       //TODO
    }

    override fun getState(): MyState? {
        //TODO
        return null
    }
}