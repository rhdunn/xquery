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
package uk.co.reecedunn.intellij.plugin.xslt.intellij.lexer

import com.intellij.codeInsight.daemon.impl.analysis.XmlNSColorProvider
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlTag
import uk.co.reecedunn.intellij.plugin.xslt.intellij.lang.XSLT
import uk.co.reecedunn.intellij.plugin.xslt.intellij.lang.isIntellijXPathPluginEnabled

class XsltNSColorProvider : XmlNSColorProvider {
    override fun getKeyForNamespace(namespace: String?, context: XmlElement?): TextAttributesKey? {
        return if ((context as? XmlTag)?.namespace == XSLT.NAMESPACE && !isIntellijXPathPluginEnabled())
            XsltSyntaxHighlighterColors.XSLT_DIRECTIVE
        else
            null
    }
}