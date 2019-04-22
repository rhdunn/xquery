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
package uk.co.reecedunn.intellij.plugin.processor.tests

import com.intellij.credentialStore.PasswordSafeSettings
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.passwordSafe.impl.PasswordSafeImpl
import com.intellij.mock.MockProjectEx
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PlatformLiteFixture
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.junit.Assume
import org.junit.jupiter.api.*
import uk.co.reecedunn.intellij.plugin.core.tests.assertion.assertThat
import uk.co.reecedunn.intellij.plugin.basex.query.session.BaseXSession
import uk.co.reecedunn.intellij.plugin.existdb.query.rest.EXistDBRest
import uk.co.reecedunn.intellij.plugin.intellij.lang.XQuery
import uk.co.reecedunn.intellij.plugin.marklogic.query.rest.MarkLogicRest
import uk.co.reecedunn.intellij.plugin.processor.query.*
import uk.co.reecedunn.intellij.plugin.saxon.query.s9api.SaxonS9API
import java.util.concurrent.ExecutionException

@Suppress("Reformat")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("XQuery processor")
private class ProcessorTest : PlatformLiteFixture() {
    val home = System.getProperty("user.home")
    // Modify these for the processor being tested:
    val processorVersion = "9.0"
    var provider: QueryProcessorSettings? = null

    private fun createQuery(query: String): RunnableQuery {
        return (provider!!.session as RunnableQueryProvider).createRunnableQuery(createVirtualFile(query), XQuery)
    }


    @BeforeAll
    override fun setUp() {
        super.setUp()
        initApplication()
        Extensions.registerAreaClass("IDEA_PROJECT", null)
        myProject = MockProjectEx(testRootDisposable)
        registerApplicationService(PasswordSafe::class.java, PasswordSafeImpl(PasswordSafeSettings()))

        registerExtensionPoint(QueryProcessorApi.QUERY_PROCESSOR_API_EP, QueryProcessorApi::class.java)
        registerExtension(QueryProcessorApi.QUERY_PROCESSOR_API_EP, BaseXSession)
        registerExtension(QueryProcessorApi.QUERY_PROCESSOR_API_EP, EXistDBRest)
        registerExtension(QueryProcessorApi.QUERY_PROCESSOR_API_EP, MarkLogicRest)
        registerExtension(QueryProcessorApi.QUERY_PROCESSOR_API_EP, SaxonS9API)

        provider = QueryProcessorSettings(
            name = "Test Instance",
            apiId = BaseXSession.id,
            jar = "$home/xquery/basex/basex-9.0/BaseX.jar",
            configurationPath = null,
            connection = null
        )
    }

    @AfterAll
    override fun tearDown() {
        super.tearDown()
        provider?.close()
    }

    private fun createVirtualFile(query: String): VirtualFile {
        val file = LightVirtualFile("test.xqy", query)
        file.charset = CharsetToolkit.UTF8_CHARSET
        return file
    }

    protected fun <T> registerApplicationService(aClass: Class<T>, `object`: T) {
        PlatformLiteFixture.getApplication().registerService(aClass, `object`)
        Disposer.register(myProject, com.intellij.openapi.Disposable {
            PlatformLiteFixture.getApplication().picoContainer.unregisterComponent(aClass.name)
        })
    }

    @Test @DisplayName("version") fun version() {
        assertThat(provider!!.session.version.execute().get(), `is`(processorVersion))
    }

    @Nested
    @DisplayName("return value type display name")
    internal inner class ReturnValues {
        private fun node(query: String, valueMatcher: Matcher<String>, typeMatcher: Matcher<String>, mimetype: String) {
            val q = createQuery(query)
            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, valueMatcher)
            assertThat(items[0].type, typeMatcher)
            assertThat(items[0].mimetype, `is`(mimetype))
        }

        private fun node(query: String, value: String, type: String, mimetype: String) {
            node(query, `is`(value), `is`(type), mimetype)
        }

        private fun atomic(value: String, type: String, valueMatcher: Matcher<String>, typeMatcher: Matcher<String>) {
            val q = createQuery("\"$value\" cast as $type")
            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, valueMatcher)
            assertThat(items[0].mimetype, `is`("text/plain"))

