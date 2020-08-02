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
package uk.co.reecedunn.intellij.plugin.xslt.intellij.lang

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileNameMatcher
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import uk.co.reecedunn.intellij.plugin.core.lang.LanguageData

/**
 * XML Stylesheet Language: Transform
 */
object XSLT : Language("XSLT") {
    const val NAMESPACE: String = "http://www.w3.org/1999/XSL/Transform"

    fun isXsltFile(file: PsiFile): Boolean = (file as? XmlFile)?.rootTag?.namespace == NAMESPACE

    override fun isCaseSensitive(): Boolean = true

    override fun getDisplayName(): String = "XSLT"

    override fun getAssociatedFileType(): LanguageFileType? = null

    init {
        putUserData(LanguageData.KEY, object : LanguageData {
            override val associations: List<FileNameMatcher> = listOf(
                ExtensionFileNameMatcher("xsl"),
                ExtensionFileNameMatcher("xslt")
            )

            override val mimeTypes: Array<String> = arrayOf("application/xslt+xml")
        })
    }

    // endregion
}
