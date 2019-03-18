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
package uk.co.reecedunn.intellij.plugin.existdb.tests.query.rest

import org.hamcrest.CoreMatchers.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.co.reecedunn.intellij.plugin.core.tests.assertion.assertThat
import uk.co.reecedunn.intellij.plugin.existdb.query.rest.EXistDBQueryError

@DisplayName("IntelliJ - Base Platform - Run Configuration - XQuery Processor - EXistDBQueryError")
class EXistDBQueryErrorTest {
    @Test
    @DisplayName("XPathException")
    fun xpathException() {
        @Language("xml")
        val exception = """<?xml version="1.0" ?>
            <exception>
                <path>/db</path>
                <message>exerr:ERROR org.exist.xquery.XPathException: err:XPST0003 unexpected token: null [at line 1, column 7]
</message>
            </exception>
        """

        val e = EXistDBQueryError(exception)
        assertThat(e.standardCode, `is`("XPST0003"))
        assertThat(e.vendorCode, `is`(nullValue()))
        assertThat(e.description, `is`("unexpected token: null"))
        assertThat(e.frame.module, `is`("/db"))
        assertThat(e.frame.lineNumber, `is`(1))
        assertThat(e.frame.columnNumber, `is`(7))
    }

    @Test
    @DisplayName("type error")
    fun typeError() {
        @Language("xml")
        val exception = """<?xml version="1.0" ?>
            <exception>
                <path>/db</path>
                <message>exerr:ERROR Type: xs:dateTimeStamp is not defined</message>
            </exception>
        """

        val e = EXistDBQueryError(exception)
        assertThat(e.standardCode, `is`("FOER0000"))
        assertThat(e.vendorCode, `is`(nullValue()))
        assertThat(e.description, `is`("xs:dateTimeStamp is not defined"))
        assertThat(e.frame.module, `is`("/db"))
        assertThat(e.frame.lineNumber, `is`(nullValue()))
        assertThat(e.frame.columnNumber, `is`(nullValue()))
    }

    @Test
    @DisplayName("error")
    fun error() {
        @Language("xml")
        val exception = """<?xml version="1.0" ?>
            <exception>
                <path>/db</path>
                <message>err:XPTY0004 Too many operands at the left of * [at line 1, column 11, source: (1, 2, 3) * 2]</message>
            </exception>
        """

        val e = EXistDBQueryError(exception)
        assertThat(e.standardCode, `is`("XPTY0004"))
        assertThat(e.vendorCode, `is`(nullValue()))
        assertThat(e.description, `is`("Too many operands at the left of *"))
        assertThat(e.frame.module, `is`("/db"))
        assertThat(e.frame.lineNumber, `is`(1))
        assertThat(e.frame.columnNumber, `is`(11))
    }
}
