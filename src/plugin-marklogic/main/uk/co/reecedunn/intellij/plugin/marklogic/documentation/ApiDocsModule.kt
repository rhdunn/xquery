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
package uk.co.reecedunn.intellij.plugin.marklogic.documentation

import uk.co.reecedunn.intellij.plugin.core.xml.XmlElement
import uk.co.reecedunn.intellij.plugin.xdm.documentation.XdmDocumentationReference

data class ApiDocsModule(private val xml: XmlElement) : XdmDocumentationReference {
    // region apidoc:module

    val name: String by lazy { xml.attribute("name")!! }

    val category: String by lazy { xml.attribute("category")!! }

    val lib: String by lazy { xml.attribute("lib")!! }

    val bucket: String? by lazy { xml.attribute("bucket") }

    private val importDecl: MatchResult? by lazy {
        xml.child("apidoc:summary")?.children("p")?.mapNotNull { p ->
            p.child("code")?.text()?.let { text -> RE_IMPORT_DECL.find(text) }
        }?.firstOrNull()
    }

    val namespaceUri: String? by lazy { importDecl?.groups?.get(2)?.value }

    val locationUri: String? by lazy { importDecl?.groups?.get(3)?.value }

    // endregion
    // region XdmDocumentationReference

    override val href: String = "https://docs.marklogic.com/$lib"

    override val documentation: String by lazy { xml.child("apidoc:summary")?.innerXml() ?: "" }

    override val summary: String by lazy {
        val summary = xml.child("apidoc:summary")
        summary?.child("p")?.xml() ?: summary?.text() ?: ""
    }

    // endregion

    companion object {
        private val RE_IMPORT_DECL =
            "^import module namespace ([a-zA-Z0-9\\-]+) = \"([^\"]+)\"\\s*at\\s*\"([^\"]+)\"\\s*;\\s*$".toRegex()
    }
}