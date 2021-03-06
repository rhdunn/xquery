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
package uk.co.reecedunn.intellij.plugin.marklogic.log.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import uk.co.reecedunn.intellij.plugin.marklogic.log.lexer.MarkLogicErrorLogTokenType

class MarkLogicErrorLogParser : PsiParser {
    // region PsiParser

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        parse(builder)
        rootMarker.done(root)
        return builder.treeBuilt
    }

    // endregion
    // region Grammar

    fun parse(builder: PsiBuilder) {
        while (builder.tokenType != null) {
            parseLogLine(builder)
        }
    }

    private fun parseLogLine(builder: PsiBuilder) {
        val marker = builder.mark()
        while (true) when (builder.tokenType) {
            null -> {
                marker.done(MarkLogicErrorLogElementType.LOG_LINE)
                return
            }
            MarkLogicErrorLogTokenType.MESSAGE -> {
                builder.advanceLexer()
                marker.done(MarkLogicErrorLogElementType.LOG_LINE)
                return
            }
            else -> builder.advanceLexer()
        }
    }

    // endregion
}
