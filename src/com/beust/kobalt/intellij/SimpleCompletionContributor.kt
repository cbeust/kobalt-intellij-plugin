package com.beust.kobalt.intellij

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.java.IKeywordElementType
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NotNull

public class SimpleCompletionContributor : CompletionContributor() {
    init {
        val provider = object: CompletionProvider<CompletionParameters>() {
            override fun addCompletions(@NotNull parameters: CompletionParameters,
                    context: ProcessingContext,
                    @NotNull resultSet: CompletionResultSet) {
                var sorter = object: CompletionSorter() {
                    override fun weigh(p0: LookupElementWeigher?): CompletionSorter? {
                        throw UnsupportedOperationException()
                    }

                    override fun weighAfter(p0: String, vararg p1: LookupElementWeigher?): CompletionSorter? {
                        throw UnsupportedOperationException()
                    }

                    override fun weighBefore(p0: String, vararg p1: LookupElementWeigher?): CompletionSorter? {
                        throw UnsupportedOperationException()
                    }

                }
                resultSet.withRelevanceSorter(sorter)
                resultSet.addElement(LookupElementBuilder.create("Kobalt completion"));
            }
        }
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(),
//                com.intellij.patterns.PlatformPatterns.psiElement(JavaTokenType.COMMA).withLanguage(Language.ANY),
                provider)
    }
}