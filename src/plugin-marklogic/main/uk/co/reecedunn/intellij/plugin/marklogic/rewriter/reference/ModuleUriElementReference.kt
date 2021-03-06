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
package uk.co.reecedunn.intellij.plugin.marklogic.rewriter.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import uk.co.reecedunn.intellij.plugin.core.sequences.children
import uk.co.reecedunn.intellij.plugin.xdm.module.path.XdmModuleType
import uk.co.reecedunn.intellij.plugin.xdm.types.XdmUriContext
import uk.co.reecedunn.intellij.plugin.xdm.types.XsAnyUriValue
import uk.co.reecedunn.intellij.plugin.xdm.types.impl.values.XsAnyUri
import uk.co.reecedunn.intellij.plugin.xpm.module.loader.resolve
import uk.co.reecedunn.intellij.plugin.xpm.module.resolveUri
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryMainModule
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryQueryBody

class ModuleUriElementReference(element: XmlTag) : PsiReferenceBase<XmlTag>(element) {
    private val uri: XsAnyUriValue by lazy {
        XsAnyUri(element.value.text, XdmUriContext.Location, arrayOf(XdmModuleType.XQuery))
    }

    override fun resolve(): PsiElement? {
        val module = uri.resolve(element) ?: uri.resolveUri(element.project) ?: return null
        val mainModule = module.children().filterIsInstance<XQueryMainModule>().firstOrNull()
        val queryBody = mainModule?.children()?.filterIsInstance<XQueryQueryBody>()?.firstOrNull()
        return queryBody ?: module
    }

    override fun getVariants(): Array<Any> = arrayOf()

    companion object : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            val tag = element as XmlTag
            return when {
                tag.value.text.isBlank() -> arrayOf()
                else -> arrayOf(ModuleUriElementReference(tag))
            }
        }
    }
}
