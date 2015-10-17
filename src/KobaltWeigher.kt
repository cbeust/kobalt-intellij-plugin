package com.beust.kobalt.intellij.plugin

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.completion.JavaMethodCallElement
import com.intellij.codeInsight.lookup.LookupElement

class KobaltWeigher : CompletionWeigher() {
    override fun weigh(element: LookupElement, location: CompletionLocation): Comparable<Nothing>? {
        if ("Build.kt" == element.psiElement?.containingFile?.name && element is JavaMethodCallElement) {
            val annotations = element.`object`.modifierList.annotations
            if (annotations.size() > 0 && annotations.get(0).qualifiedName == "Directive") {
                return 1
            } else {
                return -1
            }
        }
        return null
    }
}