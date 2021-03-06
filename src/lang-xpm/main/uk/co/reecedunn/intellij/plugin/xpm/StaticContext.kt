/*
 * Copyright (C) 2020 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xpm

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import uk.co.reecedunn.intellij.plugin.xdm.types.XsQNameValue
import uk.co.reecedunn.intellij.plugin.xpm.optree.function.XpmFunctionDeclaration
import uk.co.reecedunn.intellij.plugin.xpm.optree.function.XpmFunctionProvider
import uk.co.reecedunn.intellij.plugin.xpm.optree.namespace.XdmNamespaceType
import uk.co.reecedunn.intellij.plugin.xpm.optree.namespace.XpmNamespaceDeclaration
import uk.co.reecedunn.intellij.plugin.xpm.optree.namespace.XpmNamespaceProvider
import uk.co.reecedunn.intellij.plugin.xpm.optree.variable.XpmVariableDefinition
import uk.co.reecedunn.intellij.plugin.xpm.optree.variable.XpmVariableProvider

fun PsiElement.staticallyKnownNamespaces(): Sequence<XpmNamespaceDeclaration> {
    return XpmNamespaceProvider.EP_NAME.extensionList.asSequence().flatMap {
        it.getInstance().staticallyKnownNamespaces(this)
    }
}

fun PsiElement.defaultNamespace(type: XdmNamespaceType): Sequence<XpmNamespaceDeclaration> {
    return XpmNamespaceProvider.EP_NAME.extensionList.asSequence().flatMap {
        it.getInstance().defaultNamespace(this, type)
    }
}

fun PsiElement.inScopeVariables(): Sequence<XpmVariableDefinition> {
    return XpmVariableProvider.EP_NAME.extensionList.asSequence().flatMap {
        it.getInstance().inScopeVariables(this)
    }
}

fun PsiFile.staticallyKnownFunctions(): Sequence<XpmFunctionDeclaration> {
    return XpmFunctionProvider.EP_NAME.extensionList.asSequence().flatMap {
        it.getInstance().staticallyKnownFunctions(this)
    }
}

fun XsQNameValue.staticallyKnownFunctions(): Sequence<XpmFunctionDeclaration> {
    return XpmFunctionProvider.EP_NAME.extensionList.asSequence().flatMap {
        it.getInstance().staticallyKnownFunctions(this)
    }
}
