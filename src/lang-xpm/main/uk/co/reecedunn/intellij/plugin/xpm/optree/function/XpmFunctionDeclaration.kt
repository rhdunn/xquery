/*
 * Copyright (C) 2018-2021 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xpm.optree.function

import uk.co.reecedunn.intellij.plugin.xdm.types.XdmSequenceType
import uk.co.reecedunn.intellij.plugin.xdm.types.XsQNameValue
import uk.co.reecedunn.intellij.plugin.xpm.optree.annotation.XpmAnnotated
import uk.co.reecedunn.intellij.plugin.xpm.optree.annotation.XpmVariadic
import uk.co.reecedunn.intellij.plugin.xpm.optree.expression.XpmExpression
import uk.co.reecedunn.intellij.plugin.xpm.optree.variable.XpmParameter

interface XpmFunctionDeclaration : XpmAnnotated {
    // region Data Model

    val functionName: XsQNameValue?

    val parameters: List<XpmParameter>

    val returnType: XdmSequenceType?

    val functionBody: XpmExpression?

    // endregion
    // region Variadic Type and Arity

    val variadicType: XpmVariadic

    val declaredArity: Int

    val requiredArity: Int

    // endregion
    // region Presentation

    val paramListPresentableText: String

    val functionRefPresentableText: String?

    // endregion
}
