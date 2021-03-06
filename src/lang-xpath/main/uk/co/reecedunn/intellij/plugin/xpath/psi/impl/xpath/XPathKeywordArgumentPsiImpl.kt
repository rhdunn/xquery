/*
 * Copyright (C) 2021 Reece H. Dunn
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

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import uk.co.reecedunn.intellij.plugin.core.psi.ASTWrapperPsiElement
import uk.co.reecedunn.intellij.plugin.core.sequences.siblings
import uk.co.reecedunn.intellij.plugin.xdm.types.XsQNameValue
import uk.co.reecedunn.intellij.plugin.xpath.ast.xpath.XPathKeywordArgument
import uk.co.reecedunn.intellij.plugin.xpm.lang.validation.XpmSyntaxValidationElement
import uk.co.reecedunn.intellij.plugin.xpm.optree.expression.XpmExpression
import uk.co.reecedunn.intellij.plugin.xpm.optree.expression.impl.XsNCNameExpression

class XPathKeywordArgumentPsiImpl(node: ASTNode) :
    ASTWrapperPsiElement(node),
    XPathKeywordArgument,
    XpmSyntaxValidationElement {
    companion object {
        private val KEY_EXPRESSION = Key.create<XpmExpression>("KEY_EXPRESSION")
    }
    // region ASTDelegatePsiElement

    override fun subtreeChanged() {
        super.subtreeChanged()
        clearUserData(KEY_EXPRESSION)
    }

    // endregion
    // region XpmSyntaxValidationElement

    override val conformanceElement: PsiElement
        get() = firstChild

    // endregion
    // region XPathKeywordArgument

    override val keyExpression: XpmExpression
        get() = computeUserDataIfAbsent(KEY_EXPRESSION) {
            XsNCNameExpression((firstChild as XsQNameValue).localName!!)
        }

    override val valueExpression: XpmExpression?
        get() = firstChild.siblings().filterIsInstance<XpmExpression>().firstOrNull()

    // endregion
}
