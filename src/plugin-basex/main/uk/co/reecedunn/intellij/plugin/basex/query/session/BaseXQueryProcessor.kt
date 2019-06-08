/*
 * Copyright (C) 2018-2019 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.basex.query.session

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import uk.co.reecedunn.intellij.plugin.basex.query.session.binding.Session
import uk.co.reecedunn.intellij.plugin.basex.resources.BaseXQueries
import uk.co.reecedunn.intellij.plugin.core.async.ExecutableOnPooledThread
import uk.co.reecedunn.intellij.plugin.core.async.cached
import uk.co.reecedunn.intellij.plugin.core.async.getValue
import uk.co.reecedunn.intellij.plugin.core.async.local_thread
import uk.co.reecedunn.intellij.plugin.core.vfs.decode
import uk.co.reecedunn.intellij.plugin.intellij.lang.XQuery
import uk.co.reecedunn.intellij.plugin.processor.log.LogViewProvider
import uk.co.reecedunn.intellij.plugin.processor.profile.ProfileableQuery
import uk.co.reecedunn.intellij.plugin.processor.profile.ProfileableQueryProvider
import uk.co.reecedunn.intellij.plugin.processor.query.*

internal class BaseXQueryProcessor(val session: Session, val classLoader: ClassLoader) :
    ProfileableQueryProvider,
    RunnableQueryProvider,
    LogViewProvider {
    override val version: ExecutableOnPooledThread<String> by cached {
        createRunnableQuery(BaseXQueries.Version, XQuery).use { query ->
            query.run().then { results -> results.results.first().value as String }
        }
    }

    override val servers: ExecutableOnPooledThread<List<String>> = local_thread {
        listOf<String>()
    }

    override val databases: ExecutableOnPooledThread<List<String>> = local_thread {
        listOf<String>()
    }

    override fun createProfileableQuery(query: VirtualFile, language: Language): ProfileableQuery {
        return when (language) {
            XQuery -> BaseXProfileableQuery(session, query.decode()!!, query, classLoader)
            else -> throw UnsupportedQueryType(language)
        }
    }

    override fun createRunnableQuery(query: VirtualFile, language: Language): RunnableQuery {
        return when (language) {
            XQuery -> BaseXRunnableQuery(session, query.decode()!!, query, classLoader)
            else -> throw UnsupportedQueryType(language)
        }
    }

    override fun logs(): ExecutableOnPooledThread<List<String>> {
        return createRunnableQuery(BaseXQueries.Log.Logs, XQuery).use { query ->
            query.run().then { results -> results.results.map { it.value as String } }
        }
    }

    override fun log(name: String): ExecutableOnPooledThread<List<String>> {
        return createRunnableQuery(BaseXQueries.Log.Log, XQuery).use { query ->
            query.bindVariable("name", name, "xs:string")
            query.run().then { results -> results.results.map { it.value as String } }
        }
    }

    override fun close() {
        session.close()
    }
}
