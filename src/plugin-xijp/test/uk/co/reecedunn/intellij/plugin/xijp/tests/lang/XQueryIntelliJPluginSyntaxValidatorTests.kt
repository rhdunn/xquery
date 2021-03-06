/*
 * Copyright (C) 2021 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xijp.tests.lang

import com.intellij.compat.testFramework.registerServiceInstance
import com.intellij.lang.LanguageASTFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.psi.PsiElement
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.*
import uk.co.reecedunn.intellij.plugin.core.extensions.PluginDescriptorProvider
import uk.co.reecedunn.intellij.plugin.core.tests.assertion.assertThat
import uk.co.reecedunn.intellij.plugin.core.tests.parser.ParsingTestCase
import uk.co.reecedunn.intellij.plugin.xijp.lang.XQueryIntelliJPlugin
import uk.co.reecedunn.intellij.plugin.xijp.lang.XQueryIntelliJPluginSyntaxValidator
import uk.co.reecedunn.intellij.plugin.xijp.lang.XQueryIntelliJPluginVersion
import uk.co.reecedunn.intellij.plugin.xpath.lang.XPath
import uk.co.reecedunn.intellij.plugin.xpath.parser.XPathASTFactory
import uk.co.reecedunn.intellij.plugin.xpath.parser.XPathParserDefinition
import uk.co.reecedunn.intellij.plugin.xpm.lang.configuration.XpmLanguageConfiguration
import uk.co.reecedunn.intellij.plugin.xpm.lang.diagnostics.XpmDiagnostics
import uk.co.reecedunn.intellij.plugin.xpm.lang.validation.XpmSyntaxValidation
import uk.co.reecedunn.intellij.plugin.xpm.lang.validation.XpmSyntaxValidator
import uk.co.reecedunn.intellij.plugin.xquery.ast.xquery.XQueryModule
import uk.co.reecedunn.intellij.plugin.xquery.lang.XQuery
import uk.co.reecedunn.intellij.plugin.xquery.parser.XQueryASTFactory
import uk.co.reecedunn.intellij.plugin.xquery.parser.XQueryParserDefinition
import uk.co.reecedunn.intellij.plugin.xquery.project.settings.XQueryProjectSettings

@Suppress("ClassName")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("XQuery IntelliJ Plugin - Syntax Validation - XQuery IntelliJ Plugin")
class XQueryIntelliJPluginSyntaxValidatorTest :
    ParsingTestCase<XQueryModule>("xqy", XQueryParserDefinition(), XPathParserDefinition()),
    PluginDescriptorProvider,
    XpmDiagnostics {
    // region ParsingTestCase

    @BeforeAll
    override fun setUp() {
        super.setUp()
        project.registerServiceInstance(XQueryProjectSettings::class.java, XQueryProjectSettings())
        addExplicitExtension(LanguageASTFactory.INSTANCE, XPath, XPathASTFactory())
        addExplicitExtension(LanguageASTFactory.INSTANCE, XQuery, XQueryASTFactory())

        XpmSyntaxValidator.register(this, XQueryIntelliJPluginSyntaxValidator)
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
    }

    // endregion
    // region PluginDescriptorProvider

    override val pluginId: PluginId = PluginId.getId("XQueryIntelliJPluginSyntaxValidatorTest")

    override val pluginDescriptor: PluginDescriptor
        get() = DefaultPluginDescriptor(pluginId, this::class.java.classLoader)

    override val pluginDisposable: Disposable
        get() = testRootDisposable

    // endregion
    // region XpmDiagnostics

    val report: StringBuffer = StringBuffer()

    @BeforeEach
    fun reset() {
        report.delete(0, report.length)
    }

    override fun error(element: PsiElement, code: String, description: String) {
        if (report.isNotEmpty()) {
            report.append('\n')
        }
        report.append("E $code(${element.textOffset}:${element.textOffset + element.textLength}): $description")
    }

    override fun warning(element: PsiElement, code: String, description: String) {
        if (report.isNotEmpty()) {
            report.append('\n')
        }
        report.append("W $code(${element.textOffset}:${element.textOffset + element.textLength}): $description")
    }

    val validator: XpmSyntaxValidation = XpmSyntaxValidation()

    // endregion

    @Suppress("PrivatePropertyName")
    private val VERSION_1_0 = XpmLanguageConfiguration(
        XQuery.VERSION_1_0,
        XQueryIntelliJPluginVersion(XQueryIntelliJPlugin, 1, 0)
    )

    @Suppress("PrivatePropertyName")
    private val VERSION_1_3 = XpmLanguageConfiguration(XQuery.VERSION_1_0, XQueryIntelliJPlugin.VERSION_1_3)

    @Suppress("PrivatePropertyName")
    private val VERSION_1_4 = XpmLanguageConfiguration(XQuery.VERSION_1_0, XQueryIntelliJPlugin.VERSION_1_4)

    @Nested
    @DisplayName("XQuery IntelliJ Plugin EBNF (95) ParamList; XQuery 3.1 EBNF (32) FunctionDecl")
    internal inner class ParamList_FunctionDecl {
        @Test
        @DisplayName("variadic; XQuery IntelliJ Plugin >= 1.3")
        fun supported() {
            val file = parse<XQueryModule>("declare function f(\$a as xs:string ...) external;")[0]
            validator.configuration = VERSION_1_4
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(report.toString(), `is`(""))
        }

        @Test
        @DisplayName("variadic; XQuery IntelliJ Plugin < 1.3")
        fun notSupported() {
            val file = parse<XQueryModule>("declare function f(\$a as xs:string ...) external;")[0]
            validator.configuration = VERSION_1_3
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(
                report.toString(), `is`(
                    """
                    E XPST0003(35:38): XQuery IntelliJ Plugin 1.3 does not support XQuery IntelliJ Plugin 1.4 constructs.
                    """.trimIndent()
                )
            )
        }

        @Test
        @DisplayName("non-variadic")
        fun nonVariadic() {
            val file = parse<XQueryModule>("declare function f(\$a as xs:string) external;")[0]
            validator.configuration = VERSION_1_0
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(report.toString(), `is`(""))
        }
    }

    @Nested
    @DisplayName("XQuery IntelliJ Plugin EBNF (95) ParamList; XQuery 3.1 EBNF (169) InlineFunctionExpr")
    internal inner class ParamList_InlineFunctionExpr {
        @Test
        @DisplayName("variadic; XQuery IntelliJ Plugin >= 1.3")
        fun supported() {
            val file = parse<XQueryModule>("function f(\$a as xs:string ...) { 2 }")[0]
            validator.configuration = VERSION_1_4
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(report.toString(), `is`(""))
        }

        @Test
        @DisplayName("variadic; XQuery IntelliJ Plugin < 1.3")
        fun notSupported() {
            val file = parse<XQueryModule>("function (\$a as xs:string ...) { 2 }")[0]
            validator.configuration = VERSION_1_3
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(
                report.toString(), `is`(
                    """
                    E XPST0003(26:29): XQuery IntelliJ Plugin 1.3 does not support XQuery IntelliJ Plugin 1.4 constructs.
                    """.trimIndent()
                )
            )
        }

        @Test
        @DisplayName("non-variadic")
        fun nonVariadic() {
            val file = parse<XQueryModule>("function (\$a as xs:string) { 2 }")[0]
            validator.configuration = VERSION_1_0
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(report.toString(), `is`(""))
        }
    }

    @Nested
    @DisplayName("XQuery IntelliJ Plugin EBNF (86) SequenceTypeUnion")
    internal inner class SequenceTypeUnion {
        @Test
        @DisplayName("XQuery IntelliJ Plugin >= 1.3")
        fun supported() {
            val file = parse<XQueryModule>("1 instance of (xs:string*|element(test))")[0]
            validator.configuration = VERSION_1_3
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(report.toString(), `is`(""))
        }

        @Test
        @DisplayName("XQuery IntelliJ Plugin < 1.3")
        fun notSupported() {
            val file = parse<XQueryModule>("1 instance of (xs:string*|element(test))")[0]
            validator.configuration = VERSION_1_0
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(
                report.toString(), `is`(
                    """
                    E XPST0003(25:26): XQuery IntelliJ Plugin 1.0 does not support XQuery IntelliJ Plugin 1.3 constructs.
                    """.trimIndent()
                )
            )
        }

        @Test
        @DisplayName("XQuery 1.0 EBNF (43) TypeswitchExpr ; XQuery 1.0 EBNF (44) CaseClause")
        fun caseClause() {
            val file = parse<XQueryModule>("typeswitch (1) case xs:string*|element(test) return 1 default return 2")[0]
            validator.configuration = VERSION_1_0
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(report.toString(), `is`(""))
        }
    }

    @Nested
    @DisplayName("XQuery IntelliJ Plugin EBNF (87) SequenceTypeList")
    internal inner class SequenceTypeList {
        @Test
        @DisplayName("XQuery IntelliJ Plugin >= 1.3")
        fun supported() {
            val file = parse<XQueryModule>("1 instance of (xs:string*, element(test))")[0]
            validator.configuration = VERSION_1_3
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(report.toString(), `is`(""))
        }

        @Test
        @DisplayName("XQuery IntelliJ Plugin < 1.3")
        fun notSupported() {
            val file = parse<XQueryModule>("1 instance of (xs:string*, element(test))")[0]
            validator.configuration = VERSION_1_0
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(
                report.toString(), `is`(
                    """
                    E XPST0003(25:26): XQuery IntelliJ Plugin 1.0 does not support XQuery IntelliJ Plugin 1.3 constructs.
                    """.trimIndent()
                )
            )
        }

        @Test
        @DisplayName("XQuery 3.0 EBNF (193) TypedFunctionTest")
        fun typedFunctionTest() {
            val file = parse<XQueryModule>("1 instance of function (xs:string*, element(test)) as item()")[0]
            validator.configuration = VERSION_1_0
            validator.validate(file, this@XQueryIntelliJPluginSyntaxValidatorTest)
            assertThat(report.toString(), `is`(""))
        }
    }
}
