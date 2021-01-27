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
package uk.co.reecedunn.intellij.plugin.marklogic.xray.runner

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.psi.PsiFile
import uk.co.reecedunn.intellij.plugin.processor.intellij.execution.process.QueryResultListener
import uk.co.reecedunn.intellij.plugin.processor.intellij.execution.process.QueryResultTime
import uk.co.reecedunn.intellij.plugin.processor.query.QueryResult
import uk.co.reecedunn.intellij.plugin.xdm.types.XsDurationValue

class XRayTestProcessListener(private val console: ConsoleView) : QueryResultListener {
    override fun onBeginResults() {
    }

    override fun onEndResults(): PsiFile? {
        return null
    }

    override fun onQueryResult(result: QueryResult) {
        console.print(result.value as String, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    override fun onException(e: Throwable) {
        console.print(e.message ?: "", ConsoleViewContentType.ERROR_OUTPUT)
    }

    override fun onQueryResultTime(resultTime: QueryResultTime, time: XsDurationValue) {
    }

    override fun onQueryResultsPsiFile(psiFile: PsiFile) {
    }
}
