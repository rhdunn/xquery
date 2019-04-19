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
package uk.co.reecedunn.intellij.plugin.saxon.query.s9api.binding

import uk.co.reecedunn.intellij.plugin.saxon.query.s9api.SAXON_NAMESPACES
import uk.co.reecedunn.intellij.plugin.xpath.functions.op_qname_parse

open class XdmValue(val saxonObject: Any, private val `class`: Class<*>) {
    fun iterator(): Any {
        return `class`.getMethod("iterator").invoke(saxonObject)
    }

    fun getUnderlyingValue(): Any {
        return `class`.getMethod("getUnderlyingValue").invoke(saxonObject)
    }

    override fun toString(): String {
        return saxonObject.toString()
    }

    override fun hashCode(): Int {
        return saxonObject.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is XdmItem) return false
        return saxonObject == other.saxonObject
    }

    companion object {
        fun newInstance(value: Any?, type: String, loader: ClassLoader): XdmValue {
            return when (type) {
                "empty-sequence()" -> XdmEmptySequence.getInstance(loader)
                "xs:QName" -> XdmAtomicValue(op_qname_parse(value as String, SAXON_NAMESPACES).toQName(loader))
                "xs:numeric" -> XdmNumeric.newInstance(value as String, loader)
                else -> XdmAtomicValue(value as String, type, loader)
            }
        }
    }
}
