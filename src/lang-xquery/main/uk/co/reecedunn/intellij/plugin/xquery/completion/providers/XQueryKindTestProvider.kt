/*
 * Copyright (C) 2019 Reece H. Dunn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.reecedunn.intellij.plugin.xquery.completion.providers

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import uk.co.reecedunn.intellij.plugin.core.completion.CompletionProviderEx
import uk.co.reecedunn.intellij.plugin.intellij.lang.MarkLogic
import uk.co.reecedunn.intellij.plugin.intellij.lang.XQuerySpec
import uk.co.reecedunn.intellij.plugin.xpath.completion.lookup.XPathInsertText
import uk.co.reecedunn.intellij.plugin.xpath.completion.lookup.XPathKeywordLookup
import uk.co.reecedunn.intellij.plugin.xpath.completion.property.XPathCompletionProperty
import uk.co.reecedunn.intellij.plugin.xquery.completion.property.XQueryCompletionProperty

object XQueryKindTestProvider : CompletionProviderEx {
    private val MARKLOGIC_60_KIND_TESTS = listOf(
        XPathKeywordLookup("binary", XPathInsertText.EMPTY_PARAMS)
    )

    private val MARKLOGIC_70_KIND_TESTS = listOf(
        XPathKeywordLookup("attribute-decl", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("attribute-decl", XPathInsertText.PARAMS_WILDCARD),
        XPathKeywordLookup("attribute-decl", XPathInsertText.PARAMS_NAME),
        XPathKeywordLookup("complex-type", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("complex-type", XPathInsertText.PARAMS_WILDCARD),
        XPathKeywordLookup("complex-type", XPathInsertText.PARAMS_TYPE),
        XPathKeywordLookup("element-decl", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("element-decl", XPathInsertText.PARAMS_WILDCARD),
        XPathKeywordLookup("element-decl", XPathInsertText.PARAMS_NAME),
        XPathKeywordLookup("model-group", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("model-group", XPathInsertText.PARAMS_WILDCARD),
        XPathKeywordLookup("model-group", XPathInsertText.PARAMS_NAME),
        XPathKeywordLookup("schema-component", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("schema-facet", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("schema-facet", XPathInsertText.PARAMS_WILDCARD),
        XPathKeywordLookup("schema-facet", XPathInsertText.PARAMS_NAME),
        XPathKeywordLookup("schema-particle", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("schema-particle", XPathInsertText.PARAMS_WILDCARD),
        XPathKeywordLookup("schema-particle", XPathInsertText.PARAMS_NAME),
        XPathKeywordLookup("schema-root", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("schema-type", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("schema-type", XPathInsertText.PARAMS_WILDCARD),
        XPathKeywordLookup("schema-type", XPathInsertText.PARAMS_TYPE),
        XPathKeywordLookup("schema-wildcard", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("simple-type", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("simple-type", XPathInsertText.PARAMS_WILDCARD),
        XPathKeywordLookup("simple-type", XPathInsertText.PARAMS_TYPE)
    )

    private val MARKLOGIC_80_KIND_TESTS = listOf(
        XPathKeywordLookup("array-node", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("array-node", XPathInsertText.PARAMS_KEYNAME),
        XPathKeywordLookup("boolean-node", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("boolean-node", XPathInsertText.PARAMS_KEYNAME),
        XPathKeywordLookup("node", XPathInsertText.PARAMS_WILDCARD), // XPath/XQuery extension
        XPathKeywordLookup("node", XPathInsertText.PARAMS_KEYNAME), // XPath/XQuery extension
        XPathKeywordLookup("null-node", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("null-node", XPathInsertText.PARAMS_KEYNAME),
        XPathKeywordLookup("number-node", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("number-node", XPathInsertText.PARAMS_KEYNAME),
        XPathKeywordLookup("object-node", XPathInsertText.EMPTY_PARAMS),
        XPathKeywordLookup("object-node", XPathInsertText.PARAMS_KEYNAME),
        XPathKeywordLookup("text", XPathInsertText.PARAMS_KEYNAME) // XPath/XQuery extension
    )

    private fun isMarkLogicXQueryVersion(context: ProcessingContext): Boolean {
        return when (context[XQueryCompletionProperty.XQUERY_VERSION]) {
            XQuerySpec.MARKLOGIC_1_0 -> true
            XQuerySpec.MARKLOGIC_0_9 -> true
            else -> false
        }
    }

    override fun apply(element: PsiElement, context: ProcessingContext, result: CompletionResultSet) {
        val version = context[XPathCompletionProperty.XPATH_PRODUCT_VERSION]
        if (version.kind === MarkLogic && isMarkLogicXQueryVersion(context)) {
            if (version.value >= 6.0) result.addAllElements(MARKLOGIC_60_KIND_TESTS)
            if (version.value >= 7.0) result.addAllElements(MARKLOGIC_70_KIND_TESTS)
            if (version.value >= 8.0) result.addAllElements(MARKLOGIC_80_KIND_TESTS)
        }
    }
}
