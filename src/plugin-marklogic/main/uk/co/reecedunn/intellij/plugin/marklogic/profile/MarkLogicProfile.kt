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
package uk.co.reecedunn.intellij.plugin.marklogic.profile

import uk.co.reecedunn.intellij.plugin.core.xml.XmlDocument
import uk.co.reecedunn.intellij.plugin.core.xml.XmlElement
import uk.co.reecedunn.intellij.plugin.processor.debug.StackFrame
import uk.co.reecedunn.intellij.plugin.processor.profile.ProfileEntry
import uk.co.reecedunn.intellij.plugin.processor.profile.ProfileReport
import uk.co.reecedunn.intellij.plugin.xpath.model.XsDurationValue
import uk.co.reecedunn.intellij.plugin.xpath.model.toXsDuration

private val PROFILE_NAMESPACES = mapOf("prof" to "http://marklogic.com/xdmp/profile")

private fun XmlElement.toProfileEntry(script: String): ProfileEntry {
    val path = children("prof:uri").first().text()
    return ProfileEntry(
        id = children("prof:expr-id").first().text()!!,
        expression = children("prof:expr-source").first().text()!!,
        frame = StackFrame(
            if (path.isNullOrEmpty()) script else path,
            children("prof:line").first().text()?.toInt(),
            children("prof:column").first().text()?.toInt()
        ),
        hits = children("prof:count").first().text()!!.toInt(),
        shallowTime = children("prof:shallow-time").first().text()?.toXsDuration()!!,
        deepTime = children("prof:deep-time").first().text()?.toXsDuration()!!
    )
}

fun String.toMarkLogicProfileReport(script: String): ProfileReport {
    val doc = XmlDocument.parse(this, PROFILE_NAMESPACES)
    val metadata = doc.root.children("prof:metadata").first()
    val histogram = doc.root.children("prof:histogram").first()
    return ProfileReport(
        xml = this,
        elapsed = metadata.children("prof:overall-elapsed").first().text()?.toXsDuration()!!,
        created = metadata.children("prof:created").first().text()!!,
        version = metadata.children("prof:server-version").first().text()!!,
        results = histogram.children("prof:expression").map { expression -> expression.toProfileEntry(script) }
    )
}
