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
package uk.co.reecedunn.intellij.plugin.xpm.optree.annotation

import uk.co.reecedunn.intellij.plugin.xdm.types.XdmAnnotation

interface XpmAnnotated {
    val annotations: Sequence<XdmAnnotation>

    val accessLevel: XpmAccessLevel

    @Suppress("unused")
    companion object {
        const val NAMESPACE = "http://www.w3.org/2012/xquery"

        const val PUBLIC = "public"
        const val PRIVATE = "private"
    }
}
