/*
 * Copyright (C) 2018-2019 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.marklogic.query.rest

import uk.co.reecedunn.intellij.plugin.core.xml.XmlDocument
import uk.co.reecedunn.intellij.plugin.core.xml.children
import uk.co.reecedunn.intellij.plugin.processor.debug.StackFrame
import uk.co.reecedunn.intellij.plugin.processor.query.QueryError

class MarkLogicQueryError(xml: String) : QueryError() {
    companion object {
        private val XMLNS_ERR = "http://www.w3.org/2005/xqt-errors"
    }

    private val doc = XmlDocument.parse(xml)

    override val standardCode: String by lazy {
        doc.root.children(XMLNS_ERR, "code").first().firstChild.nodeValue.replace("^err:".toRegex(), "")
    }

    override val vendorCode: String? by lazy {
        doc.root.children(XMLNS_ERR, "vendor-code").first().firstChild?.nodeValue
    }

    override val description: String? by lazy {
        doc.root.children(XMLNS_ERR, "description").first().firstChild?.nodeValue
    }

    override val frame: StackFrame by lazy {
        object : StackFrame {
            override val module: String? =
                doc.root.children(XMLNS_ERR, "module").firstOrNull()?.firstChild?.nodeValue

            override val lineNumber: Int? =
                doc.root.children(XMLNS_ERR, "module").firstOrNull()?.getAttribute("line")?.toInt()

            override val columnNumber: Int? =
                doc.root.children(XMLNS_ERR, "module").firstOrNull()?.getAttribute("column")?.toInt()
        }
    }
}
