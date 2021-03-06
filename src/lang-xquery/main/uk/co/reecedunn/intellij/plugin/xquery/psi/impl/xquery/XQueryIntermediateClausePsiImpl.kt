/*
 * Copyright (C) 2016-2018 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xquery.psi.impl.xquery

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import uk.co.reecedunn.intellij.plugin.core.psi.elementType
import uk.co.reecedunn.intellij.plugin.intellij.lang.Version
import uk.co.reecedunn.intellij.plugin.intellij.lang.VersionConformance
import uk.co.reecedunn.intellij.plugin.intellij.lang.XQuerySpec
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryInitialClause
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryIntermediateClause
import uk.co.reecedunn.intellij.plugin.xquery.parser.XQueryElementType

private val XQUERY10: List<Version> = listOf()
private val XQUERY30: List<Version> = listOf(XQuerySpec.REC_3_0_20140408)

class XQueryIntermediateClausePsiImpl(node: ASTNode) :
    ASTWrapperPsiElement(node), XQueryIntermediateClause, VersionConformance {

    override val requiresConformance: List<Version>
        get() {
            val current = firstChild.elementType
            if (current === XQueryElementType.COUNT_CLAUSE || current === XQueryElementType.GROUP_BY_CLAUSE) {
                return XQUERY30
            }

            val prevElement = prevSibling
            val prev = if (prevElement is XQueryInitialClause) null else prevElement.firstChild.elementType
            if (prev === XQueryElementType.WHERE_CLAUSE) {
                return if (current === XQueryElementType.ORDER_BY_CLAUSE) XQUERY10 else XQUERY30
            } else if (prev === XQueryElementType.ORDER_BY_CLAUSE) {
                return XQUERY30
            }
            return XQUERY10
        }

    override val conformanceElement: PsiElement
        get() = firstChild.firstChild
}
