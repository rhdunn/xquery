/*
 * Copyright (C) 2020 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.w3.lang

import com.intellij.navigation.ItemPresentation
import uk.co.reecedunn.intellij.plugin.xpm.lang.XpmProductType
import uk.co.reecedunn.intellij.plugin.xpm.lang.XpmProductVersion
import uk.co.reecedunn.intellij.plugin.xpm.resources.XpmIcons
import javax.swing.Icon

object W3CSpecifications : ItemPresentation, XpmProductType {
    // region ItemPresentation

    override fun getPresentableText(): String = "W3C Specifications"

    override fun getLocationString(): String? = null

    override fun getIcon(unused: Boolean): Icon = XpmIcons.W3.Product

    // endregion
    // region XpmProductType

    override val id: String = "w3c"

    override val presentation: ItemPresentation
        get() = this

    // endregion
    // region Language Versions

    val REC: XpmProductVersion = W3CSpecificationsVersion(this)

    @Suppress("unused")
    val languageVersions: List<XpmProductVersion> = listOf(REC)

    // endregion
}
