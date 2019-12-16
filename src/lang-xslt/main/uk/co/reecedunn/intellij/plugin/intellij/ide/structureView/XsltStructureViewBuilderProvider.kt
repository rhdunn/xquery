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
package uk.co.reecedunn.intellij.plugin.intellij.ide.structureView

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.impl.xml.XmlStructureViewTreeModel
import com.intellij.ide.structureView.xml.XmlStructureViewBuilderProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.xml.XmlFile
import uk.co.reecedunn.intellij.plugin.xslt.dom.xsltFile

class XsltStructureViewBuilderProvider : XmlStructureViewBuilderProvider {
    @Suppress("UNUSED_VARIABLE")
    override fun createStructureViewBuilder(file: XmlFile): StructureViewBuilder? {
        val xsl = file.xsltFile() ?: return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return XmlStructureViewTreeModel(file, editor)
            }
        }
    }
}