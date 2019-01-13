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
package uk.co.reecedunn.intellij.plugin.intellij.lang

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileNameMatcher
import uk.co.reecedunn.intellij.plugin.core.lang.LanguageAssociations

val NTriples: Language by lazy {
    Language.findInstancesByMimeType("application/n-triples").firstOrNull() ?: {
        val language = object : Language("NTriples", "application/n-triples") {
            override fun getDisplayName(): String = "N-Triples"
        }
        language.putUserData(LanguageAssociations.KEY, object : LanguageAssociations {
            override val associations: List<FileNameMatcher> = listOf(
                ExtensionFileNameMatcher("nt")
            )
        })
        language
    }()
}

val RDF_FORMATS: List<Language> by lazy {
    listOf(
        NTriples
    )
}
