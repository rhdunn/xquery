/*
 * Copyright (C) 2017-2018 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xdm.tests

import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.Test
import uk.co.reecedunn.intellij.plugin.core.tests.assertion.assertThat
import uk.co.reecedunn.intellij.plugin.xdm.*
import uk.co.reecedunn.intellij.plugin.xdm.datatype.FORG0001
import uk.co.reecedunn.intellij.plugin.xdm.datatype.FnErrorObject
import uk.co.reecedunn.intellij.plugin.xdm.datatype.XPTY0004
import uk.co.reecedunn.intellij.plugin.xdm.model.XdmTypeCastResult
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

@Suppress("JoinDeclarationAndAssignment", "CanBeVal")
class TypeCasting {
    // region Primitive Types :: xs:float
    //
    // Reference: https://www.w3.org/TR/xpath-functions/#casting-to-numerics
    // Reference: https://www.w3.org/TR/xmlschema11-2/#float

    @Test
    fun testXsFloat_FromXsFloat() {
        var result: XdmTypeCastResult

        result = XsFloat.cast(1.23f, XsFloat)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(1.23f))
    }

    @Test
    fun testXsFloat_FromXsDouble() {
        var result: XdmTypeCastResult

        result = XsFloat.cast(1.2300000190734863, XsDouble)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(1.23f))
    }

    @Test
    fun testXsFloat_FromXsDecimal() {
        var result: XdmTypeCastResult

        result = XsFloat.cast(BigDecimal("1.23"), XsDecimal)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(1.23f))
    }

    @Test
    fun testXsFloat_FromXsInteger() {
        var result: XdmTypeCastResult

        result = XsFloat.cast(BigInteger.valueOf(123), XsInteger)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(123.0f))
    }

    @Test
    fun testXsFloat_FromXsBoolean() {
        var result: XdmTypeCastResult

        result = XsFloat.cast(true, XsBoolean)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(1.0f))

        result = XsFloat.cast(false, XsBoolean)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(0.0f))
    }

    @Test
    fun testXsFloat_FromXsString() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("12", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(12.0f))

        result = XsFloat.cast("-1.23", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(-1.23f))

        result = XsFloat.cast("2.3e8", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(2.3e8f))
    }

    @Test
    fun testXsFloat_FromXsString_Zero() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("-0", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(-0.0f))

        result = XsFloat.cast("0", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(0.0f))

        result = XsFloat.cast("+0", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(0.0f))
    }

    @Test
    fun testXsFloat_FromXsString_Infinity() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("-INF", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(Float.NEGATIVE_INFINITY))

        result = XsFloat.cast("INF", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(Float.POSITIVE_INFINITY))

        // Valid in XSD 1.1, and MarkLogic; Invalid in BaseX.
        result = XsFloat.cast("+INF", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(Float.POSITIVE_INFINITY))
    }

    @Test
    fun testXsFloat_FromXsString_NaN() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("NaN", XsString)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(Float.NaN))
    }

    @Test
    fun testXsFloat_FromXsString_InvalidPattern() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("()", XsString)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:float'."))

        result = XsFloat.cast("true", XsString)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:float'."))
    }

    @Test
    fun testXsFloat_FromXsUntypedAtomic() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("12", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(12.0f))

        result = XsFloat.cast("-1.23", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(-1.23f))

        result = XsFloat.cast("2.3e8", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(2.3e8f))
    }

    @Test
    fun testXsFloat_FromXsUntypedAtomic_Zero() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("-0", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(-0.0f))

        result = XsFloat.cast("0", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(0.0f))

        result = XsFloat.cast("+0", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(0.0f))
    }

    @Test
    fun testXsFloat_FromXsUntypedAtomic_Infinity() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("-INF", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(Float.NEGATIVE_INFINITY))

        result = XsFloat.cast("INF", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(Float.POSITIVE_INFINITY))

        // Valid in XSD 1.1, and MarkLogic; Invalid in BaseX.
        result = XsFloat.cast("+INF", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(Float.POSITIVE_INFINITY))
    }

    @Test
    fun testXsFloat_FromXsUntypedAtomic_NaN() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("NaN", XsUntypedAtomic)
        assertThat(result.type, `is`(XsFloat))
        assertThat(result.value, `is`(instanceOf(Float::class.java)))
        assertThat(result.value as Float, `is`(Float.NaN))
    }

    @Test
    fun testXsFloat_FromXsUntypedAtomic_InvalidPattern() {
        var result: XdmTypeCastResult

        result = XsFloat.cast("()", XsUntypedAtomic)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:float'."))

        result = XsFloat.cast("true", XsUntypedAtomic)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:float'."))
    }

    @Test
    fun testXsFloat_FromIncompatiblePrimitiveType() {
        var result: XdmTypeCastResult

        result = XsFloat.cast(Date(), XsDate)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(XPTY0004))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("Incompatible types when casting 'xs:date' to 'xs:float'."))
    }

    // endregion
    // region Primitive Types :: xs:boolean
    //
    // Reference: https://www.w3.org/TR/xpath-functions/#casting-boolean
    // Reference: https://www.w3.org/TR/xmlschema11-2/#boolean

    @Test
    fun testXsBoolean_FromXsBoolean() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast(false, XsBoolean)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast(true, XsBoolean)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))
    }

    @Test
    fun testXsBoolean_FromXsFloat() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast(0.0f, XsFloat)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast(1.0f, XsFloat)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))

        result = XsBoolean.cast(2.0f, XsFloat)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))

        result = XsBoolean.cast("NaN".toFloat(), XsFloat)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))
    }

    @Test
    fun testXsBoolean_FromXsDouble() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast(0.0, XsDouble)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast(1.0, XsDouble)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))

        result = XsBoolean.cast(2.0, XsDouble)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))

        result = XsBoolean.cast("NaN".toDouble(), XsDouble)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))
    }

    @Test
    fun testXsBoolean_FromXsDecimal() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast(BigDecimal.ZERO, XsDecimal)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast(BigDecimal.ONE, XsDecimal)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))

        result = XsBoolean.cast(BigDecimal.valueOf(2), XsDecimal)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))
    }

    @Test
    fun testXsBoolean_FromXsInteger() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast(BigInteger.ZERO, XsInteger)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast(BigInteger.ONE, XsInteger)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))

        result = XsBoolean.cast(BigInteger.valueOf(2), XsInteger)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))
    }

    @Test
    fun testXsBoolean_FromXsString() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast("0", XsString)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast("1", XsString)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))

        result = XsBoolean.cast("false", XsString)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast("true", XsString)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))
    }

    @Test
    fun testXsBoolean_FromXsString_InvalidPattern() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast("()", XsString)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))

        result = XsBoolean.cast("2", XsString)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))

        result = XsBoolean.cast("True", XsString)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))
    }

    @Test
    fun testXsBoolean_FromXsUntypedAtomic() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast("0", XsUntypedAtomic)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast("1", XsUntypedAtomic)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))

        result = XsBoolean.cast("false", XsUntypedAtomic)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))

        result = XsBoolean.cast("true", XsUntypedAtomic)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))
    }

    @Test
    fun testXsBoolean_FromXsUntypedAtomic_InvalidPattern() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast("()", XsUntypedAtomic)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))

        result = XsBoolean.cast("2", XsUntypedAtomic)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))

        result = XsBoolean.cast("True", XsUntypedAtomic)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))
    }

    @Test
    fun testXsBoolean_FromIncompatiblePrimitiveType() {
        var result: XdmTypeCastResult

        result = XsBoolean.cast(Date(), XsDate)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(XPTY0004))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("Incompatible types when casting 'xs:date' to 'xs:boolean'."))
    }

    // endregion
    // region itemType? [XdmOptional]

    @Test
    fun testXdmOptional_FromEmptySequence() {
        var result: XdmTypeCastResult

        result = XdmOptional(XsBoolean).cast(null, XdmEmptySequence)
        assertThat(result.type, `is`(XdmEmptySequence))
        assertThat(result.value, `is`(nullValue()))
    }

    @Test
    fun testXdmOptional_FromUntyped() {
        var result: XdmTypeCastResult

        result = XdmOptional(XsBoolean).cast("true", XsUntyped)
        assertThat(result.type, `is`(XsUntyped))
        assertThat(result.value, `is`(instanceOf(String::class.java)))
        assertThat(result.value as String, `is`("true"))
    }

    @Test
    fun testXdmOptional_FromType() {
        var result: XdmTypeCastResult

        result = XdmOptional(XsBoolean).cast(false, XsBoolean)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(false))
    }

    @Test
    fun testXdmOptional_FromCastableType() {
        var result: XdmTypeCastResult

        result = XdmOptional(XsBoolean).cast(BigInteger.valueOf(2), XsInteger)
        assertThat(result.type, `is`(XsBoolean))
        assertThat(result.value, `is`(instanceOf(Boolean::class.java)))
        assertThat(result.value as Boolean, `is`(true))
    }

    @Test
    fun testXdmOptional_FromCastableType_InvalidPattern() {
        var result: XdmTypeCastResult

        result = XdmOptional(XsBoolean).cast("()", XsString)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))

        result = XdmOptional(XsBoolean).cast("2", XsString)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))

        result = XdmOptional(XsBoolean).cast("True", XsString)
        assertThat(result.type, `is`(FnError))
        assertThat(result.value, `is`(instanceOf(FnErrorObject::class.java)))
        assertThat((result.value as FnErrorObject).code, `is`(FORG0001))
        assertThat((result.value as FnErrorObject).description?.staticType, `is`(XsString))
        assertThat((result.value as FnErrorObject).description?.staticValue as String,
                `is`("The value does not match the lexical representation for 'xs:boolean'."))
    }

    // endregion
}
