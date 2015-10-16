package com.beust.kobalt.intellij.plugin

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.tree.java.IKeywordElementType
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NotNull

public class SimpleCompletionContributor : CompletionContributor() {
    init {
        val provider = object: CompletionProvider<CompletionParameters>() {
            override fun addCompletions(@NotNull parameters: CompletionParameters,
                    context: ProcessingContext,
                    @NotNull resultSet: CompletionResultSet) {
                resultSet.addElement(LookupElementBuilder.create("Hello"));
            }
        }
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(IKeywordElementType("return")).withLanguage(JavaLanguage.INSTANCE),
                provider)
    }
}