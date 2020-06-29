/*
 * Copyright (C) 2016-2019 Reece H. Dunn
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
import uk.co.reecedunn.intellij.plugin.core.sequences.children
import uk.co.reecedunn.intellij.plugin.xdm.variables.XdmVariableBinding
import uk.co.reecedunn.intellij.plugin.xdm.variables.XdmVariableName
import uk.co.reecedunn.intellij.plugin.xdm.types.XsAnyUriValue
import uk.co.reecedunn.intellij.plugin.xdm.types.XsQNameValue
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryGroupingSpec
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryGroupingVariable

class XQueryGroupingSpecPsiImpl(node: ASTNode) : ASTWrapperPsiElement(node), XQueryGroupingSpec, XdmVariableBinding {
    // region XQueryGroupingSpec

    override val collation: XsAnyUriValue? get() = children().filterIsInstance<XsAnyUriValue>().firstOrNull()

    // endregion
    // region XPathVariableBinding

    private val varName
        get(): XdmVariableName? {
            return children().filterIsInstance<XQueryGroupingVariable>().firstOrNull() as? XdmVariableName
        }

    override val variableName: XsQNameValue? get() = varName?.variableName

    // endregion
}
