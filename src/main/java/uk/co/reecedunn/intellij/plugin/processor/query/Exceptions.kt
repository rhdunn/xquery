/*
 * Copyright (C) 2018 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.processor.query

abstract class QueryError : RuntimeException() {
    override val message: String? get() = description?.let { "[$standardCode] $it" } ?: standardCode

    abstract val standardCode: String

    abstract val vendorCode: String?

    abstract val description: String?

    abstract val module: String?

    abstract val lineNumber: Int?

    abstract val columnNumber: Int?
}

class MissingJarFileException(val jarType: String) : RuntimeException("Missing JAR file for $jarType.")

class UnsupportedQueryType(val mimetype: String) :
    RuntimeException("Unsupported query type: ${mimetype}")
