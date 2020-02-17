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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import uk.co.reecedunn.intellij.plugin.core.vfs.ZipFileSystem
import uk.co.reecedunn.intellij.plugin.core.xml.XmlDocument
import uk.co.reecedunn.intellij.plugin.core.xml.XmlElement
import uk.co.reecedunn.intellij.plugin.xqdoc.documentation.XQDocDocumentationIndex
import uk.co.reecedunn.intellij.plugin.xqdoc.documentation.XQDocDocumentation
import uk.co.reecedunn.intellij.plugin.xqdoc.documentation.XQDocFunctionDocumentation
import uk.co.reecedunn.intellij.plugin.xdm.functions.XdmFunctionReference
import uk.co.reecedunn.intellij.plugin.xdm.module.path.XdmModuleType
import uk.co.reecedunn.intellij.plugin.xdm.namespaces.XdmNamespaceDeclaration

data class ApiDocs(private val filesystem: VirtualFileSystem, private val root: VirtualFile) : XQDocDocumentationIndex {
    // region XdmDocumentationIndex

    override fun invalidate() {}

    override fun lookup(ref: XdmFunctionReference): XQDocFunctionDocumentation? {
        val name = ref.functionName ?: return null
        return functions.find {
            it.name(XdmModuleType.XQuery) == name.localName?.data && it.namespace == name.namespace?.data
        }
    }

    override fun lookup(decl: XdmNamespaceDeclaration): XQDocDocumentation? {
        return modules.find { it.namespaceUri == decl.namespaceUri?.data }
    }

    // endregion

    val docs: XmlDocument by lazy {
        val docs = XmlDocument.parse("<apidoc:collection xmlns:apidoc=\"http://marklogic.com/xdmp/apidoc\"/>", NAMESPACES)
        root.children[0].findFileByRelativePath("pubs/raw/apidoc")!!.children.forEach {
            if (it.name.endsWith(".xml")) {
                val xml = XmlDocument.parse(it, NAMESPACES)
                when {
                    xml.root.`is`("apidoc:module") -> {
                        val node = docs.doc.importNode(xml.root.element, true)
                        docs.root.appendChild(node)
                    }
                    else -> {}
                }
            }
        }
        docs
    }

    val modules: List<ApiDocsModule> by lazy {
        root.children[0].findFileByRelativePath("pubs/raw/apidoc")!!.children.asSequence().map {
            if (it.name.endsWith(".xml")) {
                val xml = XmlDocument.parse(it, NAMESPACES)
                when {
                    xml.root.`is`("apidoc:module") -> ApiDocsModule(xml.root)
                    else -> null
                }
            } else
                null
        }.filterNotNull().toList()
    }

    val functions: Sequence<ApiDocsFunction> get() = modules.asSequence().flatMap { it.functions.asSequence() }

    companion object {
        private val NAMESPACES = mapOf(
            "apidoc" to "http://marklogic.com/xdmp/apidoc"
        )

        fun create(docs: VirtualFile): ApiDocs {
            return if (docs.isDirectory) {
                ApiDocs(docs.fileSystem, docs)
            } else
                create(ZipFileSystem(docs.inputStream)) // contentsToByteArray can throw a file too big exception.
        }

        fun create(pkg: ByteArray): ApiDocs = create(ZipFileSystem(pkg))

        private fun create(pkg: ZipFileSystem): ApiDocs = ApiDocs(pkg, pkg.findFileByPath("")!!)
    }
}

internal val XmlElement.moduleTypes: Array<XdmModuleType>
    get() = when (val name = attribute("class")) {
        "javascript" -> XdmModuleType.JAVASCRIPT
        "xquery" -> XdmModuleType.XPATH_OR_XQUERY
        null -> XdmModuleType.MARKLOGIC
        else -> throw UnsupportedOperationException("Unknown MarkLogic apidoc class '$name'")
    }
