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
package uk.co.reecedunn.intellij.plugin.xijp.lang

import uk.co.reecedunn.intellij.plugin.xpm.lang.XpmProductType
import uk.co.reecedunn.intellij.plugin.xpm.lang.XpmProductVersion

data class XQueryIntelliJPluginVersion(
    override val product: XpmProductType,
    val major: Int,
    val minor: Int,
    override val features: String = ""
) : XpmProductVersion {
    // region XpmProductVersion

    override val id: String = "$major.$minor"

    override fun compareTo(other: XpmProductVersion): Int {
        return when (val productDiff = this.product.id.compareTo(other.product.id)) {
            0 -> when (val majorDiff = this.major - (other as XQueryIntelliJPluginVersion).major) {
                0 -> this.minor - other.minor
                else -> majorDiff
            }
            else -> productDiff
        }
    }

    override fun toString(): String = "${product.presentation.presentableText} $id"

    // endregion
}