            Assume.assumeThat("Skipping type check for Saxon 9.5.1 and earlier.", items[0].type, `is`(not("text()")))
            Assume.assumeThat("Skipping type check for BaseX 7.0 and earlier.", items[0].type, `is`(not("item()")))
            assertThat(items[0].type, typeMatcher)
        }

        private fun atomic_values(value: String, type: String, valueMatcher: Matcher<String>) {
            atomic(value, type, valueMatcher, `is`(type))
        }

        private fun atomic_types(value: String, type: String, typeMatcher: Matcher<String>) {
            atomic(value, type, `is`(value), typeMatcher)
        }

        private fun atomic(value: String, type: String) {
            atomic(value, type, `is`(value), `is`(type))
        }

        @Nested
        @DisplayName("sequence type")
        internal inner class SequenceType {
            @Test @DisplayName("empty-sequence()") fun emptySequence() {
                val q = createQuery("()")
                val items = q.run().execute().get().results
                q.close()

                assertThat(items.size, `is`(0))
            }

            @Test @DisplayName("sequence (same type values)") fun sequenceSameTypeValues() {
                val q = createQuery("(1, 2, 3)")
                val items = q.run().execute().get().results
                q.close()

                assertThat(items.size, `is`(3))

                assertThat(items[0].value, `is`("1"))
                assertThat(items[0].type, `is`("xs:integer"))
                assertThat(items[0].mimetype, `is`("text/plain"))

                assertThat(items[1].value, `is`("2"))
                assertThat(items[1].type, `is`("xs:integer"))
                assertThat(items[1].mimetype, `is`("text/plain"))

                assertThat(items[2].value, `is`("3"))
                assertThat(items[2].type, `is`("xs:integer"))
                assertThat(items[2].mimetype, `is`("text/plain"))
            }

            @Test @DisplayName("sequence (different type values)") fun sequenceDifferentTypeValues() {
                val q = createQuery("(1 cast as xs:int, 2 cast as xs:byte, 3 cast as xs:decimal)")
                val items = q.run().execute().get().results
                q.close()

                assertThat(items.size, `is`(3))

                assertThat(items[0].value, `is`("1"))
                assertThat(items[0].type, `is`("xs:int"))
                assertThat(items[0].mimetype, `is`("text/plain"))

                assertThat(items[1].value, `is`("2"))
                assertThat(items[1].type, `is`("xs:byte"))
                assertThat(items[1].mimetype, `is`("text/plain"))

                assertThat(items[2].value, `is`("3"))
                assertThat(items[2].type, `is`("xs:decimal"))
                assertThat(items[2].mimetype, `is`("text/plain"))
            }
        }

        @Nested
        @DisplayName("node type (XQuery 3.0)")
        internal inner class NodeTypeXQuery30 {
            @Test @DisplayName("attribute()") fun attribute() {
                node(
                    "attribute lorem {\"ipsum\"}",
                    anyOf(`is`("lorem=\"ipsum\""), `is`("ipsum")),
                    anyOf(`is`("attribute()"), `is`("attribute(Q{}lorem)")),
                    "text/plain"
                )
            }
            @Test @DisplayName("comment()") fun comment() {
                node("comment {\"lorem ipsum\"}", "<!--lorem ipsum-->", "comment()", "text/plain")
            }
            @Test @DisplayName("document-node()") fun documentNode() {
                node(
                    "document { <test/> }",
                    anyOf(`is`("<test/>"), `is`("<test/>\n"), `is`("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<test/>")),
                    anyOf(`is`("document-node(element())()"), `is`("document-node(element(Q{}test))"), `is`("document-node()")),
                    "application/xml"
                )
            }
            @Test @DisplayName("element()") fun element() {
                node(
                    "<test/>",
                    anyOf(`is`("<test/>"), `is`("<test/>\n")),
                    anyOf(`is`("element()"), `is`("element(Q{}test)")),
                    "application/xml"
                )
            }
            @Test @DisplayName("function()") fun function() {
                node("fn:true#0", "fn:true#0", "function(*)", "text/plain")
            }
            @Test @DisplayName("processing-instruction()") fun processingInstruction() {
                node("<?test?>", anyOf(`is`("<?test?>"), `is`("<?test ?>")), `is`("processing-instruction()"), "text/plain")
            }
            @Test @DisplayName("text()") fun text() {
                node("text {\"lorem ipsum\"}", "lorem ipsum", "text()", "text/plain")
            }
        }

        @Nested
        @DisplayName("node type (XQuery 3.1)")
        internal inner class NodeTypeXQuery31 {
            @Test @DisplayName("array(*)") fun array() {
                node("array {}", `is`("[]"), anyOf(`is`("function(*)"), `is`("array(empty-sequence())")), "text/plain")
            }
            @Test @DisplayName("map(*)") fun map() {
                node("map {}", anyOf(`is`("map{}"), `is`("map {\r\n}")), anyOf(`is`("function(*)"), `is`("map(*)")), "text/plain")
            }
        }

        @Nested
        @DisplayName("node type (MarkLogic)")
        internal inner class NodeTypeMarkLogic {
            @Test @DisplayName("array-node()") fun arrayNode() {
                node("array-node {}", "[]", "array-node()", "application/json")
            }
            @Test @DisplayName("binary()") fun binary() {
                node("binary {\"6C6F72656D20697073756D\"}", "lorem ipsum", "binary()", "application/octet-stream")
            }
            @Test @DisplayName("boolean-node()") fun booleanNode() {
                node("boolean-node { false() }", "false", "boolean-node()", "application/json")
            }
            @Test @DisplayName("null-node()") fun nullNode() {
                node("null-node {}", "null", "null-node()", "application/json")
            }
            @Test @DisplayName("number-node()") fun numberNode() {
                node("number-node { 2 }", "2", "number-node()", "application/json")
            }
            @Test @DisplayName("object-node()") fun objectNode() {
                node("object-node {}", "{}", "object-node()", "application/json")
            }
        }

        @Nested
        @DisplayName("atomic type (primitive)")
        internal inner class AtomicTypePrimitive {
            @Test @DisplayName("xs:anyURI") fun xsAnyURI() { atomic("http://www.example.co.uk", "xs:anyURI") }
            @Test @DisplayName("xs:base64Binary") fun xsBase64Binary() { atomic_values("bG9yZW0gaXBzdW0=", "xs:base64Binary", anyOf(`is`("bG9yZW0gaXBzdW0="), `is`("lorem ipsum"))) }
            @Test @DisplayName("xs:boolean") fun xsBoolean() { atomic("true", "xs:boolean") }
            @Test @DisplayName("xs:date") fun xsDate() { atomic("1995-10-12", "xs:date") }
            @Test @DisplayName("xs:dateTime") fun xsDateTime() { atomic("1995-10-12T11:22:33.444", "xs:dateTime") }
            @Test @DisplayName("xs:dayTimeDuration") fun xsDayTimeDuration() { atomic("P5D", "xs:dayTimeDuration") }
            @Test @DisplayName("xs:decimal") fun xsDecimal() { atomic("2.5", "xs:decimal") }
            @Test @DisplayName("xs:double") fun xsDouble() { atomic("2.5", "xs:double") }
            @Test @DisplayName("xs:duration") fun xsDuration() { atomic("P5D", "xs:duration") }
            @Test @DisplayName("xs:float") fun xsFloat() { atomic("2.5", "xs:float") }
            @Test @DisplayName("xs:gDay") fun xsGDay() { atomic("---12", "xs:gDay") }
            @Test @DisplayName("xs:gMonth") fun xsGMonth() { atomic("--10", "xs:gMonth") }
            @Test @DisplayName("xs:gMonthDay") fun xsGMonthDay() { atomic("--10-12", "xs:gMonthDay") }
            @Test @DisplayName("xs:gYear") fun xsGYear() { atomic("1995", "xs:gYear") }
            @Test @DisplayName("xs:gYearMonth") fun xsGYearMonth() { atomic("1995-10", "xs:gYearMonth") }
            @Test @DisplayName("xs:hexBinary") fun xsHexBinary() { atomic_values("6C6F72656D20697073756D", "xs:hexBinary", anyOf(`is`("6C6F72656D20697073756D"), `is`("lorem ipsum"))) }
            @Test @DisplayName("xs:integer") fun xsInteger() { atomic("2", "xs:integer") }
            @Test @DisplayName("xs:string") fun xsString() { atomic_values("lorem &amp; ipsum", "xs:string", `is`("lorem & ipsum")) }
            @Test @DisplayName("xs:time") fun xsTime() { atomic("11:22:33.444", "xs:time") }
            @Test @DisplayName("xs:untypedAtomic") fun xsUntypedAtomic() { atomic("2", "xs:untypedAtomic") }
            @Test @DisplayName("xs:yearMonthDuration") fun xsYearMonthDuration() { atomic("P5M", "xs:yearMonthDuration") }

            @Test @DisplayName("xs:QName") fun xsQName() { atomic("xs:string", "xs:QName") }

            @Test @DisplayName("xs:string (empty)") fun xsStringEmpty() { atomic("", "xs:string") }
            @Test @DisplayName("xs:string (unicode)") fun xsStringUnicode() { atomic("•\uD834\uDD1E", "xs:string") }
        }

        @Nested
        @DisplayName("atomic type (derived)")
        internal inner class AtomicTypeDerived {
            @Test @DisplayName("xs:byte") fun xsByte() { atomic("2", "xs:byte") }
            @Test @DisplayName("xs:int") fun xsInt() { atomic("2", "xs:int") }
            @Test @DisplayName("xs:language") fun xsLanguage() { atomic("fr-FR", "xs:language") }
            @Test @DisplayName("xs:long") fun xsLong() { atomic("2", "xs:long") }
            @Test @DisplayName("xs:negativeInteger") fun xsNegativeInteger() { atomic("-2", "xs:negativeInteger") }
            @Test @DisplayName("xs:nonNegativeInteger") fun xsNonNegativeInteger() { atomic("2", "xs:nonNegativeInteger") }
            @Test @DisplayName("xs:nonPositiveInteger") fun xsNonPositiveInteger() { atomic("-2", "xs:nonPositiveInteger") }
            @Test @DisplayName("xs:normalizedString") fun xsNormalizedString() { atomic("lorem ipsum", "xs:normalizedString") }
            @Test @DisplayName("xs:positiveInteger") fun xsPositiveInteger() { atomic("2", "xs:positiveInteger") }
            @Test @DisplayName("xs:short") fun xsShort() { atomic("2", "xs:short") }
            @Test @DisplayName("xs:token") fun xsToken() { atomic("2", "xs:token") }
            @Test @DisplayName("xs:unsignedByte") fun xsUnsignedByte() { atomic("2", "xs:unsignedByte") }
            @Test @DisplayName("xs:unsignedInt") fun xsUnsignedInt() { atomic("2", "xs:unsignedInt") }
            @Test @DisplayName("xs:unsignedLong") fun xsUnsignedLong() { atomic("2", "xs:unsignedLong") }
            @Test @DisplayName("xs:unsignedShort") fun xsUnsignedShort() { atomic("2", "xs:unsignedShort") }

            @Test @DisplayName("xs:ENTITY") fun xsENTITY() { atomic("lorem-ipsum", "xs:ENTITY") }
            @Test @DisplayName("xs:ID") fun xsID() { atomic("lorem-ipsum", "xs:ID") }
            @Test @DisplayName("xs:IDREF") fun xsIDREF() { atomic("lorem-ipsum", "xs:IDREF") }
            @Test @DisplayName("xs:Name") fun xsName() { atomic("lorem-ipsum", "xs:Name") }
            @Test @DisplayName("xs:NCName") fun xsNCName() { atomic("lorem-ipsum", "xs:NCName") }
            @Test @DisplayName("xs:NMTOKEN") fun xsNMTOKEN() { atomic("lorem-ipsum", "xs:NMTOKEN") }
        }

        @Nested
        @DisplayName("atomic type (derived; XML Schema 1.1 Part 2)")
        internal inner class AtomicTypeDerivedXSD11 {
            @Test @DisplayName("xs:dateTimeStamp") fun xsDateTimeStamp() { atomic_types("1995-10-12T11:22:33.444Z", "xs:dateTimeStamp", anyOf(`is`("xs:dateTimeStamp"), `is`("xs:dateTime"))) }
        }

        @Nested
        @DisplayName("union type")
        internal inner class UnionType {
            @Test @DisplayName("xs:numeric") fun xsNumeric() { atomic_types("2", "xs:numeric", anyOf(`is`("xs:double"), `is`("xs:anySimpleType"))) }
        }
    }

    @Nested
    @DisplayName("bind variable")
    internal inner class BindVariableName {
        @Test @DisplayName("by NCName") fun ncname() {
            val q = createQuery("declare variable \$x external; \$x")
            q.bindVariable("x", "2", "xs:integer")

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, `is`("2"))
            assertThat(items[0].mimetype, `is`("text/plain"))

            Assume.assumeThat("Skipping type check for Saxon 9.5.1 and earlier.", items[0].type, `is`(not("text()")))
            Assume.assumeThat("Skipping type check for BaseX 7.0 and earlier.", items[0].type, `is`(not("item()")))
            assertThat(items[0].type, `is`("xs:integer"))
        }
        @Test @DisplayName("by URIQualifiedName") fun uriQualifiedName() {
            val q = createQuery("declare variable \$Q{http://www.example.co.uk}x external; \$x")
            q.bindVariable("Q{http://www.example.co.uk}x", "2", "xs:integer")

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, `is`("2"))
            assertThat(items[0].type, `is`("xs:integer"))
            assertThat(items[0].mimetype, `is`("text/plain"))
        }
        @Test @DisplayName("by QName") fun qname() {
            val q = createQuery("declare variable \$local:x external; \$x")
            q.bindVariable("local:x", "2", "xs:integer")

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, `is`("2"))
            assertThat(items[0].type, `is`("xs:integer"))
            assertThat(items[0].mimetype, `is`("text/plain"))
        }

        private fun node(value: String, type: String, valueMatcher: Matcher<String>, typeMatcher: Matcher<String>, mimetype: String) {
            val q = createQuery("declare variable \$x external; \$x")
            q.bindVariable("x", value, type)

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, valueMatcher)
            assertThat(items[0].type, typeMatcher)
            assertThat(items[0].mimetype, `is`(mimetype))
        }

        private fun atomic(value: String, type: String, valueMatcher: Matcher<String>, typeMatcher: Matcher<String>) {
            val q = createQuery("declare variable \$x external; \$x")
            q.bindVariable("x", value, type)

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, valueMatcher)
            assertThat(items[0].mimetype, `is`("text/plain"))

            Assume.assumeThat("Skipping type check for Saxon 9.5.1 and earlier.", items[0].type, `is`(not("text()")))
            Assume.assumeThat("Skipping type check for BaseX 7.0 and earlier.", items[0].type, `is`(not("item()")))
            assertThat(items[0].type, typeMatcher)
        }

        private fun atomic_values(value: String, type: String, valueMatcher: Matcher<String>) {
            atomic(value, type, valueMatcher, `is`(type))
        }

        private fun atomic_types(value: String, type: String, typeMatcher: Matcher<String>) {
            atomic(value, type, `is`(value), typeMatcher)
        }

        private fun atomic(value: String, type: String) {
            atomic(value, type, `is`(value), `is`(type))
        }

        @Test @DisplayName("as null") fun nullValue() {
            val q = createQuery("declare variable \$x external; \$x")
            q.bindVariable("x", null, "empty-sequence()")

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(0))
        }

        @Nested
        @DisplayName("as node type (XML)")
        internal inner class NodeTypeXml {
            @Test @DisplayName("element()") fun element() { node("<test/>", "element()", `is`("<test/>"), `is`("element()"), "application/xml") }
            @Test @DisplayName("document-node()") fun documentNode() { node("<?xml version=\"1.0\"?><test/>", "document-node()", `is`("<test/>"), `is`("document-node(element())()"), "application/xml") }
        }

        @Nested
        @DisplayName("as node type (JSON)")
        internal inner class NodeTypeJson {
            @Test @DisplayName("array(*)") fun array() { node("[]", "array(*)", `is`("[]"), `is`("array(*)"), "application/json") }
            @Test @DisplayName("map(*)") fun map() { node("{}", "map(*)", `is`("{}"), `is`("map(*)"), "application/json") }

            @Test @DisplayName("array-node()") fun arrayNode() { node("[]", "array-node()", `is`("[]"), `is`("array-node()"), "application/json") }
            @Test @DisplayName("object-node()") fun objectNode() { node("{}", "object-node()", `is`("{}"), `is`("object-node()"), "application/json") }
        }

        @Nested
        @DisplayName("as sequence type")
        internal inner class SequenceType {
            @Test @DisplayName("empty-sequence()") fun emptySequence() {
                val q = createQuery("declare variable \$x external; \$x")
                q.bindVariable("x", "()", "empty-sequence()")

                val items = q.run().execute().get().results
                q.close()

                assertThat(items.size, `is`(0))
            }
        }

        @Nested
        @DisplayName("as atomic type (primitive)")
        internal inner class AtomicTypePrimitive {
            @Test @DisplayName("xs:anyURI") fun xsAnyURI() { atomic("http://www.example.co.uk", "xs:anyURI") }
            @Test @DisplayName("xs:base64Binary") fun xsBase64Binary() { atomic_values("bG9yZW0gaXBzdW0=", "xs:base64Binary", anyOf(`is`("bG9yZW0gaXBzdW0="), `is`("lorem ipsum"))) }
            @Test @DisplayName("xs:boolean") fun xsBoolean() { atomic("true", "xs:boolean") }
            @Test @DisplayName("xs:date") fun xsDate() { atomic("1995-10-12", "xs:date") }
            @Test @DisplayName("xs:dateTime") fun xsDateTime() { atomic("1995-10-12T11:22:33.444", "xs:dateTime") }
            @Test @DisplayName("xs:dayTimeDuration") fun xsDayTimeDuration() { atomic("P5D", "xs:dayTimeDuration") }
            @Test @DisplayName("xs:decimal") fun xsDecimal() { atomic("2.5", "xs:decimal") }
            @Test @DisplayName("xs:double") fun xsDouble() { atomic("2.5", "xs:double") }
            @Test @DisplayName("xs:duration") fun xsDuration() { atomic("P5D", "xs:duration") }
            @Test @DisplayName("xs:float") fun xsFloat() { atomic("2.5", "xs:float") }
            @Test @DisplayName("xs:gDay") fun xsGDay() { atomic("---12", "xs:gDay") }
            @Test @DisplayName("xs:gMonth") fun xsGMonth() { atomic("--10", "xs:gMonth") }
            @Test @DisplayName("xs:gMonthDay") fun xsGMonthDay() { atomic("--10-12", "xs:gMonthDay") }
            @Test @DisplayName("xs:gYear") fun xsGYear() { atomic("1995", "xs:gYear") }
            @Test @DisplayName("xs:gYearMonth") fun xsGYearMonth() { atomic("1995-10", "xs:gYearMonth") }
            @Test @DisplayName("xs:hexBinary") fun xsHexBinary() { atomic_values("6C6F72656D20697073756D", "xs:hexBinary", anyOf(`is`("6C6F72656D20697073756D"), `is`("lorem ipsum"))) }
            @Test @DisplayName("xs:integer") fun xsInteger() { atomic("2", "xs:integer") }
            @Test @DisplayName("xs:string") fun xsString() { atomic("lorem & ipsum", "xs:string") }
            @Test @DisplayName("xs:time") fun xsTime() { atomic("11:22:33.444", "xs:time") }
            @Test @DisplayName("xs:untypedAtomic") fun xsUntypedAtomic() { atomic("2", "xs:untypedAtomic") }
            @Test @DisplayName("xs:yearMonthDuration") fun xsYearMonthDuration() { atomic("P5M", "xs:yearMonthDuration") }

            @Test @DisplayName("xs:QName") fun xsQName() { atomic("xs:string", "xs:QName") }
        }

        @Nested
        @DisplayName("as atomic type (derived)")
        internal inner class AtomicTypeDerived {
            @Test @DisplayName("xs:byte") fun xsByte() { atomic("2", "xs:byte") }
            @Test @DisplayName("xs:int") fun xsInt() { atomic("2", "xs:int") }
            @Test @DisplayName("xs:language") fun xsLanguage() { atomic("fr-FR", "xs:language") }
            @Test @DisplayName("xs:long") fun xsLong() { atomic("2", "xs:long") }
            @Test @DisplayName("xs:negativeInteger") fun xsNegativeInteger() { atomic("-2", "xs:negativeInteger") }
            @Test @DisplayName("xs:nonNegativeInteger") fun xsNonNegativeInteger() { atomic("2", "xs:nonNegativeInteger") }
            @Test @DisplayName("xs:nonPositiveInteger") fun xsNonPositiveInteger() { atomic("-2", "xs:nonPositiveInteger") }
            @Test @DisplayName("xs:normalizedString") fun xsNormalizedString() { atomic("lorem ipsum", "xs:normalizedString") }
            @Test @DisplayName("xs:positiveInteger") fun xsPositiveInteger() { atomic("2", "xs:positiveInteger") }
            @Test @DisplayName("xs:short") fun xsShort() { atomic("2", "xs:short") }
            @Test @DisplayName("xs:token") fun xsToken() { atomic("2", "xs:token") }
            @Test @DisplayName("xs:unsignedByte") fun xsUnsignedByte() { atomic("2", "xs:unsignedByte") }
            @Test @DisplayName("xs:unsignedInt") fun xsUnsignedInt() { atomic("2", "xs:unsignedInt") }
            @Test @DisplayName("xs:unsignedLong") fun xsUnsignedLong() { atomic("2", "xs:unsignedLong") }
            @Test @DisplayName("xs:unsignedShort") fun xsUnsignedShort() { atomic("2", "xs:unsignedShort") }

            @Test @DisplayName("xs:ENTITY") fun xsENTITY() { atomic("lorem-ipsum", "xs:ENTITY") }
            @Test @DisplayName("xs:ID") fun xsID() { atomic("lorem-ipsum", "xs:ID") }
            @Test @DisplayName("xs:IDREF") fun xsIDREF() { atomic("lorem-ipsum", "xs:IDREF") }
            @Test @DisplayName("xs:Name") fun xsName() { atomic("lorem-ipsum", "xs:Name") }
            @Test @DisplayName("xs:NCName") fun xsNCName() { atomic("lorem-ipsum", "xs:NCName") }
            @Test @DisplayName("xs:NMTOKEN") fun xsNMTOKEN() { atomic("lorem-ipsum", "xs:NMTOKEN") }
        }

        @Nested
        @DisplayName("as atomic type (derived; XML Schema 1.1 Part 2)")
        internal inner class AtomicTypeDerivedXSD11 {
            @Test @DisplayName("xs:dateTimeStamp") fun xsDateTimeStamp() { atomic_types("1995-10-12T11:22:33.444Z", "xs:dateTimeStamp", anyOf(`is`("xs:dateTimeStamp"), `is`("xs:dateTime"))) }
        }

        @Nested
        @DisplayName("as union type")
        internal inner class UnionType {
            @Test @DisplayName("xs:numeric") fun xsNumeric() { atomic_types("2", "xs:numeric", `is`("xs:double")) }
        }
    }

    @Nested
    @DisplayName("bind context item")
    internal inner class BindContextItem {
        private fun node(value: String, type: String, valueMatcher: Matcher<String>, typeMatcher: Matcher<String>, mimetype: String) {
            val q = createQuery(".")
            q.bindContextItem(value, type)

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, valueMatcher)
            assertThat(items[0].type, typeMatcher)
            assertThat(items[0].mimetype, `is`(mimetype))
        }

        private fun atomic(value: String, type: String, valueMatcher: Matcher<String>, typeMatcher: Matcher<String>) {
            val q = createQuery(".")
            q.bindContextItem(value, type)

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(1))
            assertThat(items[0].value, valueMatcher)
            assertThat(items[0].mimetype, `is`("text/plain"))

            Assume.assumeThat("Skipping type check for Saxon 9.5.1 and earlier.", items[0].type, `is`(not("text()")))
            Assume.assumeThat("Skipping type check for BaseX 7.0 and earlier.", items[0].type, `is`(not("item()")))
            assertThat(items[0].type, typeMatcher)
        }

        private fun atomic_values(value: String, type: String, valueMatcher: Matcher<String>) {
            atomic(value, type, valueMatcher, `is`(type))
        }

        private fun atomic_types(value: String, type: String, typeMatcher: Matcher<String>) {
            atomic(value, type, `is`(value), typeMatcher)
        }

        private fun atomic(value: String, type: String) {
            atomic(value, type, `is`(value), `is`(type))
        }

        @Test @DisplayName("as null") fun nullValue() {
            val q = createQuery(".")
            q.bindContextItem(null, "empty-sequence()")

            val items = q.run().execute().get().results
            q.close()

            assertThat(items.size, `is`(0))
        }

        @Nested
        @DisplayName("as sequence type")
        internal inner class SequenceType {
            @Test @DisplayName("empty-sequence()") fun emptySequence() {
                val q = createQuery(".")
                q.bindContextItem("()", "empty-sequence()")

                val items = q.run().execute().get().results
                q.close()

                assertThat(items.size, `is`(0))
            }
        }

        @Nested
        @DisplayName("as node type (XML)")
        internal inner class NodeTypeXml {
            @Test @DisplayName("element()") fun element() { node("<test/>", "element()", `is`("<test/>"), `is`("element()"), "application/xml") }
            @Test @DisplayName("document-node()") fun documentNode() { node("<?xml version=\"1.0\"?><test/>", "document-node(element())()", `is`("<test/>"), `is`("document-node(element())()"), "application/xml") }
        }

        @Nested
        @DisplayName("as node type (JSON)")
        internal inner class NodeTypeJson {
            @Test @DisplayName("array(*)") fun array() { node("[]", "array(*)", `is`("[]"), `is`("array(*)"), "application/json") }
            @Test @DisplayName("map(*)") fun map() { node("{}", "map(*)", `is`("{}"), `is`("map(*)"), "application/json") }

            @Test @DisplayName("array-node()") fun arrayNode() { node("[]", "array-node()", `is`("[]"), `is`("array-node()"), "application/json") }
            @Test @DisplayName("object-node()") fun objectNode() { node("{}", "object-node()", `is`("{}"), `is`("object-node()"), "application/json") }
        }

        @Nested
        @DisplayName("as atomic type (primitive)")
        internal inner class AtomicTypePrimitive {
            @Test @DisplayName("xs:anyURI") fun xsAnyURI() { atomic("http://www.example.co.uk", "xs:anyURI") }
            @Test @DisplayName("xs:base64Binary") fun xsBase64Binary() { atomic_values("bG9yZW0gaXBzdW0=", "xs:base64Binary", anyOf(`is`("bG9yZW0gaXBzdW0="), `is`("lorem ipsum"))) }
            @Test @DisplayName("xs:boolean") fun xsBoolean() { atomic("true", "xs:boolean") }
            @Test @DisplayName("xs:date") fun xsDate() { atomic("1995-10-12", "xs:date") }
            @Test @DisplayName("xs:dateTime") fun xsDateTime() { atomic("1995-10-12T11:22:33.444", "xs:dateTime") }
            @Test @DisplayName("xs:dayTimeDuration") fun xsDayTimeDuration() { atomic("P5D", "xs:dayTimeDuration") }
            @Test @DisplayName("xs:decimal") fun xsDecimal() { atomic("2.5", "xs:decimal") }
            @Test @DisplayName("xs:double") fun xsDouble() { atomic("2.5", "xs:double") }
            @Test @DisplayName("xs:duration") fun xsDuration() { atomic("P5D", "xs:duration") }
            @Test @DisplayName("xs:float") fun xsFloat() { atomic("2.5", "xs:float") }
            @Test @DisplayName("xs:gDay") fun xsGDay() { atomic("---12", "xs:gDay") }
            @Test @DisplayName("xs:gMonth") fun xsGMonth() { atomic("--10", "xs:gMonth") }
            @Test @DisplayName("xs:gMonthDay") fun xsGMonthDay() { atomic("--10-12", "xs:gMonthDay") }
            @Test @DisplayName("xs:gYear") fun xsGYear() { atomic("1995", "xs:gYear") }
            @Test @DisplayName("xs:gYearMonth") fun xsGYearMonth() { atomic("1995-10", "xs:gYearMonth") }
            @Test @DisplayName("xs:hexBinary") fun xsHexBinary() { atomic_values("6C6F72656D20697073756D", "xs:hexBinary", anyOf(`is`("6C6F72656D20697073756D"), `is`("lorem ipsum"))) }
            @Test @DisplayName("xs:integer") fun xsInteger() { atomic("2", "xs:integer") }
            @Test @DisplayName("xs:string") fun xsString() { atomic("lorem & ipsum", "xs:string") }
            @Test @DisplayName("xs:time") fun xsTime() { atomic("11:22:33.444", "xs:time") }
            @Test @DisplayName("xs:untypedAtomic") fun xsUntypedAtomic() { atomic("2", "xs:untypedAtomic") }
            @Test @DisplayName("xs:yearMonthDuration") fun xsYearMonthDuration() { atomic("P5M", "xs:yearMonthDuration") }

            @Test @DisplayName("xs:QName") fun xsQName() { atomic("xs:string", "xs:QName") }
        }

        @Nested
        @DisplayName("as atomic type (derived)")
        internal inner class AtomicTypeDerived {
            @Test @DisplayName("xs:byte") fun xsByte() { atomic("2", "xs:byte") }
            @Test @DisplayName("xs:int") fun xsInt() { atomic("2", "xs:int") }
            @Test @DisplayName("xs:language") fun xsLanguage() { atomic("fr-FR", "xs:language") }
            @Test @DisplayName("xs:long") fun xsLong() { atomic("2", "xs:long") }
            @Test @DisplayName("xs:negativeInteger") fun xsNegativeInteger() { atomic("-2", "xs:negativeInteger") }
            @Test @DisplayName("xs:nonNegativeInteger") fun xsNonNegativeInteger() { atomic("2", "xs:nonNegativeInteger") }
            @Test @DisplayName("xs:nonPositiveInteger") fun xsNonPositiveInteger() { atomic("-2", "xs:nonPositiveInteger") }
            @Test @DisplayName("xs:normalizedString") fun xsNormalizedString() { atomic("lorem ipsum", "xs:normalizedString") }
            @Test @DisplayName("xs:positiveInteger") fun xsPositiveInteger() { atomic("2", "xs:positiveInteger") }
            @Test @DisplayName("xs:short") fun xsShort() { atomic("2", "xs:short") }
            @Test @DisplayName("xs:token") fun xsToken() { atomic("2", "xs:token") }
            @Test @DisplayName("xs:unsignedByte") fun xsUnsignedByte() { atomic("2", "xs:unsignedByte") }
            @Test @DisplayName("xs:unsignedInt") fun xsUnsignedInt() { atomic("2", "xs:unsignedInt") }
            @Test @DisplayName("xs:unsignedLong") fun xsUnsignedLong() { atomic("2", "xs:unsignedLong") }
            @Test @DisplayName("xs:unsignedShort") fun xsUnsignedShort() { atomic("2", "xs:unsignedShort") }

            @Test @DisplayName("xs:ENTITY") fun xsENTITY() { atomic("lorem-ipsum", "xs:ENTITY") }
            @Test @DisplayName("xs:ID") fun xsID() { atomic("lorem-ipsum", "xs:ID") }
            @Test @DisplayName("xs:IDREF") fun xsIDREF() { atomic("lorem-ipsum", "xs:IDREF") }
            @Test @DisplayName("xs:Name") fun xsName() { atomic("lorem-ipsum", "xs:Name") }
            @Test @DisplayName("xs:NCName") fun xsNCName() { atomic("lorem-ipsum", "xs:NCName") }
            @Test @DisplayName("xs:NMTOKEN") fun xsNMTOKEN() { atomic("lorem-ipsum", "xs:NMTOKEN") }
        }

        @Nested
        @DisplayName("as atomic type (derived; XML Schema 1.1 Part 2)")
        internal inner class AtomicTypeDerivedXSD11 {
            @Test @DisplayName("xs:dateTimeStamp") fun xsDateTimeStamp() { atomic_types("1995-10-12T11:22:33.444Z", "xs:dateTimeStamp", anyOf(`is`("xs:dateTimeStamp"), `is`("xs:dateTime"))) }
        }

        @Nested
        @DisplayName("as union type")
        internal inner class UnionType {
            @Test @DisplayName("xs:numeric") fun xsNumeric() { atomic_types("2", "xs:numeric", `is`("xs:double")) }
        }
    }

    @Nested
    @DisplayName("error")
    internal inner class Error {
        fun parse(query: String): QueryError {
            return Assertions.assertThrows(QueryError::class.java) {
                try {
                    createQuery(query).use { it.run().execute().get().results }
                } catch (e: ExecutionException) {
                    throw e.cause!!
                }
            }
        }

        @Test
        @DisplayName("standard code")
        fun standardCode() {
            assertThat(parse("(1, 2,").standardCode, `is`("XPST0003"))
        }

        @Test
        @DisplayName("vendor code")
        fun vendorCode() {
            assertThat(parse("(1, 2,").vendorCode, anyOf(`is`("XDMP-UNEXPECTED"), `is`(nullValue())))
        }

        @Test
        @DisplayName("description")
        fun description() {
            assertThat(
                parse("(1, 2,").description,
                anyOf(
                    `is`("Incomplete expression."), // BaseX
                    `is`("unexpected token: null"), // eXist-db
                    `is`("Unexpected token"), // MarkLogic
                    `is`("Expected an expression, but reached the end of the input") // Saxon
                )
            )
        }

        @Test
        @DisplayName("module")
        fun module() {
            assertThat(parse("(1, 2,").frames[0].module?.path, `is`("/test.xqy"))
        }

        @Test
        @DisplayName("line number")
        fun lineNumber() {
            assertThat(parse("(1, 2,").frames[0].lineNumber, `is`(1))
        }

        @Test
        @DisplayName("column number")
        fun columnNumber() {
            // On the ',' (e.g. MarkLogic); after the ',' (e.g. BaseX); or at the start of the line:
            assertThat(parse("(1, 2,").frames[0].columnNumber, anyOf(`is`(6), `is`(7), `is`(1)))
        }
    }
}
