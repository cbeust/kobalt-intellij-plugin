package com.beust.kobalt.intellij.plugin

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.completion.JavaMethodCallElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager

class KobaltWeigher : CompletionWeigher() {
    override fun weigh(element: LookupElement, location: CompletionLocation): Comparable<Nothing>? {
        val project = location.project
        if ("Build.kt" == element.psiElement?.containingFile?.name && element is JavaMethodCallElement) {
            val annotations = element.`object`.modifierList.annotations
            return if (annotations.any { it.qualifiedName == "Directive" }) 1
                else -1
        }
        return null
    }
}