/*
 * Copyright (C) 2016, 2018-2019 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xpath.psi.impl.xpath

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import uk.co.reecedunn.intellij.plugin.xpath.ast.xpath.XPathSequenceType
import uk.co.reecedunn.intellij.plugin.xpath.lexer.XPathTokenType
import uk.co.reecedunn.intellij.plugin.xpath.model.XdmItemType
import uk.co.reecedunn.intellij.plugin.xpath.model.XdmSequenceType

class XPathSequenceTypePsiImpl(node: ASTNode) : ASTWrapperPsiElement(node), XPathSequenceType, XdmSequenceType {
    // region XdmSequenceType

    private val occurrenceIndicator: IElementType?
        get() = findChildByType<PsiElement>(XPathTokenType.OCCURRENCE_INDICATOR_TOKENS)?.node?.elementType

    override val typeName: String
        get() {
            return when (occurrenceIndicator) {
                XPathTokenType.OPTIONAL -> "${itemType.typeName}?"
                XPathTokenType.STAR -> "${itemType.typeName}*"
                XPathTokenType.PLUS -> "${itemType.typeName}+"
                else -> itemType.typeName
            }
        }

    override val itemType get(): XdmItemType = firstChild as XdmItemType

    override val lowerBound: Int?
        get() {
            return when (occurrenceIndicator) {
                XPathTokenType.OPTIONAL -> 0
                XPathTokenType.STAR -> 0
                XPathTokenType.PLUS -> 1
                else -> 1
            }
        }

    override val upperBound: Int?
        get() {
            return when (occurrenceIndicator) {
                XPathTokenType.OPTIONAL -> 1
                XPathTokenType.STAR -> Int.MAX_VALUE
                XPathTokenType.PLUS -> Int.MAX_VALUE
                else -> 1
            }
        }

    // endregion
}
