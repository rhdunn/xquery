/*
 * Copyright (C) 2016, 2019 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xquery.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import uk.co.reecedunn.intellij.plugin.core.sequences.children
import uk.co.reecedunn.intellij.plugin.core.sequences.filterIsElementType
import uk.co.reecedunn.intellij.plugin.xpath.ast.xpath.XPathNCName
import uk.co.reecedunn.intellij.plugin.xdm.model.XsQNameValue
import uk.co.reecedunn.intellij.plugin.xquery.ast.plugin.PluginDirAttribute
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryAnnotation
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryDirElemConstructor
import uk.co.reecedunn.intellij.plugin.xpath.lexer.IKeywordOrNCNameType
import uk.co.reecedunn.intellij.plugin.intellij.lexer.XQuerySyntaxHighlighterColors
import uk.co.reecedunn.intellij.plugin.intellij.resources.XPathBundle
import uk.co.reecedunn.intellij.plugin.xdm.model.XdmWildcardValue
import uk.co.reecedunn.intellij.plugin.xpath.ast.xpath.XPathWildcard
import uk.co.reecedunn.intellij.plugin.xpath.lexer.XPathTokenType
import uk.co.reecedunn.intellij.plugin.xpath.parser.XPathElementType
import uk.co.reecedunn.intellij.plugin.xquery.lexer.XQueryTokenType

class QNameAnnotator : Annotator {
    private fun checkQNameWhitespaceBefore(qname: XsQNameValue, separator: PsiElement, holder: AnnotationHolder) {
        val before = separator.prevSibling
        if (
            before.node.elementType === XPathTokenType.WHITE_SPACE ||
            before.node.elementType === XPathElementType.COMMENT
        ) {
            val message =
                if (qname is XPathWildcard)
                    XPathBundle.message("parser.error.wildcard.whitespace-before-local-part")
                else
                    XPathBundle.message("parser.error.qname.whitespace-before-local-part")
            holder.createErrorAnnotation(before, message)
        }
    }

    private fun checkQNameWhitespaceAfter(qname: XsQNameValue, separator: PsiElement, holder: AnnotationHolder) {
        val after = separator.nextSibling
        if (
            after.node.elementType === XPathTokenType.WHITE_SPACE ||
            after.node.elementType === XPathElementType.COMMENT
        ) {
            val message =
                if (qname is XPathWildcard)
                    XPathBundle.message("parser.error.wildcard.whitespace-after-local-part")
                else
                    XPathBundle.message("parser.error.qname.whitespace-after-local-part")
            holder.createErrorAnnotation(after, message)
        }
    }

    override fun annotate(qname: PsiElement, holder: AnnotationHolder) {
        if (qname !is XsQNameValue) return

        val xmlns: Boolean
        if (qname.prefix != null) {
            when {
                qname.prefix!!.data == "xmlns" -> xmlns = true
                qname.prefix !is XdmWildcardValue -> {
                    xmlns = false
                    val prefix = qname.prefix?.element!!
                    holder.createInfoAnnotation(prefix, null).enforcedTextAttributes = TextAttributes.ERASE_MARKER
                    if (qname.parent is PluginDirAttribute || qname.parent is XQueryDirElemConstructor) {
                        holder.createInfoAnnotation(prefix, null).textAttributes = XQuerySyntaxHighlighterColors.XML_TAG
                    }
                    holder.createInfoAnnotation(prefix, null).textAttributes = XQuerySyntaxHighlighterColors.NS_PREFIX
                }
                else -> xmlns = false
            }

            // Detect whitespace errors here instead of the parser so the QName annotator gets run.
            qname.children().filterIsElementType(XPathTokenType.QNAME_SEPARATOR).firstOrNull()?.let {
                checkQNameWhitespaceBefore(qname, it, holder)
                checkQNameWhitespaceAfter(qname, it, holder)
            }
        } else {
            xmlns = false
        }

        if (qname.localName != null) {
            val localName = qname.localName?.element!!
            if (xmlns) {
                holder.createInfoAnnotation(localName, null).enforcedTextAttributes = TextAttributes.ERASE_MARKER
                if (qname.parent is PluginDirAttribute) {
                    holder.createInfoAnnotation(localName, null).textAttributes = XQuerySyntaxHighlighterColors.XML_TAG
                }
                holder.createInfoAnnotation(localName, null).textAttributes = XQuerySyntaxHighlighterColors.NS_PREFIX
            } else if (qname.parent is XQueryAnnotation) {
                holder.createInfoAnnotation(localName, null).enforcedTextAttributes = TextAttributes.ERASE_MARKER
                holder.createInfoAnnotation(localName, null).textAttributes = XQuerySyntaxHighlighterColors.ANNOTATION
            } else if (localName.node.elementType is IKeywordOrNCNameType) {
                holder.createInfoAnnotation(localName, null).enforcedTextAttributes = TextAttributes.ERASE_MARKER
                holder.createInfoAnnotation(localName, null).textAttributes = XQuerySyntaxHighlighterColors.IDENTIFIER
            } else if (localName is XPathNCName) {
                if (localName.node.elementType is IKeywordOrNCNameType) {
                    holder.createInfoAnnotation(localName, null).enforcedTextAttributes = TextAttributes.ERASE_MARKER
                    holder.createInfoAnnotation(localName, null).textAttributes =
                        XQuerySyntaxHighlighterColors.IDENTIFIER
                }
            }
        }
    }
}
