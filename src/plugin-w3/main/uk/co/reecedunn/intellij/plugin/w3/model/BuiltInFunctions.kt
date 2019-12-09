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
package uk.co.reecedunn.intellij.plugin.w3.model

import uk.co.reecedunn.intellij.plugin.xdm.module.JarModuleResolver

object BuiltInFunctions : JarModuleResolver() {
    override val classLoader: ClassLoader = this::class.java.classLoader

    override val modules = mapOf(
        "http://www.w3.org/2001/XMLSchema" to "org/w3/www/2001/XMLSchema.xqy",
        "http://www.w3.org/2005/xpath-functions" to "org/w3/www/2005/xpath-functions.xqy",
        "http://www.w3.org/2005/xpath-functions/array" to "org/w3/www/2005/xpath-functions/array.xqy",
        "http://www.w3.org/2005/xpath-functions/map" to "org/w3/www/2005/xpath-functions/map.xqy",
        "http://www.w3.org/2005/xpath-functions/math" to "org/w3/www/2005/xpath-functions/math.xqy"
    )
}
