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
package uk.co.reecedunn.intellij.plugin.xslt.tests.psi

import org.hamcrest.CoreMatchers.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.co.reecedunn.intellij.plugin.core.tests.assertion.assertThat
import uk.co.reecedunn.intellij.plugin.xslt.ast.xslt.*
import uk.co.reecedunn.intellij.plugin.xslt.intellij.lang.XSLT
import uk.co.reecedunn.intellij.plugin.xslt.tests.parser.ParserTestCase

// NOTE: This class is private so the JUnit 4 test runner does not run the tests contained in it.
@DisplayName("XSLT 3.0 - IntelliJ Program Structure Interface (PSI)")
private class XsltPsiTest : ParserTestCase() {
    @Nested
    @DisplayName("XSLT 3.0 (3) Stylesheet Structure")
    internal inner class StylesheetStructure {
        @Nested
        @DisplayName("XSLT 3.0 (3.7) xsl:stylesheet")
        internal inner class Stylesheet {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                    </xsl:stylesheet>
                """
                val psi = parse<XsltStylesheet>(xml, XSLT.NAMESPACE, "stylesheet")[0]

                assertThat(psi.parent, `is`(nullValue()))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (3.7) xsl:transform")
        internal inner class Transform {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                    </xsl:transform>
                """
                val psi = parse<XsltStylesheet>(xml, XSLT.NAMESPACE, "transform")[0]

                assertThat(psi.parent, `is`(nullValue()))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (3.11.2) xsl:include")
        internal inner class Include {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:include href="test.xsl"/>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltInclude>(xml, XSLT.NAMESPACE, "include")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltStylesheet::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (3.11.3) xsl:import")
        internal inner class Import {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:import href="test.xsl"/>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltImport>(xml, XSLT.NAMESPACE, "import")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltStylesheet::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }
    }

    @Nested
    @DisplayName("XSLT 3.0 (6) Template Rules")
    internal inner class TemplateRules {
        @Nested
        @DisplayName("XSLT 3.0 (6.1) xsl:template")
        internal inner class Template {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="test"/>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltTemplate>(xml, XSLT.NAMESPACE, "template")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltStylesheet::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (6.3) xsl:apply-templates")
        internal inner class ApplyTemplates {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:apply-templates select="ipsum"/>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltApplyTemplates>(xml, XSLT.NAMESPACE, "apply-templates")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (6.8) xsl:apply-imports")
        internal inner class ApplyImports {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:apply-imports/>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltApplyImports>(xml, XSLT.NAMESPACE, "apply-imports")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }
    }

    @Nested
    @DisplayName("XSLT 3.0 (10) Callable Components")
    internal inner class CallableComponents {
        @Nested
        @DisplayName("XSLT 3.0 (10.1) xsl:call-template")
        internal inner class CallTemplate {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:call-template name="ipsum"/>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltCallTemplate>(xml, XSLT.NAMESPACE, "call-template")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (10.2) xsl:attribute-set")
        internal inner class AttributeSet {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:attribute-set name="test"/>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltAttributeSet>(xml, XSLT.NAMESPACE, "attribute-set")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltStylesheet::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }
    }

    @Nested
    @DisplayName("XSLT 3.0 (11) Creating Nodes and Sequences")
    internal inner class CreatingNodesAndSequences {
        @Nested
        @DisplayName("XSLT 3.0 (11.1) xsl:namespace-alias")
        internal inner class NamespaceAlias {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                                    xmlns:axsl="urn:xslt:namespace-alias" version="1.0">
                        <xsl:namespace-alias stylesheet-prefix="axsl" result-prefix="xsl"/>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltNamespaceAlias>(xml, XSLT.NAMESPACE, "namespace-alias")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltStylesheet::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (11.2) xsl:element")
        internal inner class Element {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:element name="ipsum"/>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltElement>(xml, XSLT.NAMESPACE, "element")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (11.3) xsl:attribute")
        internal inner class Attribute {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:attribute name="ipsum"/>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltAttribute>(xml, XSLT.NAMESPACE, "attribute")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (11.4.2) xsl:text")
        internal inner class Text {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:text>ipsum</xsl:text>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltText>(xml, XSLT.NAMESPACE, "text")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (11.6) xsl:processing-instruction")
        internal inner class ProcessingInstruction {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:processing-instruction name="ipsum">dolor</xsl:processing-instruction>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltProcessingInstruction>(xml, XSLT.NAMESPACE, "processing-instruction")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (11.8) xsl:comment")
        internal inner class Comment {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:comment>ipsum</xsl:comment>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltComment>(xml, XSLT.NAMESPACE, "comment")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }

        @Nested
        @DisplayName("XSLT 3.0 (11.9.1) xsl:copy")
        internal inner class Copy {
            @Test
            @DisplayName("hierarchy")
            fun hierarchy() {
                @Language("XML") val xml = """
                    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                        <xsl:template match="lorem">
                            <xsl:copy/>
                        </xsl:template>
                    </xsl:stylesheet>
                """
                val psi = parse<XsltCopy>(xml, XSLT.NAMESPACE, "copy")[0]

                assertThat(psi.parent, `is`(instanceOf(XsltTemplate::class.java)))
                assertThat(psi.children.size, `is`(0))
                assertThat(psi.prevSibling, `is`(nullValue()))
                assertThat(psi.nextSibling, `is`(nullValue()))

                val parent = psi.parent!!
                assertThat(parent.children.size, `is`(1))
                assertThat(parent.children[0], `is`(sameInstance(psi)))
            }
        }
    }
}