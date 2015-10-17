package com.beust.kobalt.intellij.plugin

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.completion.JavaMethodCallElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.Weigher

class KobaltWeigher : CompletionWeigher() {
    override fun weigh(element: LookupElement, location: CompletionLocation): Comparable<Nothing>? {
        println("Weighing $element at location $location")
        if (element is JavaMethodCallElement) {
            val pe = element.psiElement
            val method = element.`object` as PsiMethod
            val annotations = method.modifierList.annotations
            if (annotations.size() > 0 && annotations.get(0).qualifiedName == "Directive") {
                return 1
            }
        }
        return -1
    }

}