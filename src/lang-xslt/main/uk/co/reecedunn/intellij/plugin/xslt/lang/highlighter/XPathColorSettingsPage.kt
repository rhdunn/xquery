/*
 * Copyright (C) 2016, 2019-2021 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xslt.lang.highlighter

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import uk.co.reecedunn.intellij.plugin.xpath.lang.highlighter.XPathSyntaxHighlighter
import uk.co.reecedunn.intellij.plugin.xpath.lang.highlighter.XPathSyntaxHighlighterColors
import uk.co.reecedunn.intellij.plugin.xpath.resources.XPathQueries
import javax.swing.Icon

class XPathColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon? = null

    override fun getHighlighter(): SyntaxHighlighter = XPathSyntaxHighlighter

    override fun getDemoText(): String = demo

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> =
        XPathSyntaxHighlighterColors.ADDITIONAL_DESCRIPTORS

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "XPath and XSLT"

    companion object {
        private val DESCRIPTORS = arrayOf(
            XPathSyntaxHighlighterColors.DESCRIPTORS,
            XsltSyntaxHighlighterColors.DESCRIPTORS
        ).flatten().toTypedArray()

        @Suppress("RegExpAnonymousGroup", "Reformat")
        private val demo: String by lazy {
            var ret = XPathQueries.ColorSettingsDemo
            ret = ret.replace("\r\n", "\n")
            ret = ret.replace(" \$items ", " \$<variable>items</variable> ")
            ret = ret.replace("lorem[", "<element>lorem</element>[")
            ret = ret.replace("([^a-zA-Z0-9_])fn:".toRegex()) { it.groups[1]!!.value + "<nsprefix>fn</nsprefix>:" }
            ret = ret.replace(":position(", ":<function-call>position</function-call>(")
            ret = ret.replace(":true(", ":<function-call>true</function-call>(")
            ret = ret.replace("::ipsum[", "::<element>ipsum</element>[")
            ret = ret.replace("@value", "@<attribute>value</attribute>")
            ret = ret.replace("(\$a ", "(\$<parameter>a</parameter> ")
            ret = ret.replace(" xs:integer)", " <nsprefix>xs</nsprefix>:<type>integer</type>)")
            ret = ret.replace("{ \$a ", "{ \$<parameter>a</parameter> ")
            ret = ret.replace("::one ", "::<element>one</element> ")
            ret = ret.replace("::two ", "::<attribute>two</attribute> ")
            ret = ret.replace("::three ", "::<nsprefix>three</nsprefix> ")
            ret = ret.replace("?key-name", "?<map-key>key-name</map-key>")
            ret = ret.replace("processing-instruction(test)", "processing-instruction(<processing-instruction>test</processing-instruction>)")
            ret = ret.replace(" ext ", " <pragma>ext</pragma> ")
            ret
        }
    }
}
