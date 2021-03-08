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
package uk.co.reecedunn.intellij.plugin.xslt.psi.impl.saxon

import com.intellij.psi.PsiElement
import uk.co.reecedunn.intellij.plugin.xpath.resources.XPathIcons
import uk.co.reecedunn.intellij.plugin.xslt.ast.saxon.SaxonItemType
import uk.co.reecedunn.intellij.plugin.xslt.psi.impl.XsltShadowPsiElement
import javax.swing.Icon

class SaxonItemTypePsiImpl(element: PsiElement) : XsltShadowPsiElement(element), SaxonItemType {
    override fun getIcon(flags: Int): Icon = XPathIcons.Nodes.TypeDecl
}
