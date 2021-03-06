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
package uk.co.reecedunn.intellij.plugin.saxon.query.s9api.runner

import uk.co.reecedunn.intellij.plugin.processor.query.ProcessTerminatedException
import uk.co.reecedunn.intellij.plugin.processor.query.QueryProcessState
import uk.co.reecedunn.intellij.plugin.saxon.query.s9api.binding.expr.XPathContext
import uk.co.reecedunn.intellij.plugin.saxon.query.s9api.binding.trace.InstructionInfo
import uk.co.reecedunn.intellij.plugin.saxon.query.s9api.proxy.TraceListener

open class SaxonTraceListener : TraceListener {
    // region Process State

    protected var state: QueryProcessState = QueryProcessState.Starting

    open fun onstart() {}

    open fun onfinish() {}

    fun stop() {
        if (state !== QueryProcessState.Stopped) {
            state = QueryProcessState.Stopping
        }
    }

    private fun checkIsStopping() {
        if (state === QueryProcessState.Stopping) {
            state = QueryProcessState.Stopped
            throw ProcessTerminatedException()
        }
    }

    // endregion
    // region TraceListener

    override fun setOutputDestination(logger: Any) {
    }

    override fun open(controller: Any?) {
        // Saxon <= 9.6 call open twice.
        if (state !== QueryProcessState.Starting) return

        state = QueryProcessState.Running
        onstart()
    }

    override fun close() {
        // Saxon <= 9.6 call close twice, or the query was stopped by the user.
        if (state === QueryProcessState.Stopped) return

        state = QueryProcessState.Stopped
        onfinish()
    }

    override fun enter(instruction: InstructionInfo, properties: Map<String, Any>, context: XPathContext) {
        checkIsStopping()
    }

    override fun leave(instruction: InstructionInfo) {
        checkIsStopping()
    }

    override fun startCurrentItem(currentItem: Any) {
        checkIsStopping()
    }

    override fun endCurrentItem(currentItem: Any) {
        checkIsStopping()
    }

    override fun startRuleSearch() {
        checkIsStopping()
    }

    override fun endRuleSearch(rule: Any, mode: Any, item: Any) {
        checkIsStopping()
    }

    // endregion
}
