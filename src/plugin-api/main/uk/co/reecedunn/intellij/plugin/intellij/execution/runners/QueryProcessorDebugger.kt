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
package uk.co.reecedunn.intellij.plugin.intellij.execution.runners

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.DefaultProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebuggerManager
import uk.co.reecedunn.intellij.plugin.intellij.execution.configurations.QueryProcessorRunConfiguration
import uk.co.reecedunn.intellij.plugin.processor.debug.DebuggableQueryProvider

class QueryProcessorDebugger : DefaultProgramRunner() {
    override fun getRunnerId(): String = "XIJPQueryProcessorDebugger"

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId != DefaultDebugExecutor.EXECUTOR_ID || profile !is QueryProcessorRunConfiguration) {
            return false
        }
        return profile.processor?.api?.canExecute(profile.language, executorId) == true
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        FileDocumentManager.getInstance().saveAllDocuments()
        val starter = createProcessStarter(environment.runProfile as QueryProcessorRunConfiguration)
        val session = XDebuggerManager.getInstance(environment.project).startSession(environment, starter)
        return session.runContentDescriptor
    }

    private fun createProcessStarter(configuration: QueryProcessorRunConfiguration): XDebugProcessStarter {
        val session = configuration.processor!!.session
        val source = configuration.scriptFile
            ?: throw ExecutionException("Unsupported query file: " + (configuration.scriptFilePath ?: ""))

        val query = (session as DebuggableQueryProvider).createDebuggableQuery(source, configuration.language)
        query.rdfOutputFormat = configuration.rdfOutputFormat
        query.updating = configuration.updating
        query.xpathSubset = configuration.xpathSubset
        query.database = configuration.database ?: ""
        query.server = configuration.server ?: ""
        query.modulePath = configuration.modulePath ?: ""
        configuration.contextItem?.let { query.bindContextItem(it, null) }
        return query
    }
}
