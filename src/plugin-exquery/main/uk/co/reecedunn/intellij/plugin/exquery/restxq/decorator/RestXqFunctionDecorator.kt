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
package uk.co.reecedunn.intellij.plugin.exquery.restxq.decorator

import uk.co.reecedunn.intellij.plugin.exquery.restxq.endpoints.RestXqAnnotations
import uk.co.reecedunn.intellij.plugin.xpm.optree.function.XpmFunctionDeclaration
import uk.co.reecedunn.intellij.plugin.xpm.optree.function.XpmFunctionDecorator
import uk.co.reecedunn.intellij.plugin.xquery.resources.XQueryIcons
import javax.swing.Icon

object RestXqFunctionDecorator : XpmFunctionDecorator {
    override fun getIcon(function: XpmFunctionDeclaration): Icon? = RestXqAnnotations.create(function)?.let {
        XQueryIcons.Endpoints.FunctionEndpoint
    }
}
