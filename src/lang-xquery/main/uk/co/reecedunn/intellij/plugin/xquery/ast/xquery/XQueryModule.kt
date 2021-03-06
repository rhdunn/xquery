/*
 * Copyright (C) 2016-2017, 2019 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xquery.ast.xquery

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import uk.co.reecedunn.intellij.plugin.intellij.lang.Specification
import uk.co.reecedunn.intellij.plugin.intellij.lang.XQuerySpec
import uk.co.reecedunn.intellij.plugin.xdm.types.XsStringValue
import uk.co.reecedunn.intellij.plugin.xquery.model.XQueryPrologResolver
import uk.co.reecedunn.intellij.plugin.xquery.project.settings.XQueryProjectSettings

data class XQueryVersionRef(val declaration: XsStringValue?, val version: Specification?) {
    fun getVersionOrDefault(project: Project): Specification {
        if (version == null) {
            val settings: XQueryProjectSettings = XQueryProjectSettings.getInstance(project)
            val product = settings.product
            val productVersion = settings.productVersion
            val xquery = settings.XQueryVersion ?: return XQuerySpec.REC_1_0_20070123
            return XQuerySpec.versionForXQuery(product, productVersion, xquery) ?: XQuerySpec.REC_1_0_20070123
        }
        return version
    }
}

/**
 * An XQuery 1.0 `Module` node in the XQuery AST.
 *
 * This is used as the IntelliJ file implementation for XQuery files, as it is
 * the top-level grammar production in the XQuery specifications.
 */
interface XQueryModule : PsiFile {
    @Suppress("PropertyName")
    val XQueryVersion: XQueryVersionRef

    @Suppress("PropertyName")
    val XQueryVersions: Sequence<XQueryVersionRef>

    val predefinedStaticContext: XQueryProlog?

    val mainOrLibraryModule: XQueryPrologResolver?
}
