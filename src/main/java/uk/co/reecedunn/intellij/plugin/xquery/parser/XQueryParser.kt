/*
 * Copyright (C) 2016-2018 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xquery.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import uk.co.reecedunn.intellij.plugin.core.lang.errorOnTokenType
import uk.co.reecedunn.intellij.plugin.core.lang.matchTokenType
import uk.co.reecedunn.intellij.plugin.core.lang.matchTokenTypeWithMarker
import uk.co.reecedunn.intellij.plugin.intellij.resources.XPathBundle
import uk.co.reecedunn.intellij.plugin.xpath.lexer.INCNameType
import uk.co.reecedunn.intellij.plugin.xpath.lexer.IKeywordOrNCNameType
import uk.co.reecedunn.intellij.plugin.xquery.lexer.XQueryTokenType
import uk.co.reecedunn.intellij.plugin.intellij.resources.XQueryBundle
import uk.co.reecedunn.intellij.plugin.xpath.lexer.XPathTokenType
import uk.co.reecedunn.intellij.plugin.xpath.parser.XPathElementType
import uk.co.reecedunn.intellij.plugin.xpath.parser.XPathParser

class XQueryParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        XQueryParserImpl(builder).parse(builder)
        rootMarker.done(root)
        return builder.treeBuilt
    }
}

private enum class KindTest {
    ANY_TEST,
    TYPED_TEST,
}

private val COMPATIBILITY_ANNOTATION_TOKENS = TokenSet.create(
    XQueryTokenType.K_ASSIGNABLE,
    XQueryTokenType.K_PRIVATE,
    XQueryTokenType.K_SEQUENTIAL,
    XQueryTokenType.K_SIMPLE,
    XQueryTokenType.K_UNASSIGNABLE,
    XQueryTokenType.K_UPDATING
)

/**
 * A unified XQuery parser for different XQuery dialects.
 *
 * Supported core language specifications:
 * -  XQuery 1.0 Second Edition (W3C Recommendation 14 December 2010)
 * -  XQuery 3.0 (W3C Recommendation 08 April 2014)
 * -  XQuery 3.1 (W3C Recommendation 21 March 2017)
 *
 * Supported W3C XQuery extensions:
 * -  Full Text 1.0 (W3C Recommendation 17 March 2011)
 * -  Full Text 3.0 (W3C Recommendation 24 November 2015)
 * -  Update Facility 1.0 (W3C Recommendation 17 March 2011)
 * -  Update Facility 3.0 (W3C Working Group Note 24 January 2017)
 * -  ScriptingSpec Extension 1.0 (W3C Working Group Note 18 September 2014)
 *
 * Supported vendor extensions:
 * -  BaseX
 * -  MarkLogic 1.0-ml
 * -  Saxon
 *
 * See the *EBNF for XQuery 3.1* section of `docs/XQuery IntelliJ Plugin.md`
 * for details of the grammar implemented by this parser.
 */
@Suppress("FunctionName")
private class XQueryParserImpl(private val builder: PsiBuilder) : XPathParser() {
    // region XPath/XQuery Element Types
    //
    // These element types have different PSI implementations in XPath and XQuery.

    override val ENCLOSED_EXPR: IElementType = XQueryElementType.ENCLOSED_EXPR
    override val EXPR: IElementType = XQueryElementType.EXPR
    override val FUNCTION_BODY: IElementType = XQueryElementType.FUNCTION_BODY
    override val FUNCTION_TEST: IElementType = XQueryElementType.FUNCTION_TEST
    override val STRING_LITERAL: IElementType = XQueryElementType.STRING_LITERAL

    // endregion
    // region Parser Helper Functions

    protected fun matchTokenTypeWithMarker(type: IElementType): PsiBuilder.Marker? {
        return builder.matchTokenTypeWithMarker(type)
    }

    protected fun matchTokenTypeWithMarker(type1: IElementType, type2: IElementType): PsiBuilder.Marker? {
        return builder.matchTokenTypeWithMarker(type1, type2)
    }

    protected fun errorOnTokenType(type: IElementType, message: String): Boolean {
        return builder.errorOnTokenType(type, message)
    }

    // endregion
    // region PsiBuilder API

    protected fun mark(): PsiBuilder.Marker = builder.mark()

    protected fun getTokenType(): IElementType? = builder.tokenType

    protected fun advanceLexer() = builder.advanceLexer()

    protected fun error(message: String) = builder.error(message)

    // endregion
    // region Grammar

    override fun parse(builder: PsiBuilder, isFirst: Boolean): Boolean {
        return parseTransactions(isFirst)
    }

    private enum class ParseStatus {
        MATCHED,
        MATCHED_WITH_ERRORS,
        NOT_MATCHED
    }

    // endregion
    // region Grammar :: Modules

    private enum class TransactionType {
        WITH_PROLOG,
        WITHOUT_PROLOG,
        NONE
    }

    private fun parseTransactions(isFirst: Boolean): Boolean {
        if (parseModule(isFirst)) {
            parseWhiteSpaceAndCommentTokens(builder)
            while (parseTransactionSeparator() != TransactionType.NONE) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseModule(false)) { // NOTE: Handles error cases for VersionDecl-only and library modules.
                    error(XPathBundle.message("parser.error.expected", "MainModule"))
                }
                parseWhiteSpaceAndCommentTokens(builder)
            }
            return true
        }
        return false
    }

    private fun parseTransactionSeparator(): TransactionType {
        val transactionSeparatorMarker = matchTokenTypeWithMarker(XQueryTokenType.SEPARATOR)
        if (transactionSeparatorMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            var haveProlog = false
            if (getTokenType() === XQueryTokenType.K_XQUERY ||
                    getTokenType() === XQueryTokenType.K_DECLARE ||
                    getTokenType() === XQueryTokenType.K_IMPORT ||
                    getTokenType() === XQueryTokenType.K_MODULE) {

                val marker = mark()
                advanceLexer()
                parseWhiteSpaceAndCommentTokens(builder)
                haveProlog = getTokenType() is IKeywordOrNCNameType
                marker.rollbackTo()
            }

            transactionSeparatorMarker.done(XQueryElementType.TRANSACTION_SEPARATOR)
            return if (haveProlog) TransactionType.WITH_PROLOG else TransactionType.WITHOUT_PROLOG
        }
        return TransactionType.NONE
    }

    private fun parseModule(isFirst: Boolean): Boolean {
        var hasVersionDeclOrWhitespace: Boolean = parseVersionDecl()
        hasVersionDeclOrWhitespace = hasVersionDeclOrWhitespace or parseWhiteSpaceAndCommentTokens(builder)

        val moduleMarker = mark()
        if (parseLibraryModule()) {
            moduleMarker.done(XQueryElementType.LIBRARY_MODULE)
            return true
        } else if (parseMainModule()) {
            moduleMarker.done(XQueryElementType.MAIN_MODULE)
            return true
        }

        if (isFirst) {
            error(XQueryBundle.message("parser.error.expected-module-type"))
        }
        moduleMarker.drop()
        return hasVersionDeclOrWhitespace
    }

    private fun parseVersionDecl(): Boolean {
        val versionDeclMarker = matchTokenTypeWithMarker(XQueryTokenType.K_XQUERY)
        if (versionDeclMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XQueryTokenType.K_ENCODING)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseStringLiteral(builder)) {
                    error(XQueryBundle.message("parser.error.expected-encoding-string"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
            } else {
                if (!builder.matchTokenType(XQueryTokenType.K_VERSION)) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "version"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseStringLiteral(builder) && !haveErrors) {
                    error(XQueryBundle.message("parser.error.expected-version-string"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (builder.matchTokenType(XQueryTokenType.K_ENCODING)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseStringLiteral(builder) && !haveErrors) {
                        error(XQueryBundle.message("parser.error.expected-encoding-string"))
                        haveErrors = true
                    }

                    parseWhiteSpaceAndCommentTokens(builder)
                }
            }

            if (!builder.matchTokenType(XQueryTokenType.SEPARATOR)) {
                versionDeclMarker.done(XQueryElementType.VERSION_DECL)
                if (!haveErrors) {
                    error(XPathBundle.message("parser.error.expected", ";"))
                }
                if (getTokenType() === XPathTokenType.QNAME_SEPARATOR) {
                    advanceLexer()
                }
                return true
            }

            versionDeclMarker.done(XQueryElementType.VERSION_DECL)
            return true
        }
        return false
    }

    private fun parseMainModule(): Boolean {
        if (parseProlog(false)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExpr(XQueryElementType.QUERY_BODY)) {
                error(XQueryBundle.message("parser.error.expected-query-body"))
            }
            return true
        }
        return parseExpr(XQueryElementType.QUERY_BODY)
    }

    private fun parseLibraryModule(): Boolean {
        if (parseModuleDecl()) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseProlog(true)
            return true
        }
        return false
    }

    private fun parseModuleDecl(): Boolean {
        val moduleDeclMarker = matchTokenTypeWithMarker(XQueryTokenType.K_MODULE)
        if (moduleDeclMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_NAMESPACE)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "namespace"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseQNameOrWildcard(builder, XQueryElementType.NCNAME, false) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-ncname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.EQUAL) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "="))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.SEPARATOR)) {
                moduleDeclMarker.done(XQueryElementType.MODULE_DECL)
                if (!haveErrors) {
                    error(XPathBundle.message("parser.error.expected", ";"))
                }
                if (getTokenType() === XPathTokenType.QNAME_SEPARATOR) {
                    advanceLexer()
                }
                return true
            }

            moduleDeclMarker.done(XQueryElementType.MODULE_DECL)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Prolog

    private enum class PrologDeclState {
        HEADER_STATEMENT,
        BODY_STATEMENT,
        UNKNOWN_STATEMENT,
        NOT_MATCHED
    }

    private fun parseProlog(parseInvalidConstructs: Boolean): Boolean {
        val prologMarker = mark()

        var state = PrologDeclState.NOT_MATCHED
        while (true) {
            var nextState = parseDecl(if (state == PrologDeclState.NOT_MATCHED) PrologDeclState.HEADER_STATEMENT else state)
            if (nextState == PrologDeclState.NOT_MATCHED) {
                nextState = parseImport(if (state == PrologDeclState.NOT_MATCHED) PrologDeclState.HEADER_STATEMENT else state)
            }

            when (nextState) {
                XQueryParserImpl.PrologDeclState.NOT_MATCHED -> if (parseInvalidConstructs && getTokenType() != null) {
                    if (!parseWhiteSpaceAndCommentTokens(builder)) {
                        error(XPathBundle.message("parser.error.unexpected-token"))
                        if (!parseExprSingle(builder)) advanceLexer()
                    }
                } else {
                    if (state == PrologDeclState.NOT_MATCHED) {
                        prologMarker.drop()
                        return false
                    }
                    prologMarker.done(XQueryElementType.PROLOG)
                    return true
                }
                XQueryParserImpl.PrologDeclState.HEADER_STATEMENT, XQueryParserImpl.PrologDeclState.UNKNOWN_STATEMENT -> if (state == PrologDeclState.NOT_MATCHED) {
                    state = PrologDeclState.HEADER_STATEMENT
                }
                XQueryParserImpl.PrologDeclState.BODY_STATEMENT -> if (state != PrologDeclState.BODY_STATEMENT) {
                    state = PrologDeclState.BODY_STATEMENT
                }
            }

            if (nextState != PrologDeclState.NOT_MATCHED) {
                if (!builder.matchTokenType(XQueryTokenType.SEPARATOR)) {
                    error(XPathBundle.message("parser.error.expected", ";"))
                    if (getTokenType() === XPathTokenType.QNAME_SEPARATOR) {
                        advanceLexer()
                    }
                }
                parseWhiteSpaceAndCommentTokens(builder)
            }
        }
    }

    private fun parseDecl(state: PrologDeclState): PrologDeclState {
        val declMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DECLARE)
        if (declMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseBaseURIDecl(state)) {
                declMarker.done(XQueryElementType.BASE_URI_DECL)
            } else if (parseBoundarySpaceDecl(state)) {
                declMarker.done(XQueryElementType.BOUNDARY_SPACE_DECL)
            } else if (parseConstructionDecl(state)) {
                declMarker.done(XQueryElementType.CONSTRUCTION_DECL)
            } else if (parseCopyNamespacesDecl(state)) {
                declMarker.done(XQueryElementType.COPY_NAMESPACES_DECL)
            } else if (parseDecimalFormatDecl(state, false)) {
                declMarker.done(XQueryElementType.DECIMAL_FORMAT_DECL)
            } else if (parseDefaultDecl(declMarker, state)) {
            } else if (parseNamespaceDecl(state)) {
                declMarker.done(XQueryElementType.NAMESPACE_DECL)
            } else if (parseOptionDecl()) {
                declMarker.done(XQueryElementType.OPTION_DECL)
                return PrologDeclState.BODY_STATEMENT
            } else if (parseOrderingModeDecl(state)) {
                declMarker.done(XQueryElementType.ORDERING_MODE_DECL)
            } else if (parseRevalidationDecl(state)) {
                declMarker.done(XQueryElementType.REVALIDATION_DECL)
            } else if (parseAnnotatedDecl()) {
                declMarker.done(XQueryElementType.ANNOTATED_DECL)
                return PrologDeclState.BODY_STATEMENT
            } else if (parseContextItemDecl()) {
                declMarker.done(XQueryElementType.CONTEXT_ITEM_DECL)
                return PrologDeclState.BODY_STATEMENT
            } else if (parseTypeDecl()) {
                declMarker.done(XQueryElementType.TYPE_DECL)
            } else if (parseFTOptionDecl()) {
                declMarker.done(XQueryElementType.FT_OPTION_DECL)
            } else {
                error(XQueryBundle.message("parser.error.expected-keyword", "base-uri, boundary-space, construction, context, copy-namespaces, decimal-format, default, ft-option, function, namespace, option, ordering, revalidation, type, variable"))
                parseUnknownDecl()
                declMarker.done(XQueryElementType.UNKNOWN_DECL)
                return PrologDeclState.UNKNOWN_STATEMENT
            }
            return PrologDeclState.HEADER_STATEMENT
        }
        return PrologDeclState.NOT_MATCHED
    }

    private fun parseDefaultDecl(defaultDeclMarker: PsiBuilder.Marker, state: PrologDeclState): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DEFAULT)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseDefaultNamespaceDecl()) {
                defaultDeclMarker.done(XQueryElementType.DEFAULT_NAMESPACE_DECL)
            } else if (parseEmptyOrderDecl()) {
                defaultDeclMarker.done(XQueryElementType.EMPTY_ORDER_DECL)
            } else if (parseDefaultCollationDecl()) {
                defaultDeclMarker.done(XQueryElementType.DEFAULT_COLLATION_DECL)
            } else if (parseDecimalFormatDecl(state, true)) {
                defaultDeclMarker.done(XQueryElementType.DECIMAL_FORMAT_DECL)
            } else {
                error(XQueryBundle.message("parser.error.expected-keyword", "collation, element, function, order"))
                parseUnknownDecl()
                defaultDeclMarker.done(XQueryElementType.UNKNOWN_DECL)
            }
            return true
        }
        return false
    }

    private fun parseUnknownDecl(): Boolean {
        while (true) {
            if (parseWhiteSpaceAndCommentTokens(builder)) continue
            if (builder.matchTokenType(XPathTokenType.NCNAME)) continue
            if (parseStringLiteral(builder)) continue

            if (builder.matchTokenType(XPathTokenType.EQUAL)) continue
            if (builder.matchTokenType(XPathTokenType.COMMA)) continue
            if (builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) continue
            if (builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL)) continue
            if (builder.matchTokenType(XPathTokenType.QNAME_SEPARATOR)) continue
            if (builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) continue
            if (builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) continue

            if (builder.matchTokenType(XQueryTokenType.K_COLLATION)) continue
            if (builder.matchTokenType(XPathTokenType.K_ELEMENT)) continue
            if (builder.matchTokenType(XPathTokenType.K_EMPTY)) continue
            if (builder.matchTokenType(XQueryTokenType.K_EXTERNAL)) continue
            if (builder.matchTokenType(XPathTokenType.K_FUNCTION)) continue
            if (builder.matchTokenType(XQueryTokenType.K_GREATEST)) continue
            if (builder.matchTokenType(XQueryTokenType.K_INHERIT)) continue
            if (builder.matchTokenType(XPathTokenType.K_ITEM)) continue
            if (builder.matchTokenType(XQueryTokenType.K_LAX)) continue
            if (builder.matchTokenType(XQueryTokenType.K_LEAST)) continue
            if (builder.matchTokenType(XPathTokenType.K_NAMESPACE)) continue
            if (builder.matchTokenType(XQueryTokenType.K_NO_INHERIT)) continue
            if (builder.matchTokenType(XQueryTokenType.K_NO_PRESERVE)) continue
            if (builder.matchTokenType(XQueryTokenType.K_ORDER)) continue
            if (builder.matchTokenType(XQueryTokenType.K_ORDERED)) continue
            if (builder.matchTokenType(XQueryTokenType.K_PRESERVE)) continue
            if (builder.matchTokenType(XQueryTokenType.K_SKIP)) continue
            if (builder.matchTokenType(XQueryTokenType.K_STRICT)) continue
            if (builder.matchTokenType(XQueryTokenType.K_STRIP)) continue
            if (builder.matchTokenType(XQueryTokenType.K_UNORDERED)) continue

            if (parseDFPropertyName()) continue
            if (parseExprSingle(builder)) continue
            return true
        }
    }

    private fun parseDefaultNamespaceDecl(): Boolean {
        if (builder.matchTokenType(XPathTokenType.K_ELEMENT) || builder.matchTokenType(XPathTokenType.K_FUNCTION)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_NAMESPACE)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "namespace"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseNamespaceDecl(state: PrologDeclState): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XPathTokenType.K_NAMESPACE)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseQNameOrWildcard(builder, XQueryElementType.NCNAME, false)) {
                error(XQueryBundle.message("parser.error.expected-ncname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.EQUAL) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "="))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseFTOptionDecl(): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_FT_OPTION)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTMatchOptions()) {
                error(XQueryBundle.message("parser.error.expected-keyword", "using"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Prolog :: Header :: Setter

    private fun parseBoundarySpaceDecl(state: PrologDeclState): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XQueryTokenType.K_BOUNDARY_SPACE)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_PRESERVE) && !builder.matchTokenType(XQueryTokenType.K_STRIP)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "preserve, strip"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseDefaultCollationDecl(): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_COLLATION)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL)) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseBaseURIDecl(state: PrologDeclState): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XQueryTokenType.K_BASE_URI)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL)) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseConstructionDecl(state: PrologDeclState): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XQueryTokenType.K_CONSTRUCTION)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_PRESERVE) && !builder.matchTokenType(XQueryTokenType.K_STRIP)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "preserve, strip"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseOrderingModeDecl(state: PrologDeclState): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XQueryTokenType.K_ORDERING)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_ORDERED) && !builder.matchTokenType(XQueryTokenType.K_UNORDERED)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "ordered, unordered"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseEmptyOrderDecl(): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_ORDER)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_EMPTY)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "empty"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_GREATEST) && !builder.matchTokenType(XQueryTokenType.K_LEAST) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "greatest, least"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseRevalidationDecl(state: PrologDeclState): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XQueryTokenType.K_REVALIDATION)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_STRICT) && !builder.matchTokenType(XQueryTokenType.K_LAX) && !builder.matchTokenType(XQueryTokenType.K_SKIP)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "lax, skip, strict"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseCopyNamespacesDecl(state: PrologDeclState): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XQueryTokenType.K_COPY_NAMESPACES)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_PRESERVE) && !builder.matchTokenType(XQueryTokenType.K_NO_PRESERVE)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "preserve, no-preserve"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.COMMA) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ","))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_INHERIT) && !builder.matchTokenType(XQueryTokenType.K_NO_INHERIT) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "inherit, no-inherit"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseDecimalFormatDecl(state: PrologDeclState, isDefault: Boolean): Boolean {
        val errorMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DECIMAL_FORMAT)
        if (errorMarker != null) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            var haveErrors = false
            if (!isDefault) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                    error(XPathBundle.message("parser.error.expected-eqname"))
                    haveErrors = true
                }
            }

            while (parseDFPropertyName()) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.EQUAL) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "="))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseStringLiteral(builder) && !haveErrors) {
                    error(XQueryBundle.message("parser.error.expected-property-value-string"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseDFPropertyName(): Boolean {
        val dfPropertyNameMarker = mark()
        if (builder.matchTokenType(XQueryTokenType.K_DECIMAL_SEPARATOR) ||
                builder.matchTokenType(XQueryTokenType.K_GROUPING_SEPARATOR) ||
                builder.matchTokenType(XQueryTokenType.K_INFINITY) ||
                builder.matchTokenType(XQueryTokenType.K_MINUS_SIGN) ||
                builder.matchTokenType(XQueryTokenType.K_NAN) ||
                builder.matchTokenType(XQueryTokenType.K_PERCENT) ||
                builder.matchTokenType(XQueryTokenType.K_PER_MILLE) ||
                builder.matchTokenType(XQueryTokenType.K_ZERO_DIGIT) ||
                builder.matchTokenType(XQueryTokenType.K_DIGIT) ||
                builder.matchTokenType(XQueryTokenType.K_PATTERN_SEPARATOR) ||
                builder.matchTokenType(XQueryTokenType.K_EXPONENT_SEPARATOR)) { // XQuery 3.1

            dfPropertyNameMarker.done(XQueryElementType.DF_PROPERTY_NAME)
            return true
        }
        dfPropertyNameMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Prolog :: Header :: Import

    private fun parseImport(state: PrologDeclState): PrologDeclState {
        val importMarker = mark()
        val errorMarker = mark()
        if (builder.matchTokenType(XQueryTokenType.K_IMPORT)) {
            if (state == PrologDeclState.HEADER_STATEMENT) {
                errorMarker.drop()
            } else {
                errorMarker.error(XQueryBundle.message("parser.error.expected-prolog-body"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseSchemaImport()) {
                importMarker.done(XQueryElementType.SCHEMA_IMPORT)
            } else if (parseStylesheetImport()) {
                importMarker.done(XQueryElementType.STYLESHEET_IMPORT)
            } else if (parseModuleImport()) {
                importMarker.done(XQueryElementType.MODULE_IMPORT)
            } else {
                error(XQueryBundle.message("parser.error.expected-keyword", "schema, stylesheet, module"))
                importMarker.done(XQueryElementType.IMPORT)
                return PrologDeclState.UNKNOWN_STATEMENT
            }
            return PrologDeclState.HEADER_STATEMENT
        }

        errorMarker.drop()
        importMarker.drop()
        return PrologDeclState.NOT_MATCHED
    }

    private fun parseSchemaImport(): Boolean {
        if (getTokenType() === XQueryTokenType.K_SCHEMA) {
            advanceLexer()

            parseWhiteSpaceAndCommentTokens(builder)
            var haveErrors = parseSchemaPrefix()

            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL)) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XQueryTokenType.K_AT)) {
                do {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL) && !haveErrors) {
                        error(XQueryBundle.message("parser.error.expected-uri-string"))
                        haveErrors = true
                    }
                    parseWhiteSpaceAndCommentTokens(builder)
                } while (builder.matchTokenType(XPathTokenType.COMMA))
            }
            return true
        }
        return false
    }

    private fun parseSchemaPrefix(): Boolean {
        var haveErrors = false
        val schemaPrefixMarker = matchTokenTypeWithMarker(XPathTokenType.K_NAMESPACE)
        if (schemaPrefixMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseQNameOrWildcard(builder, XQueryElementType.NCNAME, false)) {
                error(XQueryBundle.message("parser.error.expected-ncname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.EQUAL) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "="))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            schemaPrefixMarker.done(XQueryElementType.SCHEMA_PREFIX)
            return haveErrors
        }

        val schemaPrefixDefaultMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DEFAULT)
        if (schemaPrefixDefaultMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_ELEMENT)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "element"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_NAMESPACE) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "namespace"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            schemaPrefixDefaultMarker.done(XQueryElementType.SCHEMA_PREFIX)
        }
        return haveErrors
    }

    private fun parseStylesheetImport(): Boolean {
        if (getTokenType() === XQueryTokenType.K_STYLESHEET) {
            var haveErrors = false
            advanceLexer()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_AT)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "at"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseModuleImport(): Boolean {
        if (getTokenType() === XQueryTokenType.K_MODULE) {
            var haveErrors = false
            advanceLexer()

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_NAMESPACE)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseQNameOrWildcard(builder, XQueryElementType.NCNAME, false)) {
                    error(XQueryBundle.message("parser.error.expected-ncname"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.EQUAL) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "="))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
            }

            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XQueryTokenType.K_AT)) {
                do {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL) && !haveErrors) {
                        error(XQueryBundle.message("parser.error.expected-uri-string"))
                        haveErrors = true
                    }
                    parseWhiteSpaceAndCommentTokens(builder)
                } while (builder.matchTokenType(XPathTokenType.COMMA))
            }
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Prolog :: Body

    private fun parseContextItemDecl(): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_CONTEXT)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_ITEM)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "item"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_AS)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseItemType()) {
                    error(XPathBundle.message("parser.error.expected", "ItemType"))
                    haveErrors = true
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL) || { haveErrors = errorOnTokenType(XPathTokenType.EQUAL, XQueryBundle.message("parser.error.expected-variable-value")); haveErrors }()) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseExprSingle(XQueryElementType.VAR_VALUE) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected-expression"))
                }
            } else if (builder.matchTokenType(XQueryTokenType.K_EXTERNAL)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL) || { haveErrors = errorOnTokenType(XPathTokenType.EQUAL, XQueryBundle.message("parser.error.expected-variable-value")); haveErrors }()) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseExprSingle(XQueryElementType.VAR_DEFAULT_VALUE) && !haveErrors) {
                        error(XPathBundle.message("parser.error.expected-expression"))
                    }
                }
            } else {
                error(XQueryBundle.message("parser.error.expected-variable-value"))
                parseExprSingle(XQueryElementType.VAR_VALUE)
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseAnnotatedDecl(): Boolean {
        var haveAnnotations = false
        var firstAnnotation: IElementType? = null
        var annotation: IElementType?
        do {
            annotation = if (parseAnnotation()) {
                XQueryElementType.ANNOTATION
            } else {
                parseCompatibilityAnnotationDecl()
            }

            if (firstAnnotation == null) {
                firstAnnotation = annotation
            }

            if (annotation != null) {
                parseWhiteSpaceAndCommentTokens(builder)
                haveAnnotations = true
            }
        } while (annotation != null)

        val declMarker = mark()
        if (parseVarDecl()) {
            declMarker.done(XQueryElementType.VAR_DECL)
            return true
        } else if (parseFunctionDecl(declMarker, firstAnnotation)) {
            return true
        } else if (haveAnnotations) {
            error(XQueryBundle.message("parser.error.expected-keyword", "function, variable"))
            parseUnknownDecl()
            declMarker.done(XQueryElementType.UNKNOWN_DECL)
            return true
        }
        declMarker.drop()
        return false
    }

    private fun parseAnnotation(): Boolean {
        val annotationMarker = matchTokenTypeWithMarker(XQueryTokenType.ANNOTATION_INDICATOR)
        if (annotationMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                do {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseLiteral(builder) && !haveErrors) {
                        error(XPathBundle.message("parser.error.expected", "Literal"))
                        haveErrors = true
                    }
                    parseWhiteSpaceAndCommentTokens(builder)
                } while (builder.matchTokenType(XPathTokenType.COMMA))

                if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", ")"))
                }
            }

            annotationMarker.done(XQueryElementType.ANNOTATION)
            return true
        }
        return false
    }

    private fun parseCompatibilityAnnotationDecl(): IElementType? {
        val compatibilityAnnotationMarker = mark()
        val type = getTokenType()
        if (COMPATIBILITY_ANNOTATION_TOKENS.contains(type)) {
            advanceLexer()
            compatibilityAnnotationMarker.done(XQueryElementType.COMPATIBILITY_ANNOTATION)
            return type
        }
        compatibilityAnnotationMarker.drop()
        return null
    }

    private fun parseVarDecl(): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_VARIABLE)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
                error(XPathBundle.message("parser.error.expected", "$"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseTypeDeclaration()

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL) || { haveErrors = haveErrors or errorOnTokenType(XPathTokenType.EQUAL, XQueryBundle.message("parser.error.expected-variable-value")); haveErrors }()) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseExprSingle(XQueryElementType.VAR_VALUE) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected-expression"))
                }
            } else if (builder.matchTokenType(XQueryTokenType.K_EXTERNAL)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseExprSingle(XQueryElementType.VAR_DEFAULT_VALUE) && !haveErrors) {
                        error(XPathBundle.message("parser.error.expected-expression"))
                    }
                }
            } else {
                error(XQueryBundle.message("parser.error.expected-variable-value"))
                parseExprSingle(XQueryElementType.VAR_VALUE)
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseFunctionDecl(functionDeclMarker: PsiBuilder.Marker, firstAnnotation: IElementType?): Boolean {
        if (builder.matchTokenType(XPathTokenType.K_FUNCTION)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (getTokenType() === XPathTokenType.STRING_LITERAL_START) {
                // DefaultNamespaceDecl with missing 'default' keyword.
                error(XPathBundle.message("parser.error.expected", "("))
                parseStringLiteral(builder)
                parseWhiteSpaceAndCommentTokens(builder)
                functionDeclMarker.done(XQueryElementType.UNKNOWN_DECL)
                return true
            } else if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "("))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseParamList()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_AS)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSequenceType()) {
                    error(XPathBundle.message("parser.error.expected", "SequenceType"))
                    haveErrors = true
                }
            }

            val bodyType = if (firstAnnotation === XQueryTokenType.K_SEQUENTIAL)
                XQueryElementType.BLOCK
            else
                XQueryElementType.FUNCTION_BODY

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_EXTERNAL) &&
                    !parseEnclosedExprOrBlock(bodyType, BlockOpen.REQUIRED, BlockExpr.OPTIONAL) &&
                    !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-enclosed-expression-or-keyword", "external"))
                parseExpr(XQueryElementType.EXPR, true)

                parseWhiteSpaceAndCommentTokens(builder)
                builder.matchTokenType(XPathTokenType.BLOCK_CLOSE)
            }

            parseWhiteSpaceAndCommentTokens(builder)
            functionDeclMarker.done(XQueryElementType.FUNCTION_DECL)
            return true
        }

        return false
    }

    private fun parseParamList(): Boolean {
        val paramListMarker = mark()

        while (parseParam()) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (getTokenType() === XPathTokenType.VARIABLE_INDICATOR) {
                error(XPathBundle.message("parser.error.expected", ","))
            } else if (!builder.matchTokenType(XPathTokenType.COMMA)) {
                builder.matchTokenType(XPathTokenType.ELLIPSIS)

                paramListMarker.done(XPathElementType.PARAM_LIST)
                return true
            }

            parseWhiteSpaceAndCommentTokens(builder)
        }

        paramListMarker.drop()
        return false
    }

    private fun parseParam(): Boolean {
        val paramMarker = mark()
        if (builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseTypeDeclaration()

            paramMarker.done(XPathElementType.PARAM)
            return true
        } else if (getTokenType() === XPathTokenType.NCNAME || getTokenType() is IKeywordOrNCNameType || getTokenType() === XPathTokenType.QNAME_SEPARATOR) {
            error(XPathBundle.message("parser.error.expected", "$"))
            parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)

            parseWhiteSpaceAndCommentTokens(builder)
            parseTypeDeclaration()

            paramMarker.done(XPathElementType.PARAM)
            return true
        }

        paramMarker.drop()
        return false
    }

    private fun parseOptionDecl(): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_OPTION)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-option-string"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    private fun parseTypeDecl(): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_TYPE)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                error(XQueryBundle.message("parser.error.expected-qname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.EQUAL) && !{ haveErrors = errorOnTokenType(XPathTokenType.ASSIGN_EQUAL, XPathBundle.message("parser.error.expected", "=")); haveErrors }()) {
                error(XPathBundle.message("parser.error.expected", "="))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseItemType() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "ItemType"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: EnclosedExpr|Block

    private enum class BlockOpen {
        REQUIRED,
        OPTIONAL
    }

    private enum class BlockExpr {
        REQUIRED,
        OPTIONAL
    }

    private fun parseEnclosedExprOrBlock(type: IElementType?, blockOpen: BlockOpen, blockExpr: BlockExpr): Boolean {
        var haveErrors = false
        val enclosedExprMarker = if (type == null) null else mark()
        if (!builder.matchTokenType(XPathTokenType.BLOCK_OPEN)) {
            if (blockOpen == BlockOpen.OPTIONAL) {
                error(XPathBundle.message("parser.error.expected", "{"))
                haveErrors = true
            } else {
                enclosedExprMarker?.drop()
                return false
            }
        }

        var exprType = XQueryElementType.EXPR
        if (type === XQueryElementType.BLOCK || type === XQueryElementType.WHILE_BODY) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseBlockDecls()
            exprType = XQueryElementType.BLOCK_BODY
        }

        parseWhiteSpaceAndCommentTokens(builder)
        var haveExpr = parseExpr(exprType)
        if (!haveExpr && blockExpr == BlockExpr.REQUIRED) {
            error(XPathBundle.message("parser.error.expected-expression"))
            haveErrors = true
        }

        parseWhiteSpaceAndCommentTokens(builder)
        if (builder.matchTokenType(XPathTokenType.BLOCK_CLOSE)) {
            haveExpr = true
        } else if (!haveErrors) {
            error(XPathBundle.message("parser.error.expected", "}"))
        }

        if (enclosedExprMarker != null) {
            if (haveExpr) {
                enclosedExprMarker.done(type!!)
                return true
            }
            enclosedExprMarker.drop()
        }
        return haveExpr
    }

    private fun parseBlockDecls(): Boolean {
        val blockDeclsMarker = mark()
        parseWhiteSpaceAndCommentTokens(builder)
        while (true)
            when (parseBlockVarDecl()) {
                XQueryParserImpl.ParseStatus.MATCHED -> {
                    if (!builder.matchTokenType(XQueryTokenType.SEPARATOR)) {
                        error(XPathBundle.message("parser.error.expected", ";"))
                        if (getTokenType() === XPathTokenType.QNAME_SEPARATOR) {
                            advanceLexer()
                        }
                    }
                    parseWhiteSpaceAndCommentTokens(builder)
                }
                XQueryParserImpl.ParseStatus.MATCHED_WITH_ERRORS -> {
                    builder.matchTokenType(XQueryTokenType.SEPARATOR)
                    parseWhiteSpaceAndCommentTokens(builder)
                }
                XQueryParserImpl.ParseStatus.NOT_MATCHED -> {
                    blockDeclsMarker.done(XQueryElementType.BLOCK_DECLS)
                    return true
                }
            }
    }

    private fun parseBlockVarDecl(): ParseStatus {
        val blockVarDeclMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DECLARE)
        if (blockVarDeclMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (getTokenType() === XPathTokenType.PARENTHESIS_OPEN || getTokenType() === XPathTokenType.QNAME_SEPARATOR) {
                // 'declare' used as a function name.
                blockVarDeclMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            var status: ParseStatus
            do {
                parseWhiteSpaceAndCommentTokens(builder)
                status = parseBlockVarDeclEntry()
                if (status == ParseStatus.NOT_MATCHED) {
                    status = ParseStatus.MATCHED_WITH_ERRORS
                }
            } while (builder.matchTokenType(XPathTokenType.COMMA))

            blockVarDeclMarker.done(XQueryElementType.BLOCK_VAR_DECL)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseBlockVarDeclEntry(): ParseStatus {
        val blockVarDeclEntryMarker = mark()
        var haveErrors = false
        if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
            error(XPathBundle.message("parser.error.expected", "$"))
            if (getTokenType() === XQueryTokenType.SEPARATOR) {
                blockVarDeclEntryMarker.drop()
                return ParseStatus.NOT_MATCHED
            }
            haveErrors = true
        }

        parseWhiteSpaceAndCommentTokens(builder)
        if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
            error(XPathBundle.message("parser.error.expected-eqname"))
            haveErrors = true
        }

        parseWhiteSpaceAndCommentTokens(builder)
        val errorMessage = if (parseTypeDeclaration())
            "parser.error.expected-variable-assign-scripting"
        else
            "parser.error.expected-variable-assign-scripting-no-type-decl"

        parseWhiteSpaceAndCommentTokens(builder)
        if (builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL) || { haveErrors = errorOnTokenType(XPathTokenType.EQUAL, XQueryBundle.message(errorMessage)); haveErrors }()) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }
            parseWhiteSpaceAndCommentTokens(builder)
        } else if (getTokenType() !== XPathTokenType.COMMA && getTokenType() !== XQueryTokenType.SEPARATOR) {
            error(XQueryBundle.message(errorMessage))
            parseExprSingle(builder)
            parseWhiteSpaceAndCommentTokens(builder)
            haveErrors = true
        }
        blockVarDeclEntryMarker.done(XQueryElementType.BLOCK_VAR_DECL_ENTRY)
        return if (haveErrors) ParseStatus.MATCHED_WITH_ERRORS else ParseStatus.MATCHED
    }

    // endregion
    // region Grammar :: Expr

    private fun parseExpr(type: IElementType, functionDeclRecovery: Boolean = false): Boolean {
        val exprMarker = mark()
        if (parseApplyExpr(type, functionDeclRecovery)) {
            exprMarker.done(type)
            return true
        }
        exprMarker.drop()
        return false
    }

    private fun parseApplyExpr(type: IElementType, functionDeclRecovery: Boolean): Boolean {
        // NOTE: No marker is captured here because the Expr node is an instance
        // of the ApplyExpr node and there are no other uses of ApplyExpr.
        var haveConcatExpr = false
        while (true) {
            if (!parseConcatExpr()) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (functionDeclRecovery || !errorOnTokenType(XQueryTokenType.SEPARATOR, XQueryBundle.message("parser.error.expected-query-statement", ";"))) {
                    return haveConcatExpr
                } else {
                    // Semicolon without a query body -- continue parsing.
                    parseWhiteSpaceAndCommentTokens(builder)
                    continue
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)

            val marker = mark()
            when (parseTransactionSeparator()) {
                XQueryParserImpl.TransactionType.WITH_PROLOG -> {
                    // MarkLogic transaction containing a Prolog/Module statement.
                    marker.rollbackTo()
                    return true
                }
                XQueryParserImpl.TransactionType.WITHOUT_PROLOG -> {
                    if (type !== XQueryElementType.QUERY_BODY) {
                        // ScriptingSpec Extension: Use a Separator as part of the ApplyExpr.
                        marker.rollbackTo()
                        builder.matchTokenType(XQueryTokenType.SEPARATOR)
                    } else {
                        // ScriptingSpec Extension, or MarkLogic Transaction: Keep the MarkLogic TransactionSeparator.
                        marker.drop()
                    }
                    parseWhiteSpaceAndCommentTokens(builder)
                }
                XQueryParserImpl.TransactionType.NONE -> {
                    marker.rollbackTo()
                    if (haveConcatExpr) {
                        if (type !== XQueryElementType.QUERY_BODY) {
                            // ScriptingSpec Extension: The semicolon is required to end a ConcatExpr.
                            error(XPathBundle.message("parser.error.expected", ";"))
                        } else {
                            // ScriptingSpec Extension: The semicolon is required to end a ConcatExpr.
                            // MarkLogic Transactions: The last expression must not end with a semicolon.
                            val marker2 = mark()
                            marker2.done(XQueryElementType.TRANSACTION_SEPARATOR)
                        }
                    }
                    return true
                }
            }

            haveConcatExpr = true
        }
    }

    private fun parseConcatExpr(): Boolean {
        return parseExpr(builder, XQueryElementType.CONCAT_EXPR)
    }

    override fun parseExprSingle(builder: PsiBuilder): Boolean {
        return parseExprSingleImpl(null)
    }

    private fun parseExprSingle(type: IElementType?, parentType: IElementType? = null): Boolean {
        if (type == null) {
            return parseExprSingleImpl(parentType)
        }

        val exprSingleMarker = mark()
        if (parseExprSingleImpl(parentType)) {
            exprSingleMarker.done(type)
            return true
        }

        exprSingleMarker.drop()
        return false
    }

    private fun parseExprSingleImpl(parentType: IElementType?): Boolean {
        return (parseFLWORExpr()
                || parseQuantifiedExpr()
                || parseSwitchExpr()
                || parseTypeswitchExpr()
                || parseIfExpr()
                || parseTryCatchExpr()
                || parseInsertExpr()
                || parseDeleteExpr()
                || parseRenameExpr()
                || parseReplaceExpr()
                || parseCopyModifyExpr()
                || parseUpdatingFunctionCall()
                || parseBlockExpr()
                || parseAssignmentExpr()
                || parseExitExpr()
                || parseWhileExpr()
                || parseTernaryIfExpr(parentType))
    }

    // endregion
    // region Grammar :: Expr :: FLWORExpr

    private fun parseFLWORExpr(): Boolean {
        val flworExprMarker = mark()
        if (parseInitialClause()) {
            while (parseIntermediateClause()) {
                //
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseReturnClause()) {
                error(XQueryBundle.message("parser.error.expected-keyword", "count, for, group, let, order, return, sliding, stable, tumbling, where"))
                parseWhiteSpaceAndCommentTokens(builder)
                parseExprSingle(builder)
            }

            flworExprMarker.done(XQueryElementType.FLWOR_EXPR)
            return true
        } else if (errorOnTokenType(XPathTokenType.K_RETURN, XQueryBundle.message("parser.error.return-without-flwor"))) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseQNameSeparator(builder, null)) { // QName
                flworExprMarker.rollbackTo()
                return false
            }

            return if (getTokenType() !== XPathTokenType.PARENTHESIS_OPEN && parseExprSingle(builder)) {
                flworExprMarker.drop()
                true
            } else {
                flworExprMarker.rollbackTo()
                false
            }
        }
        flworExprMarker.drop()
        return false
    }

    private fun parseInitialClause(): Boolean {
        return parseForOrWindowClause() || parseLetClause()
    }

    private fun parseIntermediateClause(): Boolean {
        val intermediateClauseMarker = mark()
        if (parseInitialClause() || parseWhereClause() || parseOrderByClause() || parseCountClause() || parseGroupByClause()) {
            intermediateClauseMarker.done(XQueryElementType.INTERMEDIATE_CLAUSE)
            return true
        }
        intermediateClauseMarker.drop()
        return false
    }

    private fun parseReturnClause(): Boolean {
        val returnClauseMarker = mark()
        if (builder.matchTokenType(XPathTokenType.K_RETURN)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder)) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            returnClauseMarker.done(XQueryElementType.RETURN_CLAUSE)
            return true
        }
        returnClauseMarker.drop()
        return false
    }

    private fun parseForOrWindowClause(): Boolean {
        val forClauseMarker = matchTokenTypeWithMarker(XPathTokenType.K_FOR)
        if (forClauseMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            return if (parseForClause()) {
                forClauseMarker.done(XQueryElementType.FOR_CLAUSE)
                true
            } else if (parseTumblingWindowClause() || parseSlidingWindowClause()) {
                forClauseMarker.done(XQueryElementType.WINDOW_CLAUSE)
                true
            } else {
                forClauseMarker.rollbackTo()
                false
            }
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: FLWORExpr :: ForClause

    private fun parseForClause(): Boolean {
        if (parseForBinding(true)) {
            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.COMMA)) {
                parseWhiteSpaceAndCommentTokens(builder)
                parseForBinding(false)
                parseWhiteSpaceAndCommentTokens(builder)
            }
            return true
        }
        return false
    }

    private fun parseForBinding(isFirst: Boolean): Boolean {
        val forBindingMarker = mark()

        var haveErrors = false
        val matched = builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)
        if (!matched && !isFirst) {
            error(XPathBundle.message("parser.error.expected", "$"))
            haveErrors = true
        }

        if (matched || !isFirst) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            val haveTypeDeclaration = parseTypeDeclaration()

            parseWhiteSpaceAndCommentTokens(builder)
            val haveAllowingEmpty = parseAllowingEmpty()

            parseWhiteSpaceAndCommentTokens(builder)
            val havePositionalVar = parsePositionalVar()

            parseWhiteSpaceAndCommentTokens(builder)
            val haveScoreVar = parseFTScoreVar()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_IN) && !haveErrors) {
                if (haveScoreVar) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "in"))
                } else if (havePositionalVar) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "in, score"))
                } else if (haveAllowingEmpty) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "at, in, score"))
                } else if (haveTypeDeclaration) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "allowing, at, in, score"))
                } else {
                    error(XQueryBundle.message("parser.error.expected-keyword", "allowing, as, at, in, score"))
                }
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            forBindingMarker.done(XQueryElementType.FOR_BINDING)
            return true
        }
        forBindingMarker.drop()
        return false
    }

    private fun parseAllowingEmpty(): Boolean {
        val allowingEmptyMarker = matchTokenTypeWithMarker(XQueryTokenType.K_ALLOWING)
        if (allowingEmptyMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_EMPTY)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "empty"))
            }

            allowingEmptyMarker.done(XQueryElementType.ALLOWING_EMPTY)
            return true
        }
        return false
    }

    private fun parsePositionalVar(): Boolean {
        val positionalVarMarker = matchTokenTypeWithMarker(XQueryTokenType.K_AT)
        if (positionalVarMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
                error(XPathBundle.message("parser.error.expected", "$"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-eqname"))
            }

            positionalVarMarker.done(XQueryElementType.POSITIONAL_VAR)
            return true
        }
        return false
    }

    private fun parseFTScoreVar(): Boolean {
        val scoreVarMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SCORE)
        if (scoreVarMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
                error(XPathBundle.message("parser.error.expected", "$"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-eqname"))
            }

            scoreVarMarker.done(XQueryElementType.FT_SCORE_VAR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: FLWORExpr :: LetClause

    private fun parseLetClause(): Boolean {
        val letClauseMarker = mark()
        if (builder.matchTokenType(XPathTokenType.K_LET)) {
            var isFirst = true
            do {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseLetBinding(isFirst) && isFirst) {
                    letClauseMarker.rollbackTo()
                    return false
                }

                isFirst = false
                parseWhiteSpaceAndCommentTokens(builder)
            } while (builder.matchTokenType(XPathTokenType.COMMA))

            letClauseMarker.done(XQueryElementType.LET_CLAUSE)
            return true
        }
        letClauseMarker.drop()
        return false
    }

    private fun parseLetBinding(isFirst: Boolean): Boolean {
        val letBindingMarker = mark()

        var haveErrors = false
        val haveVariableIndicator = builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)
        val matched = haveVariableIndicator || parseFTScoreVar()
        if (!matched) {
            error(XQueryBundle.message("parser.error.expected-keyword-or-token", "$", "score"))
            haveErrors = true
        }

        if (matched || !isFirst) {
            val errorMessage: String
            if (haveVariableIndicator) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false)) {
                    error(XPathBundle.message("parser.error.expected-eqname"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                errorMessage = if (parseTypeDeclaration())
                    XPathBundle.message("parser.error.expected", ":=")
                else
                    XQueryBundle.message("parser.error.expected-variable-assign-or-keyword", "as")
            } else {
                errorMessage = XPathBundle.message("parser.error.expected", ":=")
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (errorOnTokenType(XPathTokenType.EQUAL, errorMessage)) {
                haveErrors = true
            } else if (!builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL) && !haveErrors) {
                error(errorMessage)
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            letBindingMarker.done(XQueryElementType.LET_BINDING)
            return true
        }
        letBindingMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Expr :: FLWORExpr :: WindowClause

    private fun parseTumblingWindowClause(): Boolean {
        val tumblingWindowClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_TUMBLING)
        if (tumblingWindowClauseMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_WINDOW)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "window"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "$"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            val haveTypeDeclaration = parseTypeDeclaration()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_IN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", if (haveTypeDeclaration) "in" else "as, in"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseWindowStartCondition() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "WindowStartCondition"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseWindowEndCondition()

            tumblingWindowClauseMarker.done(XQueryElementType.TUMBLING_WINDOW_CLAUSE)
            return true
        }
        return false
    }

    private fun parseSlidingWindowClause(): Boolean {
        val slidingWindowClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SLIDING)
        if (slidingWindowClauseMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_WINDOW)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "window"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "$"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            val haveTypeDeclaration = parseTypeDeclaration()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_IN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", if (haveTypeDeclaration) "in" else "as, in"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseWindowStartCondition() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "WindowStartCondition"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseWindowEndCondition() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "WindowEndCondition"))
            }

            slidingWindowClauseMarker.done(XQueryElementType.SLIDING_WINDOW_CLAUSE)
            return true
        }
        return false
    }

    private fun parseWindowStartCondition(): Boolean {
        val windowStartConditionMarker = matchTokenTypeWithMarker(XQueryTokenType.K_START)
        if (windowStartConditionMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            parseWindowVars()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_WHEN)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "when"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            windowStartConditionMarker.done(XQueryElementType.WINDOW_START_CONDITION)
            return true
        }
        return false
    }

    private fun parseWindowEndCondition(): Boolean {
        var haveErrors = false

        var windowEndConditionMarker = matchTokenTypeWithMarker(XQueryTokenType.K_END)
        if (windowEndConditionMarker == null) {
            windowEndConditionMarker = matchTokenTypeWithMarker(XQueryTokenType.K_ONLY)
            if (windowEndConditionMarker != null) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XQueryTokenType.K_END)) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "end"))
                    haveErrors = true
                }
            }
        }

        if (windowEndConditionMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseWindowVars()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_WHEN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "when"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            windowEndConditionMarker.done(XQueryElementType.WINDOW_END_CONDITION)
            return true
        }
        return false
    }

    private fun parseWindowVars(): Boolean {
        val windowVarsMarker = mark()
        var haveErrors = false

        parseWhiteSpaceAndCommentTokens(builder)
        if (builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.CURRENT_ITEM, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }
        }

        parseWhiteSpaceAndCommentTokens(builder)
        parsePositionalVar()

        parseWhiteSpaceAndCommentTokens(builder)
        if (builder.matchTokenType(XQueryTokenType.K_PREVIOUS)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "$"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.PREVIOUS_ITEM, false) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }
        }

        parseWhiteSpaceAndCommentTokens(builder)
        if (builder.matchTokenType(XQueryTokenType.K_NEXT)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "$"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.NEXT_ITEM, false) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-eqname"))
            }
        }

        windowVarsMarker.done(XQueryElementType.WINDOW_VARS)
        return true
    }

    // endregion
    // region Grammar :: Expr :: FLWORExpr :: CountClause

    private fun parseCountClause(): Boolean {
        val countClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_COUNT)
        if (countClauseMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
                error(XPathBundle.message("parser.error.expected", "$"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-qname"))
            }

            countClauseMarker.done(XQueryElementType.COUNT_CLAUSE)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: FLWORExpr :: WhereClause

    private fun parseWhereClause(): Boolean {
        val whereClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_WHERE)
        if (whereClauseMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder)) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            whereClauseMarker.done(XQueryElementType.WHERE_CLAUSE)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: FLWORExpr :: GroupByClause

    private fun parseGroupByClause(): Boolean {
        val groupByClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_GROUP)
        if (groupByClauseMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_BY)) {
                error(XPathBundle.message("parser.error.expected", "by"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseGroupingSpecList() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "GroupingSpecList"))
            }

            groupByClauseMarker.done(XQueryElementType.GROUP_BY_CLAUSE)
            return true
        }
        return false
    }

    private fun parseGroupingSpecList(): Boolean {
        val groupingSpecListMarker = mark()
        if (parseGroupingSpec()) {
            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.COMMA)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseGroupingSpec()) {
                    error(XPathBundle.message("parser.error.expected", "GroupingSpec"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
            }

            groupingSpecListMarker.done(XQueryElementType.GROUPING_SPEC_LIST)
            return true
        }
        groupingSpecListMarker.drop()
        return false
    }

    private fun parseGroupingSpec(): Boolean {
        val groupingSpecListMarker = mark()
        if (parseGroupingVariable()) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseTypeDeclaration()) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (errorOnTokenType(XPathTokenType.EQUAL, XPathBundle.message("parser.error.expected", ":="))) {
                    haveErrors = true
                } else if (!builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL)) {
                    error(XPathBundle.message("parser.error.expected", ":="))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseExprSingle(builder) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected-expression"))
                    haveErrors = true
                }
            } else if (builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL) || { haveErrors = errorOnTokenType(XPathTokenType.EQUAL, XPathBundle.message("parser.error.expected", ":=")); haveErrors }()) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseExprSingle(builder) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected-expression"))
                    haveErrors = true
                }
            }

            if (builder.matchTokenType(XQueryTokenType.K_COLLATION)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected-expression"))
                }
            }

            groupingSpecListMarker.done(XQueryElementType.GROUPING_SPEC)
            return true
        }
        groupingSpecListMarker.drop()
        return false
    }

    private fun parseGroupingVariable(): Boolean {
        val groupingVariableMarker = matchTokenTypeWithMarker(XPathTokenType.VARIABLE_INDICATOR)
        if (groupingVariableMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
            }

            groupingVariableMarker.done(XQueryElementType.GROUPING_VARIABLE)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: FLWORExpr :: OrderByClause

    private fun parseOrderByClause(): Boolean {
        val orderByClauseMarker = mark()
        if (builder.matchTokenType(XQueryTokenType.K_ORDER)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_BY)) {
                error(XPathBundle.message("parser.error.expected", "by"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseOrderSpecList() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "OrderSpecList"))
            }

            orderByClauseMarker.done(XQueryElementType.ORDER_BY_CLAUSE)
            return true
        } else if (builder.matchTokenType(XQueryTokenType.K_STABLE)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_ORDER)) {
                error(XPathBundle.message("parser.error.expected", "order"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_BY) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "by"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseOrderSpecList() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "OrderSpecList"))
            }

            orderByClauseMarker.done(XQueryElementType.ORDER_BY_CLAUSE)
            return true
        }
        orderByClauseMarker.drop()
        return false
    }

    private fun parseOrderSpecList(): Boolean {
        val orderSpecListMarker = mark()
        if (parseOrderSpec()) {
            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.COMMA)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseOrderSpec()) {
                    error(XPathBundle.message("parser.error.expected", "OrderSpec"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
            }

            orderSpecListMarker.done(XQueryElementType.ORDER_SPEC_LIST)
            return true
        }
        orderSpecListMarker.drop()
        return false
    }

    private fun parseOrderSpec(): Boolean {
        val orderSpecMarker = mark()
        if (parseExprSingle(builder)) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseOrderModifier()

            orderSpecMarker.done(XQueryElementType.ORDER_SPEC)
            return true
        }
        orderSpecMarker.drop()
        return false
    }

    private fun parseOrderModifier(): Boolean {
        val orderModifierMarker = mark()

        if (builder.matchTokenType(XQueryTokenType.K_ASCENDING) || builder.matchTokenType(XQueryTokenType.K_DESCENDING)) {
            //
        }

        parseWhiteSpaceAndCommentTokens(builder)
        if (builder.matchTokenType(XPathTokenType.K_EMPTY)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_GREATEST) && !builder.matchTokenType(XQueryTokenType.K_LEAST)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "greatest, least"))
            }
        }

        parseWhiteSpaceAndCommentTokens(builder)
        if (builder.matchTokenType(XQueryTokenType.K_COLLATION)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL)) {
                error(XQueryBundle.message("parser.error.expected-uri-string"))
            }
        }

        orderModifierMarker.done(XQueryElementType.ORDER_MODIFIER)
        return false
    }

    // endregion
    // region Grammar :: Expr :: QuantifiedExpr

    private fun parseQuantifiedExpr(): Boolean {
        val quantifiedExprMarker = matchTokenTypeWithMarker(XPathTokenType.K_SOME, XPathTokenType.K_EVERY)
        if (quantifiedExprMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseQNameSeparator(builder, null)) { // QName
                quantifiedExprMarker.rollbackTo()
                return false
            }

            val hasBinding = parseQuantifiedExprBinding(true)
            if (hasBinding) {
                parseWhiteSpaceAndCommentTokens(builder)
                while (builder.matchTokenType(XPathTokenType.COMMA)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    parseQuantifiedExprBinding(false)
                    parseWhiteSpaceAndCommentTokens(builder)
                }
            }

            var haveErrors = false
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_SATISFIES)) {
                if (hasBinding) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "satisfies"))
                    haveErrors = true
                } else { // NCName
                    quantifiedExprMarker.rollbackTo()
                    return false
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            quantifiedExprMarker.done(XPathElementType.QUANTIFIED_EXPR)
            return true
        }
        return false
    }

    private fun parseQuantifiedExprBinding(isFirst: Boolean): Boolean {
        val bindingMarker = mark()

        var haveErrors = false
        val matched = builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)
        if (!matched && !isFirst) {
            error(XPathBundle.message("parser.error.expected", "$"))
            haveErrors = true
        }

        if (matched || !isFirst) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            val haveTypeDeclaration = parseTypeDeclaration()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_IN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", if (haveTypeDeclaration) "in" else "as, in"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            bindingMarker.done(XPathElementType.QUANTIFIED_EXPR_BINDING)
            return true
        }
        bindingMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Expr :: SwitchExpr

    private fun parseSwitchExpr(): Boolean {
        val switchExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SWITCH)
        if (switchExprMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                switchExprMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExpr(XQueryElementType.EXPR)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            var matched = false
            while (parseSwitchCaseClause()) {
                matched = true
                parseWhiteSpaceAndCommentTokens(builder)
            }
            if (!matched) {
                error(XPathBundle.message("parser.error.expected", "SwitchCaseClause"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_DEFAULT) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "case, default"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_RETURN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "return"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            switchExprMarker.done(XQueryElementType.SWITCH_EXPR)
            return true
        }
        return false
    }

    private fun parseSwitchCaseClause(): Boolean {
        val switchCaseClauseMarker = mark()

        var haveErrors = false
        var haveCase = false
        while (builder.matchTokenType(XQueryTokenType.K_CASE)) {
            haveCase = true
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseSwitchCaseOperand()) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }
            parseWhiteSpaceAndCommentTokens(builder)
        }

        if (haveCase) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_RETURN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "return"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            switchCaseClauseMarker.done(XQueryElementType.SWITCH_CASE_CLAUSE)
            return true
        }

        switchCaseClauseMarker.drop()
        return false
    }

    private fun parseSwitchCaseOperand(): Boolean {
        val switchCaseOperandMarker = mark()
        if (parseExprSingle(builder)) {
            switchCaseOperandMarker.done(XQueryElementType.SWITCH_CASE_OPERAND)
            return true
        }
        switchCaseOperandMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Expr :: TypeswitchExpr

    private fun parseTypeswitchExpr(): Boolean {
        val typeswitchExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_TYPESWITCH)
        if (typeswitchExprMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                typeswitchExprMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExpr(XQueryElementType.EXPR)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            var matched = false
            while (parseCaseClause()) {
                matched = true
                parseWhiteSpaceAndCommentTokens(builder)
            }
            if (!matched) {
                error(XPathBundle.message("parser.error.expected", "CaseClause"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseDefaultCaseClause()

            typeswitchExprMarker.done(XQueryElementType.TYPESWITCH_EXPR)
            return true
        }
        return false
    }

    private fun parseCaseClause(): Boolean {
        val caseClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_CASE)
        if (caseClauseMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false)) {
                    error(XPathBundle.message("parser.error.expected-eqname"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.K_AS) && !haveErrors) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "as"))
                    haveErrors = true
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseSequenceTypeUnion() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "SequenceType"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_RETURN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "return"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            caseClauseMarker.done(XQueryElementType.CASE_CLAUSE)
            return true
        }
        return false
    }

    private fun parseDefaultCaseClause(): Boolean {
        val caseClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DEFAULT)
        if (caseClauseMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false)) {
                    error(XPathBundle.message("parser.error.expected-eqname"))
                    haveErrors = true
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_RETURN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "return"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            caseClauseMarker.done(XQueryElementType.DEFAULT_CASE_CLAUSE)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: IfExpr

    private fun parseIfExpr(): Boolean {
        val ifExprMarker = matchTokenTypeWithMarker(XPathTokenType.K_IF)
        if (ifExprMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                ifExprMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExpr(XQueryElementType.EXPR)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_THEN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "then"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_ELSE)) { // else branch is optional in BaseX 9.1
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseExprSingle(builder) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected-expression"))
                }
            }

            ifExprMarker.done(XPathElementType.IF_EXPR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: TryCatchExpr

    private enum class CatchClauseType {
        NONE,
        XQUERY_30,
        MARK_LOGIC
    }

    private fun parseTryCatchExpr(): Boolean {
        val tryExprMarker = mark()
        if (parseTryClause()) {
            var type = CatchClauseType.NONE

            parseWhiteSpaceAndCommentTokens(builder)
            while (true) {
                val nextType = parseCatchClause(type)
                if (nextType == CatchClauseType.NONE) {
                    if (type == CatchClauseType.NONE) {
                        error(XPathBundle.message("parser.error.expected", "CatchClause"))
                    }

                    tryExprMarker.done(XQueryElementType.TRY_CATCH_EXPR)
                    return true
                } else if (type != CatchClauseType.MARK_LOGIC) {
                    type = nextType
                }

                parseWhiteSpaceAndCommentTokens(builder)
            }
        }
        tryExprMarker.drop()
        return false
    }

    private fun parseTryClause(): Boolean {
        val tryClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_TRY)
        if (tryClauseMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_TRY_TARGET_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                tryClauseMarker.rollbackTo()
                return false
            }

            tryClauseMarker.done(XQueryElementType.TRY_CLAUSE)
            return true
        }
        return false
    }

    private fun parseCatchClause(type: CatchClauseType): CatchClauseType {
        val catchClauseMarker = matchTokenTypeWithMarker(XQueryTokenType.K_CATCH)
        if (catchClauseMarker != null) {
            var haveErrors = false
            var nextType = CatchClauseType.XQUERY_30

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseCatchErrorList()) {
                //
            } else if (getTokenType() === XPathTokenType.PARENTHESIS_OPEN) {
                if (type == CatchClauseType.MARK_LOGIC) {
                    error(XQueryBundle.message("parser.error.multiple-marklogic-catch-clause"))
                }
                advanceLexer()

                nextType = CatchClauseType.MARK_LOGIC

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR)) {
                    error(XPathBundle.message("parser.error.expected", "$"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "VarName"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", ")"))
                }
            } else {
                error(XPathBundle.message("parser.error.expected", "CatchErrorList"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.OPTIONAL, BlockExpr.OPTIONAL)

            catchClauseMarker.done(XQueryElementType.CATCH_CLAUSE)
            return nextType
        }
        return CatchClauseType.NONE
    }

    private fun parseCatchErrorList(): Boolean {
        val catchErrorListMarker = mark()
        if (parseNameTest(builder, null)) {
            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.UNION)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseNameTest(builder, null)) {
                    error(XPathBundle.message("parser.error.expected", "NameTest"))
                }
                parseWhiteSpaceAndCommentTokens(builder)
            }
            catchErrorListMarker.done(XQueryElementType.CATCH_ERROR_LIST)
            return true
        }
        catchErrorListMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Expr :: InsertExpr

    private fun parseInsertExpr(): Boolean {
        val insertExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_INSERT)
        if (insertExprMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_NODE) && !builder.matchTokenType(XQueryTokenType.K_NODES)) {
                insertExprMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseSourceExpr()) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseInsertExprTargetChoice() && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "after, as, before, into"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseTargetExpr(null) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            insertExprMarker.done(XQueryElementType.INSERT_EXPR)
            return true
        }
        return false
    }

    private fun parseSourceExpr(): Boolean {
        val sourceExprMarker = mark()
        if (parseExprSingle(null, XQueryElementType.SOURCE_EXPR)) {
            sourceExprMarker.done(XQueryElementType.SOURCE_EXPR)
            return true
        }
        sourceExprMarker.drop()
        return false
    }

    private fun parseInsertExprTargetChoice(): Boolean {
        val insertExprTargetChoiceMarker = mark()
        if (builder.matchTokenType(XPathTokenType.K_AS)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_FIRST) && !builder.matchTokenType(XQueryTokenType.K_LAST)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "first, last"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_INTO) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "into"))
            }

            insertExprTargetChoiceMarker.done(XQueryElementType.INSERT_EXPR_TARGET_CHOICE)
            return true
        } else if (builder.matchTokenType(XQueryTokenType.K_INTO) ||
                builder.matchTokenType(XQueryTokenType.K_BEFORE) ||
                builder.matchTokenType(XQueryTokenType.K_AFTER)) {
            insertExprTargetChoiceMarker.done(XQueryElementType.INSERT_EXPR_TARGET_CHOICE)
            return true
        } else if (getTokenType() === XQueryTokenType.K_FIRST || getTokenType() === XQueryTokenType.K_LAST) {
            error(XQueryBundle.message("parser.error.expected-keyword", "as"))
            advanceLexer()

            parseWhiteSpaceAndCommentTokens(builder)
            builder.matchTokenType(XQueryTokenType.K_INTO)

            insertExprTargetChoiceMarker.done(XQueryElementType.INSERT_EXPR_TARGET_CHOICE)
            return true
        }

        insertExprTargetChoiceMarker.drop()
        return false
    }

    private fun parseTargetExpr(type: IElementType?): Boolean {
        val targetExprMarker = mark()
        if (parseExprSingle(null, type)) {
            targetExprMarker.done(XQueryElementType.TARGET_EXPR)
            return true
        }
        targetExprMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Expr :: DeleteExpr

    private fun parseDeleteExpr(): Boolean {
        val deleteExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DELETE)
        if (deleteExprMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_NODE) && !builder.matchTokenType(XQueryTokenType.K_NODES)) {
                deleteExprMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseTargetExpr(null)) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            deleteExprMarker.done(XQueryElementType.DELETE_EXPR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: ReplaceExpr

    private fun parseReplaceExpr(): Boolean {
        val replaceExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_REPLACE)
        if (replaceExprMarker != null) {
            var haveErrors = false
            var haveValueOf = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XQueryTokenType.K_VALUE)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.K_OF)) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "of"))
                    haveErrors = true
                }
                haveValueOf = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_NODE)) {
                if (!haveValueOf) {
                    replaceExprMarker.rollbackTo()
                    return false
                }
                if (!haveErrors) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "node"))
                    haveErrors = true
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseTargetExpr(null)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_WITH) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "with"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            replaceExprMarker.done(XQueryElementType.REPLACE_EXPR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: RenameExpr

    private fun parseRenameExpr(): Boolean {
        val renameExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_RENAME)
        if (renameExprMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_NODE)) {
                renameExprMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseTargetExpr(XQueryElementType.TARGET_EXPR)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_AS) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "as"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseNewNameExpr() && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            renameExprMarker.done(XQueryElementType.RENAME_EXPR)
            return true
        }
        return false
    }

    private fun parseNewNameExpr(): Boolean {
        val newNameExprMarker = mark()
        if (parseExprSingle(builder)) {
            newNameExprMarker.done(XQueryElementType.NEW_NAME_EXPR)
            return true
        }
        newNameExprMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Expr :: CopyModifyExpr (TransformExpr)

    private fun parseCopyModifyExpr(): Boolean {
        val copyModifyExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_COPY)
        if (copyModifyExprMarker != null) {
            var haveErrors = false
            var isFirstVarName = true
            do {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.VARIABLE_INDICATOR) && !haveErrors) {
                    if (isFirstVarName) {
                        copyModifyExprMarker.rollbackTo()
                        return false
                    } else {
                        error(XPathBundle.message("parser.error.expected", "$"))
                        haveErrors = true
                    }
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected-eqname"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (errorOnTokenType(XPathTokenType.EQUAL, XPathBundle.message("parser.error.expected", ":="))) {
                    haveErrors = true
                } else if (!builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", ":="))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseExprSingle(builder) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected-expression"))
                }

                isFirstVarName = false
                parseWhiteSpaceAndCommentTokens(builder)
            } while (builder.matchTokenType(XPathTokenType.COMMA))

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_MODIFY)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "modify"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_RETURN) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-keyword", "return"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            copyModifyExprMarker.done(XQueryElementType.COPY_MODIFY_EXPR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: BlockExpr

    private fun parseBlockExpr(): Boolean {
        val blockExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_BLOCK)
        if (blockExprMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.BLOCK, BlockOpen.REQUIRED, BlockExpr.REQUIRED)) {
                blockExprMarker.rollbackTo()
                return false
            }
            blockExprMarker.done(XQueryElementType.BLOCK_EXPR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: AssignmentExpr

    private fun parseAssignmentExpr(): Boolean {
        val assignmentExprMarker = matchTokenTypeWithMarker(XPathTokenType.VARIABLE_INDICATOR)
        if (assignmentExprMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL)) {
                // VarRef construct -- handle in the OrExpr parser for the correct AST.
                assignmentExprMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            assignmentExprMarker.done(XQueryElementType.ASSIGNMENT_EXPR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: ExitExpr

    private fun parseExitExpr(): Boolean {
        val exitExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_EXIT)
        if (exitExprMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseQNameSeparator(builder, null)) { // QName
                exitExprMarker.rollbackTo()
                return false
            }

            if (!builder.matchTokenType(XQueryTokenType.K_RETURNING)) {
                if (getTokenType() === XPathTokenType.PARENTHESIS_OPEN) {
                    // FunctionCall construct
                    exitExprMarker.rollbackTo()
                    return false
                }
                error(XQueryBundle.message("parser.error.expected-keyword", "returning"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder)) {
                if (haveErrors) {
                    // AbbrevForwardStep construct
                    exitExprMarker.rollbackTo()
                    return false
                }
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            exitExprMarker.done(XQueryElementType.EXIT_EXPR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: WhileExpr

    private fun parseWhileExpr(): Boolean {
        val whileExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_WHILE)
        if (whileExprMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                whileExprMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(builder)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.WHILE_BODY, BlockOpen.REQUIRED, BlockExpr.REQUIRED)) {
                // FunctionCall construct. Check for reserved function name in the FunctionCall PSI class.
                whileExprMarker.rollbackTo()
                return false
            }

            whileExprMarker.done(XQueryElementType.WHILE_EXPR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: TernaryIfExpr (OrExpr)

    private fun parseTernaryIfExpr(type: IElementType?): Boolean {
        val exprMarker = mark()
        if (parseElvisExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XQueryTokenType.TERNARY_IF)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseElvisExpr(null)) {
                    error(XPathBundle.message("parser.error.expected", "ElvisExpr"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XQueryTokenType.TERNARY_ELSE)) {
                    error(XPathBundle.message("parser.error.expected", "!!"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseElvisExpr(null)) {
                    error(XPathBundle.message("parser.error.expected", "ElvisExpr"))
                }

                exprMarker.done(XQueryElementType.TERNARY_IF_EXPR)
            } else {
                exprMarker.drop()
            }
            return true
        }
        exprMarker.drop()
        return false
    }

    private fun parseElvisExpr(type: IElementType?): Boolean {
        val exprMarker = mark()
        if (parseOrExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XQueryTokenType.ELVIS)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseOrExpr(null)) {
                    error(XPathBundle.message("parser.error.expected", "OrExpr"))
                }

                exprMarker.done(XQueryElementType.ELVIS_EXPR)
            } else {
                exprMarker.drop()
            }
            return true
        }
        exprMarker.drop()
        return false
    }

    private fun parseOrExpr(type: IElementType?): Boolean {
        val orExprMarker = mark()
        if (parseAndExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.K_OR) || builder.matchTokenType(XPathTokenType.K_ORELSE)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseAndExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "AndExpr"))
                }
            }

            orExprMarker.done(XPathElementType.OR_EXPR)
            return true
        }
        orExprMarker.drop()
        return false
    }

    private fun parseAndExpr(type: IElementType?): Boolean {
        val andExprMarker = mark()
        if (parseUpdateExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            var haveAndExpr = false
            while (builder.matchTokenType(XPathTokenType.K_AND) || builder.matchTokenType(XPathTokenType.K_ANDALSO)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseComparisonExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "ComparisonExpr"))
                }
                haveAndExpr = true
            }

            if (haveAndExpr)
                andExprMarker.done(XPathElementType.AND_EXPR)
            else
                andExprMarker.drop()
            return true
        }
        andExprMarker.drop()
        return false
    }

    private fun parseUpdateExpr(type: IElementType?): Boolean {
        val exprMarker = mark()
        if (parseComparisonExpr(type)) {
            var haveUpdateExpr = false
            while (builder.matchTokenType(XQueryTokenType.K_UPDATE)) {
                haveUpdateExpr = true

                parseWhiteSpaceAndCommentTokens(builder)
                if (getTokenType() === XPathTokenType.BLOCK_OPEN) {
                    parseEnclosedExprOrBlock(null, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)
                } else if (!parseExpr(XQueryElementType.EXPR)) {
                    error(XPathBundle.message("parser.error.expected-expression"))
                }
                parseWhiteSpaceAndCommentTokens(builder)
            }

            if (haveUpdateExpr)
                exprMarker.done(XQueryElementType.UPDATE_EXPR)
            else
                exprMarker.drop()
            return true
        }
        exprMarker.drop()
        return false
    }

    private fun parseComparisonExpr(type: IElementType?): Boolean {
        val comparisonExprMarker = mark()
        if (parseFTContainsExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseGeneralComp() || parseValueComp() || parseNodeComp()) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseFTContainsExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "FTContainsExpr"))
                }
                comparisonExprMarker.done(XPathElementType.COMPARISON_EXPR)
            } else {
                comparisonExprMarker.drop()
            }
            return true
        } else if (errorOnTokenType(XPathTokenType.LESS_THAN, XQueryBundle.message("parser.error.comparison-no-lhs-or-direlem"))) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTContainsExpr(type)) {
                error(XPathBundle.message("parser.error.expected", "FTContainsExpr"))
            }

            comparisonExprMarker.done(XPathElementType.COMPARISON_EXPR)
            return true
        }
        comparisonExprMarker.drop()
        return false
    }

    private fun parseFTContainsExpr(type: IElementType?): Boolean {
        val containsExprMarker = mark()
        if (parseStringConcatExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)

            if (builder.matchTokenType(XQueryTokenType.K_CONTAINS)) {
                var haveError = false

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.K_TEXT)) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "text"))
                    haveError = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseFTSelection() && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "FTSelection"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                parseFTIgnoreOption()
                containsExprMarker.done(XQueryElementType.FT_CONTAINS_EXPR)
            } else {
                containsExprMarker.drop()
            }
            return true
        }
        containsExprMarker.drop()
        return false
    }

    private fun parseFTIgnoreOption(): Boolean {
        val ignoreOptionMarker = matchTokenTypeWithMarker(XQueryTokenType.K_WITHOUT)
        if (ignoreOptionMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_CONTENT)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "content"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseUnionExpr(XQueryElementType.FT_IGNORE_OPTION) && !haveError) {
                error(XPathBundle.message("parser.error.expected", "UnionExpr"))
            }

            ignoreOptionMarker.done(XQueryElementType.FT_IGNORE_OPTION)
            return true
        }
        return false
    }

    private fun parseStringConcatExpr(type: IElementType?): Boolean {
        val stringConcatExprMarker = mark()
        if (parseRangeExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            var haveStringConcatExpr = false
            while (builder.matchTokenType(XPathTokenType.CONCATENATION)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseRangeExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "RangeExpr"))
                }
                parseWhiteSpaceAndCommentTokens(builder)
                haveStringConcatExpr = true
            }

            if (haveStringConcatExpr)
                stringConcatExprMarker.done(XPathElementType.STRING_CONCAT_EXPR)
            else
                stringConcatExprMarker.drop()
            return true
        }
        stringConcatExprMarker.drop()
        return false
    }

    private fun parseRangeExpr(type: IElementType?): Boolean {
        val rangeExprMarker = mark()
        if (parseAdditiveExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_TO)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseAdditiveExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "AdditiveExpr"))
                }
                rangeExprMarker.done(XPathElementType.RANGE_EXPR)
            } else {
                rangeExprMarker.drop()
            }
            return true
        }
        rangeExprMarker.drop()
        return false
    }

    private fun parseAdditiveExpr(type: IElementType?): Boolean {
        val additiveExprMarker = mark()
        if (parseMultiplicativeExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            var haveAdditativeExpr = false
            while (builder.matchTokenType(XPathTokenType.PLUS) || builder.matchTokenType(XPathTokenType.MINUS)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseMultiplicativeExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "MultiplicativeExpr"))
                }
                haveAdditativeExpr = true
            }

            if (haveAdditativeExpr)
                additiveExprMarker.done(XPathElementType.ADDITIVE_EXPR)
            else
                additiveExprMarker.drop()
            return true
        }
        additiveExprMarker.drop()
        return false
    }

    private fun parseMultiplicativeExpr(type: IElementType?): Boolean {
        val multiplicativeExprMarker = mark()
        if (parseUnionExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            var haveMultiplicativeExpr = false
            while (
                builder.matchTokenType(XPathTokenType.STAR) ||
                builder.matchTokenType(XPathTokenType.K_DIV) ||
                builder.matchTokenType(XPathTokenType.K_IDIV) ||
                builder.matchTokenType(XPathTokenType.K_MOD)
            ) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseUnionExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "UnionExpr"))
                }
                haveMultiplicativeExpr = true
            }

            if (haveMultiplicativeExpr)
                multiplicativeExprMarker.done(XPathElementType.MULTIPLICATIVE_EXPR)
            else
                multiplicativeExprMarker.drop()
            return true
        }
        multiplicativeExprMarker.drop()
        return false
    }

    private fun parseUnionExpr(type: IElementType?): Boolean {
        val unionExprMarker = mark()
        if (parseIntersectExceptExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            var haveUnionExpr = false
            while (builder.matchTokenType(XPathTokenType.K_UNION) || builder.matchTokenType(XPathTokenType.UNION)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseIntersectExceptExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "IntersectExceptExpr"))
                }
                haveUnionExpr = true
            }

            if (haveUnionExpr)
                unionExprMarker.done(XPathElementType.UNION_EXPR)
            else
                unionExprMarker.drop()
            return true
        }
        unionExprMarker.drop()
        return false
    }

    private fun parseIntersectExceptExpr(type: IElementType?): Boolean {
        val intersectExceptExprMarker = mark()
        if (parseInstanceofExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            var haveIntersectExceptExpr = false
            while (builder.matchTokenType(XPathTokenType.K_INTERSECT) || builder.matchTokenType(XPathTokenType.K_EXCEPT)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseInstanceofExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "InstanceofExpr"))
                }
                parseWhiteSpaceAndCommentTokens(builder)
                haveIntersectExceptExpr = true
            }

            if (haveIntersectExceptExpr)
                intersectExceptExprMarker.done(XPathElementType.INTERSECT_EXCEPT_EXPR)
            else
                intersectExceptExprMarker.drop()
            return true
        }
        intersectExceptExprMarker.drop()
        return false
    }

    private fun parseInstanceofExpr(type: IElementType?): Boolean {
        val instanceofExprMarker = mark()
        if (parseTreatExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_INSTANCE)) {
                var haveErrors = false

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.K_OF)) {
                    haveErrors = true
                    error(XQueryBundle.message("parser.error.expected-keyword", "of"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSequenceType() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "SequenceType"))
                }
                instanceofExprMarker.done(XPathElementType.INSTANCEOF_EXPR)
            } else if (getTokenType() === XPathTokenType.K_OF) {
                error(XQueryBundle.message("parser.error.expected-keyword", "instance"))
                advanceLexer()

                parseWhiteSpaceAndCommentTokens(builder)
                parseSingleType()
                instanceofExprMarker.done(XPathElementType.INSTANCEOF_EXPR)
            } else {
                instanceofExprMarker.drop()
            }
            return true
        }
        instanceofExprMarker.drop()
        return false
    }

    private fun parseTreatExpr(type: IElementType?): Boolean {
        val treatExprMarker = mark()
        if (parseCastableExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_TREAT)) {
                var haveErrors = false

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.K_AS)) {
                    haveErrors = true
                    error(XQueryBundle.message("parser.error.expected-keyword", "as"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSequenceType() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "SequenceType"))
                }
                treatExprMarker.done(XPathElementType.TREAT_EXPR)
            } else if (getTokenType() === XPathTokenType.K_AS && type !== XQueryElementType.SOURCE_EXPR && type !== XQueryElementType.TARGET_EXPR) {
                error(XQueryBundle.message("parser.error.expected-keyword", "cast, castable, treat"))
                advanceLexer()

                parseWhiteSpaceAndCommentTokens(builder)
                parseSingleType()
                treatExprMarker.done(XPathElementType.TREAT_EXPR)
            } else {
                treatExprMarker.drop()
            }
            return true
        }
        treatExprMarker.drop()
        return false
    }

    private fun parseCastableExpr(type: IElementType?): Boolean {
        val castableExprMarker = mark()
        if (parseCastExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_CASTABLE)) {
                var haveErrors = false

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.K_AS)) {
                    haveErrors = true
                    error(XQueryBundle.message("parser.error.expected-keyword", "as"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSingleType() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "SingleType"))
                }
                castableExprMarker.done(XPathElementType.CASTABLE_EXPR)
            } else {
                castableExprMarker.drop()
            }
            return true
        }
        castableExprMarker.drop()
        return false
    }

    private fun parseCastExpr(type: IElementType?): Boolean {
        val castExprMarker = mark()
        if (parseTransformWithExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_CAST)) {
                var haveErrors = false

                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.K_AS)) {
                    haveErrors = true
                    error(XQueryBundle.message("parser.error.expected-keyword", "as"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSingleType() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "SingleType"))
                }
                castExprMarker.done(XPathElementType.CAST_EXPR)
            } else {
                castExprMarker.drop()
            }
            return true
        }
        castExprMarker.drop()
        return false
    }

    private fun parseTransformWithExpr(type: IElementType?): Boolean {
        val exprMarker = mark()
        if (parseArrowExpr(type)) {
            if (builder.matchTokenType(XQueryTokenType.K_TRANSFORM)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XQueryTokenType.K_WITH)) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "with"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                parseEnclosedExprOrBlock(null, BlockOpen.OPTIONAL, BlockExpr.OPTIONAL)

                exprMarker.done(XQueryElementType.TRANSFORM_WITH_EXPR)
            } else {
                exprMarker.drop()
            }
            return true
        }
        exprMarker.drop()
        return false
    }

    private fun parseArrowExpr(type: IElementType?): Boolean {
        val exprMarker = mark()
        if (parseUnaryExpr(type)) {
            var haveErrors = false
            var haveArrowExpr = false

            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.ARROW)) {
                haveArrowExpr = true

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseArrowFunctionSpecifier() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "ArrowFunctionSpecifier"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseArgumentList() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "ArgumentList"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
            }

            if (haveArrowExpr)
                exprMarker.done(XPathElementType.ARROW_EXPR)
            else
                exprMarker.drop()
            return true
        }
        exprMarker.drop()
        return false
    }

    private fun parseArrowFunctionSpecifier(): Boolean {
        val arrowFunctionSpecifierMarker = mark()
        if (parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false) || parseVarRef(null) || parseParenthesizedExpr()) {
            arrowFunctionSpecifierMarker.done(XPathElementType.ARROW_FUNCTION_SPECIFIER)
            return true
        }
        arrowFunctionSpecifierMarker.drop()
        return false
    }

    private fun parseUnaryExpr(type: IElementType?): Boolean {
        val pathExprMarker = mark()
        var matched = false
        while (builder.matchTokenType(XPathTokenType.PLUS) || builder.matchTokenType(XPathTokenType.MINUS)) {
            parseWhiteSpaceAndCommentTokens(builder)
            matched = true
        }
        if (matched) {
            if (parseValueExpr(null)) {
                pathExprMarker.done(XPathElementType.UNARY_EXPR)
                return true
            } else if (matched) {
                error(XPathBundle.message("parser.error.expected", "ValueExpr"))
                pathExprMarker.done(XPathElementType.UNARY_EXPR)
                return true
            }
        } else if (parseValueExpr(type)) {
            pathExprMarker.drop()
            return true
        }
        pathExprMarker.drop()
        return false
    }

    private fun parseGeneralComp(): Boolean {
        return builder.matchTokenType(XPathTokenType.EQUAL) ||
                builder.matchTokenType(XPathTokenType.NOT_EQUAL) ||
                builder.matchTokenType(XPathTokenType.LESS_THAN) ||
                builder.matchTokenType(XPathTokenType.LESS_THAN_OR_EQUAL) ||
                builder.matchTokenType(XPathTokenType.GREATER_THAN) ||
                builder.matchTokenType(XPathTokenType.GREATER_THAN_OR_EQUAL)
    }

    private fun parseValueComp(): Boolean {
        return builder.matchTokenType(XPathTokenType.K_EQ) ||
                builder.matchTokenType(XPathTokenType.K_NE) ||
                builder.matchTokenType(XPathTokenType.K_LT) ||
                builder.matchTokenType(XPathTokenType.K_LE) ||
                builder.matchTokenType(XPathTokenType.K_GT) ||
                builder.matchTokenType(XPathTokenType.K_GE)
    }

    private fun parseNodeComp(): Boolean {
        return builder.matchTokenType(XPathTokenType.K_IS) ||
                builder.matchTokenType(XPathTokenType.NODE_BEFORE) ||
                builder.matchTokenType(XPathTokenType.NODE_AFTER)
    }

    private fun parseSingleType(): Boolean {
        val singleTypeMarker = mark()
        if (parseEQNameOrWildcard(builder, XPathElementType.SIMPLE_TYPE_NAME, false)) {
            parseWhiteSpaceAndCommentTokens(builder)
            builder.matchTokenType(XPathTokenType.OPTIONAL)

            singleTypeMarker.done(XPathElementType.SINGLE_TYPE)
            return true
        }
        singleTypeMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: ValueExpr

    private fun parseValueExpr(type: IElementType?): Boolean {
        return parseExtensionExpr() || parseValidateExpr() || parseSimpleMapExpr(type)
    }

    private fun parseValidateExpr(): Boolean {
        val validateExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_VALIDATE)
        if (validateExprMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            var blockOpen = BlockOpen.REQUIRED
            if (builder.matchTokenType(XQueryTokenType.K_LAX) || builder.matchTokenType(XQueryTokenType.K_STRICT)) {
                blockOpen = BlockOpen.OPTIONAL
            } else if (builder.matchTokenType(XPathTokenType.K_AS) || builder.matchTokenType(XQueryTokenType.K_TYPE)) {
                blockOpen = BlockOpen.OPTIONAL

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseEQNameOrWildcard(builder, XPathElementType.TYPE_NAME, false)) {
                    error(XPathBundle.message("parser.error.expected", "TypeName"))
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(null, blockOpen, BlockExpr.REQUIRED)) {
                validateExprMarker.rollbackTo()
                return false
            }

            validateExprMarker.done(XQueryElementType.VALIDATE_EXPR)
            return true
        }
        return false
    }

    private fun parseExtensionExpr(): Boolean {
        val extensionExprMarker = mark()
        var matched = false
        while (parsePragma()) {
            matched = true
            parseWhiteSpaceAndCommentTokens(builder)
        }
        if (matched) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseEnclosedExprOrBlock(null, BlockOpen.OPTIONAL, BlockExpr.OPTIONAL)
            extensionExprMarker.done(XQueryElementType.EXTENSION_EXPR)
            return true
        }
        extensionExprMarker.drop()
        return false
    }

    private fun parsePragma(): Boolean {
        val pragmaMarker = matchTokenTypeWithMarker(XQueryTokenType.PRAGMA_BEGIN)
        if (pragmaMarker != null) {
            var haveErrors = false

            builder.matchTokenType(XPathTokenType.WHITE_SPACE)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            // NOTE: The XQuery grammar requires pragma contents if the EQName
            // is followed by a space token, but implementations make it optional.
            builder.matchTokenType(XPathTokenType.WHITE_SPACE)
            builder.matchTokenType(XQueryTokenType.PRAGMA_CONTENTS)

            if (!builder.matchTokenType(XQueryTokenType.PRAGMA_END) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "#)"))
            }

            pragmaMarker.done(XQueryElementType.PRAGMA)
            return true
        }
        return false
    }

    private fun parseSimpleMapExpr(type: IElementType?): Boolean {
        val simpleMapExprMarker = mark()
        if (parsePathExpr(type)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            var haveSimpleMapExpr = false
            while (builder.matchTokenType(XPathTokenType.MAP_OPERATOR)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parsePathExpr(null) && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "PathExpr"))
                    haveErrors = true
                }
                parseWhiteSpaceAndCommentTokens(builder)
                haveSimpleMapExpr = true
            }

            if (haveSimpleMapExpr)
                simpleMapExprMarker.done(XPathElementType.SIMPLE_MAP_EXPR)
            else
                simpleMapExprMarker.drop()
            return true
        }
        simpleMapExprMarker.drop()
        return false
    }

    private fun parsePathExpr(type: IElementType?): Boolean {
        val pathExprMarker = mark()
        if (builder.matchTokenType(XPathTokenType.DIRECT_DESCENDANTS_PATH)) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseRelativePathExpr(null)

            pathExprMarker.done(XPathElementType.PATH_EXPR)
            return true
        } else if (builder.matchTokenType(XPathTokenType.ALL_DESCENDANTS_PATH)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseRelativePathExpr(null)) {
                error(XPathBundle.message("parser.error.expected", "RelativePathExpr"))
            }

            pathExprMarker.done(XPathElementType.PATH_EXPR)
            return true
        } else if (parseRelativePathExpr(type)) {
            pathExprMarker.drop()
            return true
        }
        pathExprMarker.drop()
        return false
    }

    private fun parseRelativePathExpr(type: IElementType?): Boolean {
        val relativePathExprMarker = mark()
        if (parseStepExpr(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            var haveRelativePathExpr = false
            while (builder.matchTokenType(XPathTokenType.DIRECT_DESCENDANTS_PATH) || builder.matchTokenType(XPathTokenType.ALL_DESCENDANTS_PATH)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseStepExpr(null)) {
                    error(XPathBundle.message("parser.error.expected", "StepExpr"))
                }

                parseWhiteSpaceAndCommentTokens(builder)
                haveRelativePathExpr = true
            }

            if (haveRelativePathExpr)
                relativePathExprMarker.done(XPathElementType.RELATIVE_PATH_EXPR)
            else
                relativePathExprMarker.drop()
            return true
        }
        relativePathExprMarker.drop()
        return false
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: StepExpr

    private fun parseStepExpr(type: IElementType?): Boolean {
        return parsePostfixExpr(type) || parseAxisStep(type)
    }

    private fun parseAxisStep(type: IElementType?): Boolean {
        val axisStepMarker = mark()
        if (parseReverseStep() || parseForwardStep(type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parsePredicateList())
                axisStepMarker.done(XPathElementType.AXIS_STEP)
            else
                axisStepMarker.drop()
            return true
        }

        axisStepMarker.drop()
        return false
    }

    private fun parseForwardStep(type: IElementType?): Boolean {
        val forwardStepMarker = mark()
        if (parseForwardAxis()) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseNodeTest(builder, null)) {
                error(XPathBundle.message("parser.error.expected", "NodeTest"))
            }

            forwardStepMarker.done(XPathElementType.FORWARD_STEP)
            return true
        } else if (parseAbbrevForwardStep(type)) {
            forwardStepMarker.drop()
            return true
        }

        forwardStepMarker.drop()
        return false
    }

    private fun parseForwardAxis(): Boolean {
        val forwardAxisMarker = mark()
        if (
            builder.matchTokenType(XPathTokenType.K_ATTRIBUTE) ||
            builder.matchTokenType(XPathTokenType.K_CHILD) ||
            builder.matchTokenType(XPathTokenType.K_DESCENDANT) ||
            builder.matchTokenType(XPathTokenType.K_DESCENDANT_OR_SELF) ||
            builder.matchTokenType(XPathTokenType.K_FOLLOWING) ||
            builder.matchTokenType(XPathTokenType.K_FOLLOWING_SIBLING) ||
            builder.matchTokenType(XPathTokenType.K_NAMESPACE) ||
            builder.matchTokenType(XPathTokenType.K_PROPERTY) ||
            builder.matchTokenType(XPathTokenType.K_SELF)
        ) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.AXIS_SEPARATOR)) {
                forwardAxisMarker.rollbackTo()
                return false
            }

            forwardAxisMarker.done(XPathElementType.FORWARD_AXIS)
            return true
        }
        forwardAxisMarker.drop()
        return false
    }

    private fun parseAbbrevForwardStep(type: IElementType?): Boolean {
        val abbrevForwardStepMarker = mark()
        val matched = builder.matchTokenType(XPathTokenType.ATTRIBUTE_SELECTOR)

        parseWhiteSpaceAndCommentTokens(builder)
        if (parseNodeTest(builder, type)) {
            if (matched)
                abbrevForwardStepMarker.done(XPathElementType.ABBREV_FORWARD_STEP)
            else
                abbrevForwardStepMarker.drop()
            return true
        } else if (matched) {
            error(XPathBundle.message("parser.error.expected", "NodeTest"))

            abbrevForwardStepMarker.done(XPathElementType.ABBREV_FORWARD_STEP)
            return true
        }
        abbrevForwardStepMarker.drop()
        return false
    }

    private fun parseReverseStep(): Boolean {
        val reverseStepMarker = mark()
        if (parseReverseAxis()) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseNodeTest(builder, null)) {
                error(XPathBundle.message("parser.error.expected", "NodeTest"))
            }

            reverseStepMarker.done(XPathElementType.REVERSE_STEP)
            return true
        } else if (parseAbbrevReverseStep()) {
            reverseStepMarker.drop()
            return true
        }

        reverseStepMarker.drop()
        return false
    }

    private fun parseReverseAxis(): Boolean {
        val reverseAxisMarker = mark()
        if (
            builder.matchTokenType(XPathTokenType.K_PARENT) ||
            builder.matchTokenType(XPathTokenType.K_ANCESTOR) ||
            builder.matchTokenType(XPathTokenType.K_ANCESTOR_OR_SELF) ||
            builder.matchTokenType(XPathTokenType.K_PRECEDING) ||
            builder.matchTokenType(XPathTokenType.K_PRECEDING_SIBLING)
        ) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.AXIS_SEPARATOR)) {
                reverseAxisMarker.rollbackTo()
                return false
            }

            reverseAxisMarker.done(XPathElementType.REVERSE_AXIS)
            return true
        }
        reverseAxisMarker.drop()
        return false
    }

    private fun parseAbbrevReverseStep(): Boolean {
        val abbrevReverseStepMarker = matchTokenTypeWithMarker(XPathTokenType.PARENT_SELECTOR)
        if (abbrevReverseStepMarker != null) {
            abbrevReverseStepMarker.done(XPathElementType.ABBREV_REVERSE_STEP)
            return true
        }
        return false
    }

    private fun parsePostfixExpr(type: IElementType?): Boolean {
        val postfixExprMarker = mark()
        if (parsePrimaryExpr(builder, type)) {
            parseWhiteSpaceAndCommentTokens(builder)
            var havePostfixExpr = false
            while (parsePredicate() || parseArgumentList() || parseLookup(XPathElementType.LOOKUP)) {
                parseWhiteSpaceAndCommentTokens(builder)
                havePostfixExpr = true
            }

            if (havePostfixExpr)
                postfixExprMarker.done(XPathElementType.POSTFIX_EXPR)
            else
                postfixExprMarker.drop()
            return true
        }
        postfixExprMarker.drop()
        return false
    }

    private fun parsePredicateList(): Boolean {
        val predicateListMarker = mark()
        var havePredicate = false
        while (parsePredicate()) {
            parseWhiteSpaceAndCommentTokens(builder)
            havePredicate = true
        }
        if (havePredicate)
            predicateListMarker.done(XPathElementType.PREDICATE_LIST)
        else
            predicateListMarker.drop()
        return havePredicate
    }

    private fun parsePredicate(): Boolean {
        val predicateMarker = matchTokenTypeWithMarker(XPathTokenType.SQUARE_OPEN)
        if (predicateMarker != null) {
            var haveErrors = false
            parseWhiteSpaceAndCommentTokens(builder)

            if (!parseExpr(XQueryElementType.EXPR)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.SQUARE_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "]"))
            }

            predicateMarker.done(XPathElementType.PREDICATE)
            return true
        }

        return false
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: PrimaryExpr

    @Suppress("Reformat") // Kotlin formatter bug: https://youtrack.jetbrains.com/issue/KT-22518
    override fun parsePrimaryExpr(builder: PsiBuilder, type: IElementType?): Boolean {
        return (
            parseLiteral(builder) ||
            parseVarRef(type) ||
            parseParenthesizedExpr() ||
            parseNonDeterministicFunctionCall() ||
            parseContextItemExpr() ||
            parseOrderedExpr() ||
            parseUnorderedExpr() ||
            parseFunctionItemExpr() ||
            parseArrayConstructor() ||
            parseBinaryConstructor() ||
            parseBooleanConstructor() ||
            parseMapConstructor() ||
            parseNodeConstructor() ||
            parseNullConstructor() ||
            parseNumberConstructor() ||
            parseStringConstructor() ||
            parseLookup(XPathElementType.UNARY_LOOKUP) ||
            parseFunctionCall()
        )
    }

    private fun parseVarRef(type: IElementType?): Boolean {
        val varRefMarker = matchTokenTypeWithMarker(XPathTokenType.VARIABLE_INDICATOR)
        if (varRefMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XPathElementType.VAR_NAME, type === XPathElementType.MAP_CONSTRUCTOR_ENTRY)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
            }

            varRefMarker.done(XPathElementType.VAR_REF)
            return true
        }
        return false
    }

    private fun parseParenthesizedExpr(): Boolean {
        val parenthesizedExprMarker = matchTokenTypeWithMarker(XPathTokenType.PARENTHESIS_OPEN)
        if (parenthesizedExprMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseExpr(XQueryElementType.EXPR)) {
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            parenthesizedExprMarker.done(XPathElementType.PARENTHESIZED_EXPR)
            return true
        }
        return false
    }

    private fun parseContextItemExpr(): Boolean {
        val contextItemExprMarker = matchTokenTypeWithMarker(XPathTokenType.DOT)
        if (contextItemExprMarker != null) {
            contextItemExprMarker.done(XPathElementType.CONTEXT_ITEM_EXPR)
            return true
        }
        return false
    }

    private fun parseOrderedExpr(): Boolean {
        val orderedExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_ORDERED)
        if (orderedExprMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                orderedExprMarker.rollbackTo()
                return false
            }

            orderedExprMarker.done(XQueryElementType.ORDERED_EXPR)
            return true
        }
        return false
    }

    private fun parseUnorderedExpr(): Boolean {
        val unorderedExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_UNORDERED)
        if (unorderedExprMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                unorderedExprMarker.rollbackTo()
                return false
            }

            unorderedExprMarker.done(XQueryElementType.UNORDERED_EXPR)
            return true
        }
        return false
    }

    private fun parseNonDeterministicFunctionCall(): Boolean {
        val nonDeterministicFunctionCallMarker = matchTokenTypeWithMarker(XQueryTokenType.K_NON_DETERMINISTIC);
        if (nonDeterministicFunctionCallMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (getTokenType() != XPathTokenType.VARIABLE_INDICATOR) {
                nonDeterministicFunctionCallMarker.rollbackTo()
                return false
            }

            if (!parseVarRef(null)) {
                error(XPathBundle.message("parser.error.expected", "VarDecl"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseArgumentList()) {
                error(XPathBundle.message("parser.error.expected", "ArgumentList"))
            }

            nonDeterministicFunctionCallMarker.done(XQueryElementType.NON_DETERMINISTIC_FUNCTION_CALL)
            return true
        }
        return false
    }

    private fun parseFunctionCall(): Boolean {
        if (getTokenType() is IKeywordOrNCNameType) {
            val type = getTokenType() as IKeywordOrNCNameType?
            when (type!!.keywordType) {
                IKeywordOrNCNameType.KeywordType.KEYWORD, IKeywordOrNCNameType.KeywordType.SCRIPTING10_RESERVED_FUNCTION_NAME -> {
                }
                IKeywordOrNCNameType.KeywordType.RESERVED_FUNCTION_NAME, IKeywordOrNCNameType.KeywordType.XQUERY30_RESERVED_FUNCTION_NAME -> return false
                IKeywordOrNCNameType.KeywordType.MARKLOGIC70_RESERVED_FUNCTION_NAME, IKeywordOrNCNameType.KeywordType.MARKLOGIC80_RESERVED_FUNCTION_NAME -> {
                    // Don't keep the MarkLogic Schema/JSON parseTree here as KindTest is not anchored to the correct parent
                    // at this point.
                    val testMarker = mark()
                    var status = parseSchemaKindTest()
                    if (status == ParseStatus.NOT_MATCHED) {
                        status = parseJsonKindTest()
                    }
                    testMarker.rollbackTo()

                    // If this is a valid MarkLogic Schema/JSON KindTest, return false here to parse it as a KindTest.
                    if (status == ParseStatus.MATCHED) {
                        return false
                    }
                }
            }
            // Otherwise, fall through to the FunctionCall parser to parse it as a FunctionCall to allow
            // standard XQuery to use these keywords as function names.
        }

        val functionCallMarker = mark()
        if (parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseArgumentList()) {
                functionCallMarker.rollbackTo()
                return false
            }

            functionCallMarker.done(XPathElementType.FUNCTION_CALL)
            return true
        }

        functionCallMarker.drop()
        return false
    }

    private fun parseArgumentList(): Boolean {
        val argumentListMarker = matchTokenTypeWithMarker(XPathTokenType.PARENTHESIS_OPEN)
        if (argumentListMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseArgument()) {
                parseWhiteSpaceAndCommentTokens(builder)
                while (builder.matchTokenType(XPathTokenType.COMMA)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseArgument() && !haveErrors) {
                        error(XQueryBundle.message("parser.error.expected-either", "ExprSingle", "?"))
                        haveErrors = true
                    }

                    parseWhiteSpaceAndCommentTokens(builder)
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            argumentListMarker.done(XPathElementType.ARGUMENT_LIST)
            return true
        }
        return false
    }

    private fun parseArgument(): Boolean {
        val argumentMarker = mark()
        if (parseExprSingle(builder) || parseArgumentPlaceholder()) {
            argumentMarker.done(XPathElementType.ARGUMENT)
            return true
        }
        argumentMarker.drop()
        return false
    }

    private fun parseArgumentPlaceholder(): Boolean {
        val argumentPlaceholderMarker = matchTokenTypeWithMarker(XPathTokenType.OPTIONAL)
        if (argumentPlaceholderMarker != null) {
            argumentPlaceholderMarker.done(XPathElementType.ARGUMENT_PLACEHOLDER)
            return true
        }
        return false
    }

    private fun parseFunctionItemExpr(): Boolean {
        return parseNamedFunctionRef() || parseInlineFunctionExpr() || parseSimpleInlineFunctionExpr()
    }

    private fun parseNamedFunctionRef(): Boolean {
        val namedFunctionRefMarker = mark()
        if (parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.FUNCTION_REF_OPERATOR)) {
                namedFunctionRefMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.INTEGER_LITERAL)) {
                error(XPathBundle.message("parser.error.expected", "IntegerLiteral"))
            }

            namedFunctionRefMarker.done(XPathElementType.NAMED_FUNCTION_REF)
            return true
        }

        namedFunctionRefMarker.drop()
        return false
    }

    private fun parseInlineFunctionExpr(): Boolean {
        val inlineFunctionExprMarker = mark()

        var haveAnnotations = false
        while (parseAnnotation()) {
            parseWhiteSpaceAndCommentTokens(builder)
            haveAnnotations = true
        }

        if (builder.matchTokenType(XPathTokenType.K_FUNCTION)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                if (!haveAnnotations) {
                    inlineFunctionExprMarker.rollbackTo()
                    return false
                }

                error(XPathBundle.message("parser.error.expected", "("))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseParamList()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.K_AS)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSequenceType()) {
                    error(XPathBundle.message("parser.error.expected", "SequenceType"))
                    haveErrors = true
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.FUNCTION_BODY, BlockOpen.REQUIRED, BlockExpr.OPTIONAL) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "{"))
                parseExpr(XQueryElementType.EXPR)

                parseWhiteSpaceAndCommentTokens(builder)
                builder.matchTokenType(XPathTokenType.BLOCK_CLOSE)
            }

            inlineFunctionExprMarker.done(XPathElementType.INLINE_FUNCTION_EXPR)
            return true
        } else if (haveAnnotations) {
            error(XQueryBundle.message("parser.error.expected-keyword", "function"))

            inlineFunctionExprMarker.done(XPathElementType.INLINE_FUNCTION_EXPR)
            return true
        }

        inlineFunctionExprMarker.drop()
        return false
    }

    private fun parseSimpleInlineFunctionExpr(): Boolean {
        val inlineFunctionExprMarker = matchTokenTypeWithMarker(XQueryTokenType.K_FN)
        if (inlineFunctionExprMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(null, BlockOpen.REQUIRED, BlockExpr.REQUIRED)) {
                inlineFunctionExprMarker.rollbackTo()
                return false
            }

            inlineFunctionExprMarker.done(XQueryElementType.SIMPLE_INLINE_FUNCTION_EXPR)
            return true
        }
        return false
    }

    private fun parseLookup(type: IElementType): Boolean {
        val lookupMarker = matchTokenTypeWithMarker(XPathTokenType.OPTIONAL)
        if (lookupMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseKeySpecifier()) {
                if (type === XPathElementType.UNARY_LOOKUP) {
                    // NOTE: This conflicts with '?' used as an ArgumentPlaceholder, so don't match '?' only as UnaryLookup.
                    lookupMarker.rollbackTo()
                    return false
                } else {
                    error(XPathBundle.message("parser.error.expected", "KeySpecifier"))
                }
            }

            lookupMarker.done(type)
            return true
        }
        return false
    }

    private fun parseKeySpecifier(): Boolean {
        val keySpecifierMarker = mark()
        if (
            builder.matchTokenType(XPathTokenType.STAR) ||
            builder.matchTokenType(XPathTokenType.INTEGER_LITERAL) ||
            parseEQNameOrWildcard(builder, XQueryElementType.NCNAME, false) ||
            parseParenthesizedExpr()
        ) {
            keySpecifierMarker.done(XPathElementType.KEY_SPECIFIER)
            return true
        }
        keySpecifierMarker.drop()
        return false
    }

    private fun parseStringConstructor(): Boolean {
        val stringConstructorMarker = matchTokenTypeWithMarker(XQueryTokenType.STRING_CONSTRUCTOR_START)
        if (stringConstructorMarker != null) {
            parseStringConstructorContent()

            if (!builder.matchTokenType(XQueryTokenType.STRING_CONSTRUCTOR_END)) {
                error(XQueryBundle.message("parser.error.incomplete-string-constructor"))
            }

            stringConstructorMarker.done(XQueryElementType.STRING_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseStringConstructorContent(): Boolean {
        val stringConstructorContentMarker = mark()
        if (builder.matchTokenType(XQueryTokenType.STRING_CONSTRUCTOR_CONTENTS)) {
            while (builder.matchTokenType(XQueryTokenType.STRING_CONSTRUCTOR_CONTENTS) || parseStringConstructorInterpolation()) {
                //
            }
        }
        stringConstructorContentMarker.done(XQueryElementType.STRING_CONSTRUCTOR_CONTENT)
        return true
    }

    private fun parseStringConstructorInterpolation(): Boolean {
        val stringConstructorInterpolationMarker = matchTokenTypeWithMarker(XQueryTokenType.STRING_INTERPOLATION_OPEN)
        if (stringConstructorInterpolationMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseExpr(XQueryElementType.EXPR)

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.STRING_INTERPOLATION_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", "}`"))
            }

            stringConstructorInterpolationMarker.done(XQueryElementType.STRING_CONSTRUCTOR_INTERPOLATION)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: PrimaryExpr :: Constructors

    private fun parseArrayConstructor(): Boolean {
        return parseSquareArrayConstructor() || parseCurlyArrayConstructor()
    }

    private fun parseSquareArrayConstructor(): Boolean {
        val arrayConstructor = matchTokenTypeWithMarker(XPathTokenType.SQUARE_OPEN)
        if (arrayConstructor != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseExprSingle(builder)) {
                parseWhiteSpaceAndCommentTokens(builder)
                while (builder.matchTokenType(XPathTokenType.COMMA)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseExprSingle(builder) && !haveErrors) {
                        error(XPathBundle.message("parser.error.expected-expression"))
                        haveErrors = true
                    }

                    parseWhiteSpaceAndCommentTokens(builder)
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.SQUARE_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "]"))
            }

            arrayConstructor.done(XPathElementType.SQUARE_ARRAY_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseCurlyArrayConstructor(): Boolean {
        var arrayConstructor = matchTokenTypeWithMarker(XPathTokenType.K_ARRAY)
        if (arrayConstructor == null) {
            arrayConstructor = matchTokenTypeWithMarker(XPathTokenType.K_ARRAY_NODE)
        }

        if (arrayConstructor != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                arrayConstructor.rollbackTo()
                return false
            }
            arrayConstructor.done(XPathElementType.CURLY_ARRAY_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseBinaryConstructor(): Boolean {
        val binaryConstructor = matchTokenTypeWithMarker(XQueryTokenType.K_BINARY)
        if (binaryConstructor != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(null, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                binaryConstructor.rollbackTo()
                return false
            }
            binaryConstructor.done(XQueryElementType.BINARY_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseBooleanConstructor(): Boolean {
        val booleanConstructor = matchTokenTypeWithMarker(XPathTokenType.K_BOOLEAN_NODE)
        if (booleanConstructor != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(null, BlockOpen.REQUIRED, BlockExpr.REQUIRED)) {
                booleanConstructor.rollbackTo()
                return false
            }
            booleanConstructor.done(XQueryElementType.BOOLEAN_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseMapConstructor(): Boolean {
        var mapConstructor = matchTokenTypeWithMarker(XPathTokenType.K_MAP)
        if (mapConstructor == null) {
            mapConstructor = matchTokenTypeWithMarker(XPathTokenType.K_OBJECT_NODE)
        }

        if (mapConstructor != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.BLOCK_OPEN)) {
                mapConstructor.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseMapConstructorEntry()) {
                parseWhiteSpaceAndCommentTokens(builder)
                while (builder.matchTokenType(XPathTokenType.COMMA)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseMapConstructorEntry() && !haveErrors) {
                        error(XPathBundle.message("parser.error.expected", "MapConstructor"))
                        haveErrors = true
                    }
                    parseWhiteSpaceAndCommentTokens(builder)
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.BLOCK_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "}"))
            }

            mapConstructor.done(XPathElementType.MAP_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseMapConstructorEntry(): Boolean {
        val mapConstructorEntry = mark()
        if (parseExprSingle(XPathElementType.MAP_KEY_EXPR, XPathElementType.MAP_CONSTRUCTOR_ENTRY)) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.QNAME_SEPARATOR) && !builder.matchTokenType(XPathTokenType.ASSIGN_EQUAL)) {
                error(XQueryBundle.message("parser.error.expected-map-entry-assign"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExprSingle(XPathElementType.MAP_VALUE_EXPR) && !haveError) {
                error(XPathBundle.message("parser.error.expected-expression"))
            }

            mapConstructorEntry.done(XPathElementType.MAP_CONSTRUCTOR_ENTRY)
            return true
        }
        mapConstructorEntry.drop()
        return false
    }

    private fun parseNodeConstructor(): Boolean {
        val constructorMarker = mark()
        if (parseDirectConstructor(0) || parseComputedConstructor()) {
            constructorMarker.done(XQueryElementType.NODE_CONSTRUCTOR)
            return true
        }

        constructorMarker.drop()
        return false
    }

    private fun parseNullConstructor(): Boolean {
        val nullConstructor = matchTokenTypeWithMarker(XPathTokenType.K_NULL_NODE)
        if (nullConstructor != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.BLOCK_OPEN)) {
                nullConstructor.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.BLOCK_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", "}"))
            }

            nullConstructor.done(XQueryElementType.NULL_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseNumberConstructor(): Boolean {
        val numberConstructor = matchTokenTypeWithMarker(XPathTokenType.K_NUMBER_NODE)
        if (numberConstructor != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(null, BlockOpen.REQUIRED, BlockExpr.REQUIRED)) {
                numberConstructor.rollbackTo()
                return false
            }
            numberConstructor.done(XQueryElementType.NUMBER_CONSTRUCTOR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: PrimaryExpr :: NodeConstructor :: DirectConstructor

    private fun parseDirectConstructor(depth: Int): Boolean {
        return (parseDirElemConstructor(depth)
                || parseDirCommentConstructor()
                || parseDirPIConstructor()
                || parseCDataSection(null))
    }

    private fun parseDirElemConstructor(depth: Int): Boolean {
        val elementMarker = mark()
        if (builder.matchTokenType(XQueryTokenType.OPEN_XML_TAG)) {
            errorOnTokenType(XQueryTokenType.XML_WHITE_SPACE, XQueryBundle.message("parser.error.unexpected-whitespace"))
            parseQNameOrWildcard(builder, XQueryElementType.QNAME, false)

            parseDirAttributeList()

            if (builder.matchTokenType(XQueryTokenType.SELF_CLOSING_XML_TAG)) {
                //
            } else if (builder.matchTokenType(XQueryTokenType.END_XML_TAG)) {
                parseDirElemContent(depth + 1)

                if (builder.matchTokenType(XQueryTokenType.CLOSE_XML_TAG)) {
                    // NOTE: The XQueryLexer ensures that CLOSE_XML_TAG is followed by an NCNAME/QNAME.
                    parseQNameOrWildcard(builder, XQueryElementType.QNAME, false)

                    builder.matchTokenType(XQueryTokenType.XML_WHITE_SPACE)
                    if (!builder.matchTokenType(XQueryTokenType.END_XML_TAG)) {
                        error(XPathBundle.message("parser.error.expected", ">"))
                    }
                } else {
                    error(XQueryBundle.message("parser.error.expected-closing-tag"))
                }
            } else {
                error(XQueryBundle.message("parser.error.incomplete-open-tag"))
            }

            elementMarker.done(XQueryElementType.DIR_ELEM_CONSTRUCTOR)
            return true
        } else if (depth == 0 && getTokenType() === XQueryTokenType.CLOSE_XML_TAG) {
            error(XQueryBundle.message("parser.error.unexpected-closing-tag"))
            builder.matchTokenType(XQueryTokenType.CLOSE_XML_TAG)
            parseQNameOrWildcard(builder, XQueryElementType.QNAME, false)
            builder.matchTokenType(XPathTokenType.WHITE_SPACE)
            builder.matchTokenType(XPathTokenType.GREATER_THAN)

            elementMarker.done(XQueryElementType.DIR_ELEM_CONSTRUCTOR)
            return true
        }

        elementMarker.drop()
        return false
    }

    private fun parseDirAttributeList(): Boolean {
        val attributeListMarker = mark()

        // NOTE: The XQuery grammar uses whitespace as the token to start the next iteration of the matching loop.
        // Because the parseQName function can consume that whitespace during error handling, the QName tokens are
        // used as the next iteration marker in this implementation.
        var parsed = builder.matchTokenType(XQueryTokenType.XML_WHITE_SPACE)
        while (parseDirAttribute()) {
            parsed = true
            builder.matchTokenType(XQueryTokenType.XML_WHITE_SPACE)
        }

        if (parsed) {
            attributeListMarker.done(XQueryElementType.DIR_ATTRIBUTE_LIST)
            return true
        }

        attributeListMarker.drop()
        return false
    }

    private fun parseDirAttribute(): Boolean {
        val attributeMarker = mark()
        if (parseQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
            var haveErrors = false

            builder.matchTokenType(XQueryTokenType.XML_WHITE_SPACE)
            if (!builder.matchTokenType(XQueryTokenType.XML_EQUAL)) {
                error(XPathBundle.message("parser.error.expected", "="))
                haveErrors = true
            }

            builder.matchTokenType(XQueryTokenType.XML_WHITE_SPACE)
            if (!parseDirAttributeValue() && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-attribute-string"))
            }

            attributeMarker.done(XQueryElementType.DIR_ATTRIBUTE)
            return true
        }
        attributeMarker.drop()
        return false
    }

    private fun parseDirAttributeValue(): Boolean {
        val stringMarker = matchTokenTypeWithMarker(XQueryTokenType.XML_ATTRIBUTE_VALUE_START)
        while (stringMarker != null) {
            if (builder.matchTokenType(XQueryTokenType.XML_ATTRIBUTE_VALUE_CONTENTS) ||
                    builder.matchTokenType(XQueryTokenType.XML_PREDEFINED_ENTITY_REFERENCE) ||
                    builder.matchTokenType(XQueryTokenType.XML_CHARACTER_REFERENCE) ||
                    builder.matchTokenType(XQueryTokenType.XML_ESCAPED_CHARACTER)) {
                //
            } else if (builder.matchTokenType(XQueryTokenType.XML_ATTRIBUTE_VALUE_END)) {
                stringMarker.done(XQueryElementType.DIR_ATTRIBUTE_VALUE)
                return true
            } else if (builder.matchTokenType(XQueryTokenType.XML_PARTIAL_ENTITY_REFERENCE)) {
                error(XQueryBundle.message("parser.error.incomplete-entity"))
            } else if (errorOnTokenType(XQueryTokenType.XML_EMPTY_ENTITY_REFERENCE, XQueryBundle.message("parser.error.empty-entity")) || builder.matchTokenType(XPathTokenType.BAD_CHARACTER)) {
                //
            } else if (parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL) || errorOnTokenType(XPathTokenType.BLOCK_CLOSE, XQueryBundle.message("parser.error.mismatched-exclosed-expr"))) {
                //
            } else {
                stringMarker.done(XQueryElementType.DIR_ATTRIBUTE_VALUE)
                error(XQueryBundle.message("parser.error.incomplete-attribute-value"))
                return true
            }
        }
        return false
    }

    private fun parseDirCommentConstructor(): Boolean {
        val commentMarker = matchTokenTypeWithMarker(XQueryTokenType.XML_COMMENT_START_TAG)
        if (commentMarker != null) {
            // NOTE: XQueryTokenType.XML_COMMENT is omitted by the PsiBuilder.
            if (builder.matchTokenType(XQueryTokenType.XML_COMMENT_END_TAG)) {
                commentMarker.done(XQueryElementType.DIR_COMMENT_CONSTRUCTOR)
            } else {
                advanceLexer() // XQueryTokenType.UNEXPECTED_END_OF_BLOCK
                commentMarker.done(XQueryElementType.DIR_COMMENT_CONSTRUCTOR)
                error(XQueryBundle.message("parser.error.incomplete-xml-comment"))
            }
            return true
        }

        return errorOnTokenType(XQueryTokenType.XML_COMMENT_END_TAG, XPathBundle.message("parser.error.end-of-comment-without-start", "<!--"))
    }

    private fun parseDirPIConstructor(): Boolean {
        val piMarker = matchTokenTypeWithMarker(XQueryTokenType.PROCESSING_INSTRUCTION_BEGIN)
        if (piMarker != null) {
            var haveErrors = false

            if (builder.matchTokenType(XPathTokenType.WHITE_SPACE)) {
                error(XQueryBundle.message("parser.error.unexpected-whitespace"))
                haveErrors = true
            }

            if (!parseQNameOrWildcard(builder, XQueryElementType.NCNAME, false) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-ncname"))
                haveErrors = true
            }

            builder.matchTokenType(XPathTokenType.WHITE_SPACE)
            if (!builder.matchTokenType(XQueryTokenType.PROCESSING_INSTRUCTION_CONTENTS) && !haveErrors) {
                error(XQueryBundle.message("parser.error.expected-pi-contents"))
                haveErrors = true
            }

            while (
                builder.matchTokenType(XPathTokenType.BAD_CHARACTER) ||
                builder.matchTokenType(XPathTokenType.NCNAME) ||
                builder.matchTokenType(XQueryTokenType.PROCESSING_INSTRUCTION_CONTENTS)
            ) {
                //
            }

            if (!builder.matchTokenType(XQueryTokenType.PROCESSING_INSTRUCTION_END) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "?>"))
            }

            piMarker.done(XQueryElementType.DIR_PI_CONSTRUCTOR)
            return true
        }

        return false
    }

    private fun parseDirElemContent(depth: Int): Boolean {
        val elemContentMarker = mark()
        var matched = false
        while (true) {
            if (
                builder.matchTokenType(XQueryTokenType.XML_ELEMENT_CONTENTS) ||
                builder.matchTokenType(XPathTokenType.BAD_CHARACTER) ||
                builder.matchTokenType(XQueryTokenType.PREDEFINED_ENTITY_REFERENCE) ||
                builder.matchTokenType(XQueryTokenType.CHARACTER_REFERENCE) ||
                builder.matchTokenType(XPathTokenType.ESCAPED_CHARACTER) ||
                errorOnTokenType(XPathTokenType.BLOCK_CLOSE, XQueryBundle.message("parser.error.mismatched-exclosed-expr")) ||
                errorOnTokenType(XQueryTokenType.EMPTY_ENTITY_REFERENCE, XQueryBundle.message("parser.error.empty-entity"))
            ) {
                matched = true
            } else if (builder.matchTokenType(XQueryTokenType.PARTIAL_ENTITY_REFERENCE)) {
                error(XQueryBundle.message("parser.error.incomplete-entity"))
                matched = true
            } else if (parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL) ||
                    parseCDataSection(XQueryElementType.DIR_ELEM_CONTENT) ||
                    parseDirectConstructor(depth)) {
                matched = true
            } else {
                if (matched) {
                    elemContentMarker.done(XQueryElementType.DIR_ELEM_CONTENT)
                    return true
                }

                elemContentMarker.drop()
                return false
            }
        }
    }

    private fun parseCDataSection(context: IElementType?): Boolean {
        val cdataMarker = mark()
        val errorMarker = mark()
        if (builder.matchTokenType(XQueryTokenType.CDATA_SECTION_START_TAG)) {
            if (context == null) {
                errorMarker.error(XQueryBundle.message("parser.error.cdata-section-not-in-element-content"))
            } else {
                errorMarker.drop()
            }

            builder.matchTokenType(XQueryTokenType.CDATA_SECTION)
            if (builder.matchTokenType(XQueryTokenType.CDATA_SECTION_END_TAG)) {
                cdataMarker.done(XQueryElementType.CDATA_SECTION)
            } else {
                advanceLexer() // XQueryTokenType.UNEXPECTED_END_OF_BLOCK
                cdataMarker.done(XQueryElementType.CDATA_SECTION)
                error(XQueryBundle.message("parser.error.incomplete-cdata-section"))
            }
            return true
        }

        errorMarker.drop()
        cdataMarker.drop()
        return errorOnTokenType(XQueryTokenType.CDATA_SECTION_END_TAG, XQueryBundle.message("parser.error.end-of-cdata-section-without-start"))
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: PrimaryExpr :: NodeConstructor :: ComputedConstructor

    private fun parseComputedConstructor(): Boolean {
        return (parseCompDocConstructor()
                || parseCompElemConstructor()
                || parseCompAttrConstructor()
                || parseCompNamespaceConstructor()
                || parseCompTextConstructor()
                || parseCompCommentConstructor()
                || parseCompPIConstructor())
    }

    private fun parseCompDocConstructor(): Boolean {
        val documentMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DOCUMENT)
        if (documentMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                documentMarker.rollbackTo()
                return false
            }

            documentMarker.done(XQueryElementType.COMP_DOC_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseCompElemConstructor(): Boolean {
        val elementMarker = matchTokenTypeWithMarker(XPathTokenType.K_ELEMENT)
        if (elementMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseQNameSeparator(builder, null)) { // QName
                elementMarker.rollbackTo()
                return false
            }

            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false) && !parseEnclosedExprOrBlock(null, BlockOpen.REQUIRED, BlockExpr.REQUIRED)) {
                if (getTokenType() === XPathTokenType.STRING_LITERAL_START) {
                    val marker = mark()
                    parseStringLiteral(builder)
                    marker.error(XQueryBundle.message("parser.error.expected-qname-or-braced-expression"))
                } else {
                    elementMarker.rollbackTo()
                    return false
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_CONTENT_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)

            elementMarker.done(XQueryElementType.COMP_ELEM_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseCompAttrConstructor(): Boolean {
        val attributeMarker = matchTokenTypeWithMarker(XPathTokenType.K_ATTRIBUTE)
        if (attributeMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseQNameSeparator(builder, null)) { // QName
                attributeMarker.rollbackTo()
                return false
            }

            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false) && !parseEnclosedExprOrBlock(null, BlockOpen.REQUIRED, BlockExpr.REQUIRED)) {
                if (getTokenType() === XPathTokenType.STRING_LITERAL_START) {
                    val marker = mark()
                    parseStringLiteral(builder)
                    marker.error(XQueryBundle.message("parser.error.expected-qname-or-braced-expression"))
                } else {
                    attributeMarker.rollbackTo()
                    return false
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)

            attributeMarker.done(XQueryElementType.COMP_ATTR_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseCompNamespaceConstructor(): Boolean {
        val namespaceMarker = matchTokenTypeWithMarker(XPathTokenType.K_NAMESPACE)
        if (namespaceMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseQNameSeparator(builder, null)) { // QName
                namespaceMarker.rollbackTo()
                return false
            }

            if (!parseEQNameOrWildcard(builder, XQueryElementType.PREFIX, false) && !parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_PREFIX_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                if (getTokenType() === XPathTokenType.STRING_LITERAL_START) {
                    val marker = mark()
                    parseStringLiteral(builder)
                    marker.error(XQueryBundle.message("parser.error.expected-identifier-or-braced-expression"))
                } else {
                    namespaceMarker.rollbackTo()
                    return false
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_URI_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)

            namespaceMarker.done(XQueryElementType.COMP_NAMESPACE_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseCompTextConstructor(): Boolean {
        val textMarker = matchTokenTypeWithMarker(XPathTokenType.K_TEXT)
        if (textMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                textMarker.rollbackTo()
                return false
            }

            textMarker.done(XQueryElementType.COMP_TEXT_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseCompCommentConstructor(): Boolean {
        val commentMarker = matchTokenTypeWithMarker(XPathTokenType.K_COMMENT)
        if (commentMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)) {
                commentMarker.rollbackTo()
                return false
            }

            commentMarker.done(XQueryElementType.COMP_COMMENT_CONSTRUCTOR)
            return true
        }
        return false
    }

    private fun parseCompPIConstructor(): Boolean {
        val piMarker = matchTokenTypeWithMarker(XPathTokenType.K_PROCESSING_INSTRUCTION)
        if (piMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseQNameSeparator(builder, null)) { // QName
                piMarker.rollbackTo()
                return false
            }

            if (!parseQNameOrWildcard(builder, XQueryElementType.NCNAME, false) && !parseEnclosedExprOrBlock(null, BlockOpen.REQUIRED, BlockExpr.REQUIRED)) {
                if (getTokenType() === XPathTokenType.STRING_LITERAL_START) {
                    val marker = mark()
                    parseStringLiteral(builder)
                    marker.error(XQueryBundle.message("parser.error.expected-identifier-or-braced-expression"))
                } else {
                    piMarker.rollbackTo()
                    return false
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            parseEnclosedExprOrBlock(XQueryElementType.ENCLOSED_EXPR, BlockOpen.REQUIRED, BlockExpr.OPTIONAL)

            piMarker.done(XQueryElementType.COMP_PI_CONSTRUCTOR)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: FTSelection

    private fun parseFTSelection(): Boolean {
        val selectionMarker = mark()
        if (parseFTOr()) {
            do {
                parseWhiteSpaceAndCommentTokens(builder)
            } while (parseFTPosFilter())

            selectionMarker.done(XQueryElementType.FT_SELECTION)
            return true
        }
        selectionMarker.drop()
        return false
    }

    private fun parseFTOr(): Boolean {
        val orMarker = mark()
        if (parseFTAnd()) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XQueryTokenType.K_FTOR)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseFTAnd() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "FTAnd"))
                    haveErrors = true
                }
            }

            orMarker.done(XQueryElementType.FT_OR)
            return true
        }
        orMarker.drop()
        return false
    }

    private fun parseFTAnd(): Boolean {
        val andMarker = mark()
        if (parseFTMildNot()) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XQueryTokenType.K_FTAND)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseFTMildNot() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "FTMildNot"))
                    haveErrors = true
                }
            }

            andMarker.done(XQueryElementType.FT_AND)
            return true
        }
        andMarker.drop()
        return false
    }

    private fun parseFTMildNot(): Boolean {
        val mildNotMarker = mark()
        if (parseFTUnaryNot()) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XQueryTokenType.K_NOT)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.K_IN) && !haveErrors) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "in"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseFTUnaryNot() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "FTUnaryNot"))
                    haveErrors = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
            }

            mildNotMarker.done(XQueryElementType.FT_MILD_NOT)
            return true
        }
        mildNotMarker.drop()
        return false
    }

    private fun parseFTUnaryNot(): Boolean {
        val unaryNotMarker = mark()

        builder.matchTokenType(XQueryTokenType.K_FTNOT)

        parseWhiteSpaceAndCommentTokens(builder)
        if (parseFTPrimaryWithOptions()) {
            parseWhiteSpaceAndCommentTokens(builder)

            unaryNotMarker.done(XQueryElementType.FT_UNARY_NOT)
            return true
        }
        unaryNotMarker.drop()
        return false
    }

    private fun parseFTPrimaryWithOptions(): Boolean {
        val primaryWithOptionsMarker = mark()
        if (parseFTPrimary()) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseFTMatchOptions()

            parseWhiteSpaceAndCommentTokens(builder)
            parseFTWeight()

            primaryWithOptionsMarker.done(XQueryElementType.FT_PRIMARY_WITH_OPTIONS)
            return true
        }
        primaryWithOptionsMarker.drop()
        return false
    }

    private fun parseFTPrimary(): Boolean {
        val primaryMarker = mark()
        if (parseFTWords()) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseFTTimes()

            primaryMarker.done(XQueryElementType.FT_PRIMARY)
            return true
        } else if (builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTSelection()) {
                error(XPathBundle.message("parser.error.expected", "FTSelection"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            primaryMarker.done(XQueryElementType.FT_PRIMARY)
            return true
        } else if (parseFTExtensionSelection()) {
            primaryMarker.done(XQueryElementType.FT_PRIMARY)
            return true
        }
        primaryMarker.drop()
        return false
    }

    private fun parseFTWords(): Boolean {
        val wordsMarker = mark()
        if (parseFTWordsValue()) {
            parseWhiteSpaceAndCommentTokens(builder)
            parseFTAnyallOption()

            wordsMarker.done(XQueryElementType.FT_WORDS)
            return true
        }
        wordsMarker.drop()
        return false
    }

    private fun parseFTWordsValue(): Boolean {
        val wordsValueMarker = mark()
        if (parseStringLiteral(builder)) {
            wordsValueMarker.done(XQueryElementType.FT_WORDS_VALUE)
            return true
        } else if (builder.matchTokenType(XPathTokenType.BLOCK_OPEN)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExpr(XQueryElementType.EXPR)) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.BLOCK_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "}"))
            }

            wordsValueMarker.done(XQueryElementType.FT_WORDS_VALUE)
            return true
        }
        wordsValueMarker.drop()
        return false
    }

    private fun parseFTExtensionSelection(): Boolean {
        val extensionSelectionMarker = mark()
        var haveError = false

        var havePragma = false
        while (parsePragma()) {
            parseWhiteSpaceAndCommentTokens(builder)
            havePragma = true
        }

        if (!havePragma) {
            extensionSelectionMarker.drop()
            return false
        }

        parseWhiteSpaceAndCommentTokens(builder)
        if (!builder.matchTokenType(XPathTokenType.BLOCK_OPEN)) {
            error(XPathBundle.message("parser.error.expected", "{"))
            haveError = true
        }

        parseWhiteSpaceAndCommentTokens(builder)
        parseFTSelection()

        parseWhiteSpaceAndCommentTokens(builder)
        if (!builder.matchTokenType(XPathTokenType.BLOCK_CLOSE) && !haveError) {
            error(XPathBundle.message("parser.error.expected", "}"))
        }

        extensionSelectionMarker.done(XQueryElementType.FT_EXTENSION_SELECTION)
        return true
    }

    private fun parseFTAnyallOption(): Boolean {
        val anyallOptionMarker = mark()
        if (builder.matchTokenType(XQueryTokenType.K_ANY)) {
            parseWhiteSpaceAndCommentTokens(builder)
            builder.matchTokenType(XQueryTokenType.K_WORD)

            anyallOptionMarker.done(XQueryElementType.FT_ANYALL_OPTION)
            return true
        } else if (builder.matchTokenType(XQueryTokenType.K_ALL)) {
            parseWhiteSpaceAndCommentTokens(builder)
            builder.matchTokenType(XQueryTokenType.K_WORDS)

            anyallOptionMarker.done(XQueryElementType.FT_ANYALL_OPTION)
            return true
        } else if (builder.matchTokenType(XQueryTokenType.K_PHRASE)) {
            anyallOptionMarker.done(XQueryElementType.FT_ANYALL_OPTION)
            return true
        }
        anyallOptionMarker.drop()
        return false
    }

    private fun parseFTTimes(): Boolean {
        val timesMarker = matchTokenTypeWithMarker(XQueryTokenType.K_OCCURS)
        if (timesMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTRange(XQueryElementType.FT_RANGE)) {
                error(XPathBundle.message("parser.error.expected", "FTRange"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_TIMES) && !haveError) {
                error(XQueryBundle.message("parser.error.expected-keyword", "times"))
            }

            timesMarker.done(XQueryElementType.FT_TIMES)
            return true
        }
        return false
    }

    private fun parseFTRange(type: IElementType): Boolean {
        if (getTokenType() === XQueryTokenType.K_EXACTLY) {
            val rangeMarker = mark()
            advanceLexer()

            parseWhiteSpaceAndCommentTokens(builder)
            if (type === XQueryElementType.FT_LITERAL_RANGE) {
                if (!builder.matchTokenType(XPathTokenType.INTEGER_LITERAL)) {
                    error(XPathBundle.message("parser.error.expected", "IntegerLiteral"))
                }
            } else {
                if (!parseAdditiveExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "AdditiveExpr"))
                }
            }

            rangeMarker.done(type)
            return true
        } else if (getTokenType() === XQueryTokenType.K_AT) {
            val rangeMarker = mark()
            advanceLexer()

            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_LEAST) && !builder.matchTokenType(XQueryTokenType.K_MOST)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "least, most"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (type === XQueryElementType.FT_LITERAL_RANGE) {
                if (!builder.matchTokenType(XPathTokenType.INTEGER_LITERAL) && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "IntegerLiteral"))
                }
            } else {
                if (!parseAdditiveExpr(type) && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "AdditiveExpr"))
                }
            }

            rangeMarker.done(type)
            return true
        } else if (getTokenType() === XQueryTokenType.K_FROM) {
            val rangeMarker = mark()
            advanceLexer()

            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (type === XQueryElementType.FT_LITERAL_RANGE) {
                if (!builder.matchTokenType(XPathTokenType.INTEGER_LITERAL)) {
                    error(XPathBundle.message("parser.error.expected", "IntegerLiteral"))
                    haveError = true
                }
            } else {
                if (!parseAdditiveExpr(type)) {
                    error(XPathBundle.message("parser.error.expected", "AdditiveExpr"))
                    haveError = true
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.K_TO) && !haveError) {
                error(XQueryBundle.message("parser.error.expected-keyword", "to"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (type === XQueryElementType.FT_LITERAL_RANGE) {
                if (!builder.matchTokenType(XPathTokenType.INTEGER_LITERAL) && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "IntegerLiteral"))
                }
            } else {
                if (!parseAdditiveExpr(type) && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "AdditiveExpr"))
                }
            }

            rangeMarker.done(type)
            return true
        }
        return false
    }

    private fun parseFTWeight(): Boolean {
        val weightMarker = matchTokenTypeWithMarker(XQueryTokenType.K_WEIGHT)
        if (weightMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.BLOCK_OPEN)) {
                error(XPathBundle.message("parser.error.expected", "{"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseExpr(XQueryElementType.EXPR) && !haveError) {
                error(XPathBundle.message("parser.error.expected-expression"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.BLOCK_CLOSE) && !haveError) {
                error(XPathBundle.message("parser.error.expected", "}"))
            }

            weightMarker.done(XQueryElementType.FT_WEIGHT)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: FTPosFilter

    private fun parseFTPosFilter(): Boolean {
        return parseFTOrder() || parseFTWindow() || parseFTDistance() || parseFTScope() || parseFTContent()
    }

    private fun parseFTOrder(): Boolean {
        val orderMarker = matchTokenTypeWithMarker(XQueryTokenType.K_ORDERED)
        if (orderMarker != null) {
            orderMarker.done(XQueryElementType.FT_ORDER)
            return true
        }
        return false
    }

    private fun parseFTWindow(): Boolean {
        val windowMarker = matchTokenTypeWithMarker(XQueryTokenType.K_WINDOW)
        if (windowMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseAdditiveExpr(XQueryElementType.FT_WINDOW)) {
                error(XPathBundle.message("parser.error.expected", "AdditiveExpr"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTUnit() && !haveError) {
                error(XQueryBundle.message("parser.error.expected-keyword", "paragraphs, sentences, words"))
            }

            windowMarker.done(XQueryElementType.FT_WINDOW)
            return true
        }
        return false
    }

    private fun parseFTDistance(): Boolean {
        val distanceMarker = matchTokenTypeWithMarker(XQueryTokenType.K_DISTANCE)
        if (distanceMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTRange(XQueryElementType.FT_RANGE)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "at, exactly, from"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTUnit() && !haveError) {
                error(XQueryBundle.message("parser.error.expected-keyword", "paragraphs, sentences, words"))
            }

            distanceMarker.done(XQueryElementType.FT_DISTANCE)
            return true
        }
        return false
    }

    private fun parseFTScope(): Boolean {
        val scopeMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SAME, XQueryTokenType.K_DIFFERENT)
        if (scopeMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTBigUnit()) {
                error(XQueryBundle.message("parser.error.expected-keyword", "paragraph, sentence"))
            }

            scopeMarker.done(XQueryElementType.FT_SCOPE)
            return true
        }
        return false
    }

    private fun parseFTContent(): Boolean {
        if (getTokenType() === XQueryTokenType.K_AT) {
            val contentMarker = mark()
            advanceLexer()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_START) && !builder.matchTokenType(XQueryTokenType.K_END)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "end, start"))
            }

            contentMarker.done(XQueryElementType.FT_CONTENT)
            return true
        } else if (getTokenType() === XQueryTokenType.K_ENTIRE) {
            val contentMarker = mark()
            advanceLexer()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_CONTENT)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "content"))
            }

            contentMarker.done(XQueryElementType.FT_CONTENT)
            return true
        }
        return false
    }

    private fun parseFTUnit(): Boolean {
        if (getTokenType() === XQueryTokenType.K_WORDS ||
                getTokenType() === XQueryTokenType.K_SENTENCES ||
                getTokenType() === XQueryTokenType.K_PARAGRAPHS) {
            val marker = mark()
            advanceLexer()
            marker.done(XQueryElementType.FT_UNIT)
            return true
        }
        return false
    }

    private fun parseFTBigUnit(): Boolean {
        if (getTokenType() === XQueryTokenType.K_SENTENCE || getTokenType() === XQueryTokenType.K_PARAGRAPH) {
            val marker = mark()
            advanceLexer()
            marker.done(XQueryElementType.FT_BIG_UNIT)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: OrExpr :: FTMatchOptions

    private fun parseFTMatchOptions(): Boolean {
        val matchOptionsMarker = mark()

        var haveFTMatchOptions = false
        var haveFTMatchOption = false
        do {
            if (builder.matchTokenType(XQueryTokenType.K_USING)) {
                parseWhiteSpaceAndCommentTokens(builder)
                parseFTMatchOption()
                haveFTMatchOption = true
            } else if (getTokenType() === XQueryTokenType.K_CASE ||
                    getTokenType() === XQueryTokenType.K_DIACRITICS ||
                    getTokenType() === XQueryTokenType.K_FUZZY ||
                    getTokenType() === XQueryTokenType.K_LANGUAGE ||
                    getTokenType() === XQueryTokenType.K_LOWERCASE ||
                    getTokenType() === XQueryTokenType.K_NO ||
                    getTokenType() === XQueryTokenType.K_OPTION ||
                    getTokenType() === XQueryTokenType.K_STEMMING ||
                    getTokenType() === XQueryTokenType.K_STOP ||
                    getTokenType() === XQueryTokenType.K_THESAURUS ||
                    getTokenType() === XQueryTokenType.K_UPPERCASE ||
                    getTokenType() === XQueryTokenType.K_WILDCARDS) {
                error(XQueryBundle.message("parser.error.expected-keyword", "using"))
                parseFTMatchOption()
                haveFTMatchOption = true
            } else {
                haveFTMatchOption = false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            haveFTMatchOptions = haveFTMatchOptions or haveFTMatchOption
        } while (haveFTMatchOption)

        if (haveFTMatchOptions) {
            matchOptionsMarker.done(XQueryElementType.FT_MATCH_OPTIONS)
        } else {
            matchOptionsMarker.drop()
        }
        return haveFTMatchOptions
    }

    private fun parseFTMatchOption(): Boolean {
        val matchOptionMarker = mark()
        if (parseFTCaseOption(matchOptionMarker) ||
                parseFTDiacriticsOption(matchOptionMarker) ||
                parseFTExtensionOption(matchOptionMarker) ||
                parseFTFuzzyOption(matchOptionMarker) ||
                parseFTLanguageOption(matchOptionMarker) ||
                parseFTStemOption(matchOptionMarker) ||
                parseFTStopWordOption(matchOptionMarker) ||
                parseFTThesaurusOption(matchOptionMarker) ||
                parseFTWildCardOption(matchOptionMarker)) {
            //
        } else if (builder.matchTokenType(XQueryTokenType.K_NO)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XQueryTokenType.K_STEMMING)) {
                matchOptionMarker.done(XQueryElementType.FT_STEM_OPTION)
            } else if (builder.matchTokenType(XQueryTokenType.K_STOP)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XQueryTokenType.K_WORDS)) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "words"))
                }

                matchOptionMarker.done(XQueryElementType.FT_STOP_WORD_OPTION)
            } else if (builder.matchTokenType(XQueryTokenType.K_THESAURUS)) {
                matchOptionMarker.done(XQueryElementType.FT_THESAURUS_OPTION)
            } else if (builder.matchTokenType(XQueryTokenType.K_WILDCARDS)) {
                matchOptionMarker.done(XQueryElementType.FT_WILDCARD_OPTION)
            } else {
                error(XQueryBundle.message("parser.error.expected-keyword", "stemming, stop, thesaurus, wildcards"))
                matchOptionMarker.drop()
                return false
            }
        } else {
            // NOTE: `fuzzy` is the BaseX FTMatchOption extension.
            error(XQueryBundle.message("parser.error.expected-keyword-or-token", "FTMatchOption", "fuzzy"))
            matchOptionMarker.drop()
            return false
        }
        return true
    }

    private fun parseFTCaseOption(caseOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_LOWERCASE) || builder.matchTokenType(XQueryTokenType.K_UPPERCASE)) {
            caseOptionMarker.done(XQueryElementType.FT_CASE_OPTION)
            return true
        } else if (builder.matchTokenType(XQueryTokenType.K_CASE)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_SENSITIVE) && !builder.matchTokenType(XQueryTokenType.K_INSENSITIVE)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "sensitive, insensitive"))
            }

            caseOptionMarker.done(XQueryElementType.FT_CASE_OPTION)
            return true
        }
        return false
    }

    private fun parseFTDiacriticsOption(diacriticsOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_DIACRITICS)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_SENSITIVE) && !builder.matchTokenType(XQueryTokenType.K_INSENSITIVE)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "sensitive, insensitive"))
            }

            diacriticsOptionMarker.done(XQueryElementType.FT_DIACRITICS_OPTION)
            return true
        }
        return false
    }

    private fun parseFTStemOption(stemOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_STEMMING)) {
            stemOptionMarker.done(XQueryElementType.FT_STEM_OPTION)
            return true
        }
        return false
    }

    private fun parseFTThesaurusOption(thesaurusOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_THESAURUS)) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            val hasParenthesis = builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_DEFAULT) && !parseFTThesaurusID()) {
                if (hasParenthesis) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "at, default"))
                } else {
                    error(XQueryBundle.message("parser.error.expected-keyword-or-token", "(", "at, default"))
                }
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            var haveComma: Boolean
            if (hasParenthesis) {
                haveComma = builder.matchTokenType(XPathTokenType.COMMA)
            } else {
                haveComma = errorOnTokenType(XPathTokenType.COMMA, XQueryBundle.message("parser.error.full-text.multientry-thesaurus-requires-parenthesis"))
                haveError = haveError or haveComma
            }

            while (haveComma) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseFTThesaurusID() && !haveError) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "at"))

                    builder.matchTokenType(XQueryTokenType.K_DEFAULT)
                    parseWhiteSpaceAndCommentTokens(builder)

                    haveError = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                haveComma = builder.matchTokenType(XPathTokenType.COMMA)
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (hasParenthesis) {
                if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveError) {
                    error(XQueryBundle.message("parser.error.expected-either", ",", ")"))
                }
            } else if (!haveError) {
                errorOnTokenType(XPathTokenType.PARENTHESIS_CLOSE, XQueryBundle.message("parser.error.expected-keyword-or-token", ";", "using"))
            } else {
                builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)
            }

            thesaurusOptionMarker.done(XQueryElementType.FT_THESAURUS_OPTION)
            return true
        }
        return false
    }

    private fun parseFTThesaurusID(): Boolean {
        val thesaurusIdMarker = matchTokenTypeWithMarker(XQueryTokenType.K_AT)
        if (thesaurusIdMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL)) {
                error(XPathBundle.message("parser.error.expected", "URILiteral"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XQueryTokenType.K_RELATIONSHIP)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseStringLiteral(builder) && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "StringLiteral"))
                    haveError = true
                }
            }

            if (parseFTRange(XQueryElementType.FT_LITERAL_RANGE)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XQueryTokenType.K_LEVELS) && !haveError) {
                    error(XQueryBundle.message("parser.error.expected-keyword", "levels"))
                }
            }

            thesaurusIdMarker.done(XQueryElementType.FT_THESAURUS_ID)
            return true
        }
        return false
    }

    private fun parseFTStopWordOption(stopWordOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_STOP)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_WORDS)) {
                error(XQueryBundle.message("parser.error.expected-keyword", "words"))
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_DEFAULT) && !parseFTStopWords()) {
                error(XQueryBundle.message("parser.error.expected-keyword-or-token", "(", "at, default"))
            }

            do {
                parseWhiteSpaceAndCommentTokens(builder)
            } while (parseFTStopWordsInclExcl())

            stopWordOptionMarker.done(XQueryElementType.FT_STOP_WORD_OPTION)
            return true
        }
        return false
    }

    private fun parseFTStopWords(): Boolean {
        if (getTokenType() === XQueryTokenType.K_AT) {
            val stopWordsMarker = mark()
            advanceLexer()

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder, XQueryElementType.URI_LITERAL)) {
                error(XPathBundle.message("parser.error.expected", "URILiteral"))
            }

            stopWordsMarker.done(XQueryElementType.FT_STOP_WORDS)
            return true
        } else if (getTokenType() === XPathTokenType.PARENTHESIS_OPEN) {
            val stopWordsMarker = mark()
            advanceLexer()

            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder)) {
                error(XPathBundle.message("parser.error.expected", "StringLiteral"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.COMMA)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseStringLiteral(builder) && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "StringLiteral"))
                    haveError = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveError) {
                error(XQueryBundle.message("parser.error.expected-either", ",", ")"))
            }

            stopWordsMarker.done(XQueryElementType.FT_STOP_WORDS)
            return true
        }
        return false
    }

    private fun parseFTStopWordsInclExcl(): Boolean {
        val stopWordsInclExclMarker = matchTokenTypeWithMarker(XPathTokenType.K_UNION, XPathTokenType.K_EXCEPT)
        if (stopWordsInclExclMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseFTStopWords()) {
                error(XQueryBundle.message("parser.error.expected-keyword-or-token", "(", "at"))
            }

            stopWordsInclExclMarker.done(XQueryElementType.FT_STOP_WORDS_INCL_EXCL)
            return true
        }
        return false
    }

    private fun parseFTLanguageOption(languageOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_LANGUAGE)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder)) {
                error(XPathBundle.message("parser.error.expected", "StringLiteral"))
            }

            languageOptionMarker.done(XQueryElementType.FT_LANGUAGE_OPTION)
            return true
        }
        return false
    }

    private fun parseFTWildCardOption(wildcardOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_WILDCARDS)) {
            wildcardOptionMarker.done(XQueryElementType.FT_WILDCARD_OPTION)
            return true
        }
        return false
    }

    private fun parseFTExtensionOption(extensionOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_OPTION)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                error(XPathBundle.message("parser.error.expected-eqname"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseStringLiteral(builder) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "StringLiteral"))
            }

            extensionOptionMarker.done(XQueryElementType.FT_EXTENSION_OPTION)
            return true
        }
        return false
    }

    private fun parseFTFuzzyOption(fuzzyOptionMarker: PsiBuilder.Marker): Boolean {
        if (builder.matchTokenType(XQueryTokenType.K_FUZZY)) {
            fuzzyOptionMarker.done(XQueryElementType.FT_FUZZY_OPTION)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: Expr :: UpdatingFunctionCall

    private fun parseUpdatingFunctionCall(): Boolean {
        val updatingFunctionCallMarker = matchTokenTypeWithMarker(XQueryTokenType.K_INVOKE)
        if (updatingFunctionCallMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XQueryTokenType.K_UPDATING)) {
                if (builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) { // FunctionCall
                    updatingFunctionCallMarker.rollbackTo()
                    return false
                }

                error(XQueryBundle.message("parser.error.expected-keyword", "updating"))
                haveErrors = true

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parsePrimaryExpr(builder, null)) { // AbbrevForwardStep
                    updatingFunctionCallMarker.rollbackTo()
                    return false
                }
            } else {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parsePrimaryExpr(builder, null)) {
                    error(XPathBundle.message("parser.error.expected", "PrimaryExpr"))
                    haveErrors = true
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", "("))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseExprSingle(builder)) {
                parseWhiteSpaceAndCommentTokens(builder)
                while (builder.matchTokenType(XPathTokenType.COMMA)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (parseExprSingle(builder) && !haveErrors) {
                        error(XPathBundle.message("parser.error.expected-expression"))
                        haveErrors = true
                    }
                    parseWhiteSpaceAndCommentTokens(builder)
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            updatingFunctionCallMarker.done(XQueryElementType.UPDATING_FUNCTION_CALL)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: TypeDeclaration

    private fun parseTypeDeclaration(): Boolean {
        if (builder.matchTokenType(XPathTokenType.K_AS)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseSequenceType()) {
                error(XPathBundle.message("parser.error.expected", "SequenceType"))
            }
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: TypeDeclaration :: SequenceType

    private fun parseSequenceTypeUnion(): Boolean {
        val sequenceTypeUnionMarker = mark()
        if (parseSequenceTypeList()) {
            var haveErrors = false
            var haveSequenceTypeUnion = false

            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.UNION)) {
                haveSequenceTypeUnion = true
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSequenceTypeList() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "SequenceType"))
                    haveErrors = true
                }
                parseWhiteSpaceAndCommentTokens(builder)
            }

            if (haveSequenceTypeUnion)
                sequenceTypeUnionMarker.done(XQueryElementType.SEQUENCE_TYPE_UNION)
            else
                sequenceTypeUnionMarker.drop()
            return true
        }
        sequenceTypeUnionMarker.drop()
        return false
    }

    private fun parseSequenceTypeList(): Boolean {
        val sequenceTypeListMarker = mark()
        if (parseSequenceType()) {
            var haveErrors = false
            var haveSequenceTypeList = false

            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.COMMA)) {
                haveSequenceTypeList = true
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSequenceType() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "SequenceType"))
                    haveErrors = true
                }
                parseWhiteSpaceAndCommentTokens(builder)
            }

            if (haveSequenceTypeList)
                sequenceTypeListMarker.done(XQueryElementType.SEQUENCE_TYPE_LIST)
            else
                sequenceTypeListMarker.drop()
            return true
        }
        sequenceTypeListMarker.drop()
        return false
    }

    private fun parseSequenceType(): Boolean {
        val sequenceTypeMarker = mark()
        if (builder.matchTokenType(XPathTokenType.K_EMPTY_SEQUENCE) || builder.matchTokenType(XPathTokenType.K_EMPTY)) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                sequenceTypeMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            sequenceTypeMarker.done(XPathElementType.SEQUENCE_TYPE)
            return true
        } else if (parseItemType()) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseOccurrenceIndicator())
                sequenceTypeMarker.done(XPathElementType.SEQUENCE_TYPE)
            else
                sequenceTypeMarker.drop()
            return true
        } else if (parseParenthesizedSequenceType()) {
            sequenceTypeMarker.drop()
            return true
        }

        sequenceTypeMarker.drop()
        return false
    }

    private fun parseOccurrenceIndicator(): Boolean {
        return builder.matchTokenType(XPathTokenType.OPTIONAL) ||
                builder.matchTokenType(XPathTokenType.STAR) ||
                builder.matchTokenType(XPathTokenType.PLUS)
    }

    private fun parseParenthesizedSequenceType(): Boolean {
        if (builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseSequenceTypeUnion()) {
                error(XPathBundle.message("parser.error.expected", "SequenceType"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: TypeDeclaration :: ItemType

    private fun parseItemType(): Boolean {
        return parseKindTest(builder) ||
                parseAnyItemType() ||
                parseAnnotatedFunctionOrSequence() ||
                parseMapTest() ||
                parseArrayTest() ||
                parseTupleType() ||
                parseUnionType() ||
                parseAtomicOrUnionType() ||
                parseParenthesizedItemType()
    }

    private fun parseAtomicOrUnionType(): Boolean {
        return parseEQNameOrWildcard(builder, XPathElementType.ATOMIC_OR_UNION_TYPE, false)
    }

    private fun parseAnyItemType(): Boolean {
        val itemTypeMarker = matchTokenTypeWithMarker(XPathTokenType.K_ITEM)
        if (itemTypeMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                itemTypeMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            itemTypeMarker.done(XPathElementType.ANY_ITEM_TYPE)
            return true
        }
        return false
    }

    private fun parseTupleType(): Boolean {
        val tupleTypeMarker = matchTokenTypeWithMarker(XQueryTokenType.K_TUPLE)
        if (tupleTypeMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                tupleTypeMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseTupleField()) {
                error(XPathBundle.message("parser.error.expected", "NCName"))
                haveError = true
            }

            var isExtensible = false
            var haveNext = true
            while (haveNext) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (isExtensible) {
                    val marker = mark()
                    if (!builder.matchTokenType(XPathTokenType.COMMA)) {
                        haveNext = false
                        marker.drop()
                        continue
                    } else {
                        marker.error(XQueryBundle.message("parser.error.tuple-wildcard-with-names-after"))
                    }
                } else if (!builder.matchTokenType(XPathTokenType.COMMA)) {
                    haveNext = false
                    continue
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (builder.matchTokenType(XPathTokenType.STAR)) {
                    isExtensible = true
                } else if (!parseTupleField() && !haveError) {
                    error(XQueryBundle.message("parser.error.expected-either", "NCName", "*"))
                    haveError = true
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveError) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            tupleTypeMarker.done(XQueryElementType.TUPLE_TYPE)
            return true
        }
        return false
    }

    private fun parseTupleField(): Boolean {
        val tupleFieldMarker = mark()
        if (parseNCName(XQueryElementType.NCNAME)) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            val haveSeparator =
                if (builder.matchTokenType(XQueryTokenType.ELVIS)) // ?: without whitespace
                    true
                else {
                    builder.matchTokenType(XPathTokenType.OPTIONAL)
                    parseWhiteSpaceAndCommentTokens(builder)
                    builder.matchTokenType(XPathTokenType.QNAME_SEPARATOR)
                }

            if (!haveSeparator) {
                if (getTokenType() === XPathTokenType.COMMA || getTokenType() === XPathTokenType.PARENTHESIS_CLOSE) {
                    tupleFieldMarker.done(XQueryElementType.TUPLE_FIELD)
                    return true
                }
                error(XPathBundle.message("parser.error.expected", ":"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseSequenceType() && !haveError) {
                error(XPathBundle.message("parser.error.expected", "SequenceType"))
            }

            tupleFieldMarker.done(XQueryElementType.TUPLE_FIELD)
            return true
        }
        tupleFieldMarker.drop()
        return false
    }

    private fun parseUnionType(): Boolean {
        val unionTypeMarker = matchTokenTypeWithMarker(XPathTokenType.K_UNION)
        if (unionTypeMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                unionTypeMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false)) {
                error(XPathBundle.message("parser.error.expected", "QName"))
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            while (builder.matchTokenType(XPathTokenType.COMMA)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseEQNameOrWildcard(builder, XQueryElementType.QNAME, false) && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "QName"))
                    haveError = true
                }
                parseWhiteSpaceAndCommentTokens(builder)
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveError) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            unionTypeMarker.done(XQueryElementType.UNION_TYPE)
            return true
        }
        return false
    }

    private fun parseAnnotatedFunctionOrSequence(): Boolean {
        val marker = mark()

        var haveAnnotations = false
        while (parseAnnotation()) {
            parseWhiteSpaceAndCommentTokens(builder)
            haveAnnotations = true
        }

        if (haveAnnotations && getTokenType() === XPathTokenType.K_FOR) {
            advanceLexer()
            parseWhiteSpaceAndCommentTokens(builder)

            if (!parseSequenceType()) {
                error(XPathBundle.message("parser.error.expected", "SequenceType"))
            }

            marker.done(XQueryElementType.ANNOTATED_SEQUENCE_TYPE)
            return true
        } else if (parseAnyOrTypedFunctionTest()) {
            marker.done(XQueryElementType.FUNCTION_TEST)
            return true
        } else if (haveAnnotations) {
            error(XQueryBundle.message("parser.error.expected-keyword", "function"))

            marker.done(XQueryElementType.FUNCTION_TEST)
            return true
        }

        marker.drop()
        return false
    }

    private fun parseAnyOrTypedFunctionTest(): Boolean {
        val functionTestMarker = matchTokenTypeWithMarker(XPathTokenType.K_FUNCTION)
        if (functionTestMarker != null) {
            var type: KindTest = KindTest.ANY_TEST
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                functionTestMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.STAR)) {
                //
            } else if (parseSequenceType()) {
                type = KindTest.TYPED_TEST

                parseWhiteSpaceAndCommentTokens(builder)
                while (builder.matchTokenType(XPathTokenType.COMMA)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseSequenceType() && !haveErrors) {
                        error(XPathBundle.message("parser.error.expected", "SequenceType"))
                        haveErrors = true
                    }
                    parseWhiteSpaceAndCommentTokens(builder)
                }
            } else {
                type = KindTest.TYPED_TEST
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                error(XPathBundle.message("parser.error.expected", ")"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (getTokenType() === XPathTokenType.K_AS) {
                if (type === KindTest.ANY_TEST && !haveErrors) {
                    val errorMarker = mark()
                    advanceLexer()
                    errorMarker.error(XQueryBundle.message("parser.error.as-not-supported-in-test", "AnyFunctionTest"))
                    haveErrors = true
                } else {
                    advanceLexer()
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSequenceType() && !haveErrors) {
                    error(XPathBundle.message("parser.error.expected", "SequenceType"))
                }
            } else if (type === KindTest.TYPED_TEST) {
                error(XPathBundle.message("parser.error.expected", "as"))
            }

            functionTestMarker.drop()
            return true
        }
        return false
    }

    private fun parseParenthesizedItemType(): Boolean {
        val parenthesizedItemTypeMarker = matchTokenTypeWithMarker(XPathTokenType.PARENTHESIS_OPEN)
        if (parenthesizedItemTypeMarker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!parseSequenceType()) {
                error(XPathBundle.message("parser.error.expected", "ItemType"))
                haveErrors = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                if (
                    getTokenType() === XPathTokenType.UNION ||
                    getTokenType() === XPathTokenType.COMMA
                ) {
                    parenthesizedItemTypeMarker.rollbackTo() // parenthesized sequence type
                    return false
                }
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            parenthesizedItemTypeMarker.done(XPathElementType.PARENTHESIZED_ITEM_TYPE)
            return true
        }
        return false
    }

    private fun parseMapTest(): Boolean {
        val mapTestMarker = matchTokenTypeWithMarker(XPathTokenType.K_MAP)
        if (mapTestMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                mapTestMarker.rollbackTo()
                return false
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.STAR)) {
                type = XPathElementType.ANY_MAP_TEST
            } else if (parseUnionType() || parseAtomicOrUnionType()) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (!builder.matchTokenType(XPathTokenType.COMMA)) {
                    error(XPathBundle.message("parser.error.expected", ","))
                    haveError = true
                }

                parseWhiteSpaceAndCommentTokens(builder)
                if (!parseSequenceType() && !haveError) {
                    error(XPathBundle.message("parser.error.expected", "SequenceType"))
                    haveError = true
                }

                type = XPathElementType.TYPED_MAP_TEST
            } else if (getTokenType() === XPathTokenType.COMMA) {
                error(XQueryBundle.message("parser.error.expected-either", "UnionType", "AtomicOrUnionType"))
                haveError = true

                builder.matchTokenType(XPathTokenType.COMMA)

                parseWhiteSpaceAndCommentTokens(builder)
                parseSequenceType()

                type = XPathElementType.TYPED_MAP_TEST
            } else {
                error(XQueryBundle.message("parser.error.expected-eqname-or-token", "*"))
                type = XPathElementType.ANY_MAP_TEST
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveError) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            mapTestMarker.done(type)
            return true
        }
        return false
    }

    private fun parseArrayTest(): Boolean {
        val arrayTestMarker = matchTokenTypeWithMarker(XPathTokenType.K_ARRAY)
        if (arrayTestMarker != null) {
            var haveError = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                arrayTestMarker.rollbackTo()
                return false
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (builder.matchTokenType(XPathTokenType.STAR)) {
                type = XPathElementType.ANY_ARRAY_TEST
            } else if (parseSequenceType()) {
                type = XPathElementType.TYPED_ARRAY_TEST
            } else {
                error(XQueryBundle.message("parser.error.expected-either", "*", "SequenceType"))
                type = XPathElementType.ANY_ARRAY_TEST
                haveError = true
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveError) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            arrayTestMarker.done(type)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: TypeDeclaration :: KindTest

    @Suppress("Reformat") // Kotlin formatter bug: https://youtrack.jetbrains.com/issue/KT-22518
    override fun parseKindTest(builder: PsiBuilder): Boolean {
        return (
            super.parseKindTest(builder) ||
            parseBinaryTest() ||
            parseSchemaKindTest() != ParseStatus.NOT_MATCHED ||
            parseJsonKindTest() != ParseStatus.NOT_MATCHED
        )
    }

    override fun parseAnyKindTest(builder: PsiBuilder): Boolean {
        val marker = builder.matchTokenTypeWithMarker(XPathTokenType.K_NODE)
        if (marker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                marker.rollbackTo()
                return false
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseStringLiteral(builder)) { // MarkLogic 8.0
                type = XQueryElementType.NAMED_KIND_TEST
            } else {
                builder.matchTokenType(XPathTokenType.STAR) // MarkLogic 8.0
                type = XPathElementType.ANY_KIND_TEST
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                builder.error(XPathBundle.message("parser.error.expected", ")"))
            }

            marker.done(type)
            return true
        }
        return false
    }

    override fun parseDocumentTest(builder: PsiBuilder): Boolean {
        val marker = builder.matchTokenTypeWithMarker(XPathTokenType.K_DOCUMENT_NODE)
        if (marker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                marker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (
                parseElementTest(builder) ||
                parseSchemaElementTest(builder) ||
                parseAnyArrayNodeTest() != ParseStatus.NOT_MATCHED ||
                parseAnyMapNodeTest() != ParseStatus.NOT_MATCHED
            ) {
                //
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                builder.error(XPathBundle.message("parser.error.expected", ")"))
            }

            marker.done(XPathElementType.DOCUMENT_TEST)
            return true
        }
        return false
    }

    override fun parseTextTest(builder: PsiBuilder): Boolean {
        val marker = builder.matchTokenTypeWithMarker(XPathTokenType.K_TEXT)
        if (marker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                marker.rollbackTo()
                return false
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseStringLiteral(builder)) {
                type = XQueryElementType.NAMED_TEXT_TEST
            } else {
                type = XPathElementType.ANY_TEXT_TEST
                builder.errorOnTokenType(XPathTokenType.STAR, XQueryBundle.message("parser.error.expected-either", "StringLiteral", ")")) // MarkLogic 8.0
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                builder.error(XPathBundle.message("parser.error.expected", ")"))
            }

            marker.done(type)
            return true
        }
        return false
    }

    override fun parseAttributeTest(builder: PsiBuilder): Boolean {
        val marker = builder.matchTokenTypeWithMarker(XPathTokenType.K_ATTRIBUTE)
        if (marker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                marker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseAttribNameOrWildcard(builder)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (builder.matchTokenType(XPathTokenType.COMMA)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseEQNameOrWildcard(builder, XPathElementType.TYPE_NAME, false)) {
                        builder.error(XPathBundle.message("parser.error.expected-eqname"))
                        haveErrors = true
                    }
                } else if (
                    builder.tokenType !== XPathTokenType.PARENTHESIS_CLOSE &&
                    builder.tokenType !== XQueryTokenType.K_EXTERNAL
                ) {
                    builder.error(XPathBundle.message("parser.error.expected", ","))
                    haveErrors = true
                    parseEQNameOrWildcard(builder, XPathElementType.TYPE_NAME, false)
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                builder.error(XPathBundle.message("parser.error.expected", ")"))
            }

            marker.done(XPathElementType.ATTRIBUTE_TEST)
            return true
        }
        return false
    }

    override fun parseElementTest(builder: PsiBuilder): Boolean {
        val marker = builder.matchTokenTypeWithMarker(XPathTokenType.K_ELEMENT)
        if (marker != null) {
            var haveErrors = false

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                marker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (parseElementNameOrWildcard(builder)) {
                parseWhiteSpaceAndCommentTokens(builder)
                if (builder.matchTokenType(XPathTokenType.COMMA)) {
                    parseWhiteSpaceAndCommentTokens(builder)
                    if (!parseEQNameOrWildcard(builder, XPathElementType.TYPE_NAME, false)) {
                        builder.error(XPathBundle.message("parser.error.expected-eqname"))
                        haveErrors = true
                    }

                    parseWhiteSpaceAndCommentTokens(builder)
                    builder.matchTokenType(XPathTokenType.OPTIONAL)
                } else if (
                    builder.tokenType !== XPathTokenType.PARENTHESIS_CLOSE &&
                    builder.tokenType !== XQueryTokenType.K_EXTERNAL
                ) {
                    builder.error(XPathBundle.message("parser.error.expected", ","))
                    haveErrors = true
                    parseEQNameOrWildcard(builder, XPathElementType.TYPE_NAME, false)
                }
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE) && !haveErrors) {
                builder.error(XPathBundle.message("parser.error.expected", ")"))
            }

            marker.done(XPathElementType.ELEMENT_TEST)
            return true
        }
        return false
    }

    private fun parseBinaryTest(): Boolean {
        val binaryTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_BINARY)
        if (binaryTestMarker != null) {
            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                binaryTestMarker.rollbackTo()
                return false
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
            }

            binaryTestMarker.done(XQueryElementType.BINARY_TEST)
            return true
        }
        return false
    }

    // endregion
    // region Grammar :: TypeDeclaration :: KindTest :: SchemaKindTest

    private fun parseSchemaKindTest(): ParseStatus {
        var status = parseAttributeDeclTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseComplexTypeTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseElementDeclTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseSchemaComponentTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseSchemaFacetTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseSchemaParticleTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseSchemaRootTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseSchemaTypeTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseSimpleTypeTest()
        return status
    }

    private fun parseAttributeDeclTest(): ParseStatus {
        val attributeDeclTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_ATTRIBUTE_DECL)
        if (attributeDeclTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                attributeDeclTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            attributeDeclTestMarker.done(XQueryElementType.ATTRIBUTE_DECL_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseComplexTypeTest(): ParseStatus {
        val attributeDeclTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_COMPLEX_TYPE)
        if (attributeDeclTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                attributeDeclTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            attributeDeclTestMarker.done(XQueryElementType.COMPLEX_TYPE_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseElementDeclTest(): ParseStatus {
        val elementDeclTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_ELEMENT_DECL)
        if (elementDeclTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                elementDeclTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            elementDeclTestMarker.done(XQueryElementType.ELEMENT_DECL_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseSchemaComponentTest(): ParseStatus {
        val schemaComponentTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SCHEMA_COMPONENT)
        if (schemaComponentTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                schemaComponentTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            schemaComponentTestMarker.done(XQueryElementType.SCHEMA_COMPONENT_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseSchemaFacetTest(): ParseStatus {
        val schemaFacetTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SCHEMA_FACET)
        if (schemaFacetTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                schemaFacetTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            schemaFacetTestMarker.done(XQueryElementType.SCHEMA_FACET_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseSchemaParticleTest(): ParseStatus {
        val schemaParticleTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SCHEMA_PARTICLE)
        if (schemaParticleTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                schemaParticleTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            schemaParticleTestMarker.done(XQueryElementType.SCHEMA_PARTICLE_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseSchemaRootTest(): ParseStatus {
        val schemaRootTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SCHEMA_ROOT)
        if (schemaRootTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                schemaRootTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            schemaRootTestMarker.done(XQueryElementType.SCHEMA_ROOT_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseSchemaTypeTest(): ParseStatus {
        val schemaTypeTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SCHEMA_TYPE)
        if (schemaTypeTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                schemaTypeTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            schemaTypeTestMarker.done(XQueryElementType.SCHEMA_TYPE_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseSimpleTypeTest(): ParseStatus {
        val simpleTypeTestMarker = matchTokenTypeWithMarker(XQueryTokenType.K_SIMPLE_TYPE)
        if (simpleTypeTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                simpleTypeTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            simpleTypeTestMarker.done(XQueryElementType.SIMPLE_TYPE_TEST)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    // endregion
    // region Grammar :: TypeDeclaration :: KindTest :: JsonKindTest

    private fun parseJsonKindTest(): ParseStatus {
        var status = parseArrayNodeTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseBooleanNodeTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseNullNodeTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseNumberNodeTest()
        if (status == ParseStatus.NOT_MATCHED) status = parseMapNodeTest()
        return status
    }

    private fun parseAnyArrayNodeTest(): ParseStatus {
        return parseArrayNodeTest(true)
    }

    private fun parseArrayNodeTest(isAnyOnly: Boolean = false): ParseStatus {
        val arrayNodeTestMarker = matchTokenTypeWithMarker(XPathTokenType.K_ARRAY_NODE)
        if (arrayNodeTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                arrayNodeTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (isAnyOnly && getTokenType() !== XPathTokenType.PARENTHESIS_CLOSE) {
                error(XPathBundle.message("parser.error.expected", ")"))
                type = XQueryElementType.ANY_ARRAY_NODE_TEST
                status = ParseStatus.MATCHED_WITH_ERRORS

                // array-node() tests in a document-node test do not allow `StringLiteral` or `*`
                // tokens, but accept them here to recover when used incorrectly.
                parseStringLiteral(builder)
                builder.matchTokenType(XPathTokenType.STAR)
            } else if (parseStringLiteral(builder)) {
                type = XQueryElementType.NAMED_ARRAY_NODE_TEST
            } else if (errorOnTokenType(XPathTokenType.STAR, XQueryBundle.message("parser.error.expected-either", "StringLiteral", ")"))) {
                type = XQueryElementType.ANY_ARRAY_NODE_TEST
                status = ParseStatus.MATCHED_WITH_ERRORS
            } else {
                type = XQueryElementType.ANY_ARRAY_NODE_TEST
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            arrayNodeTestMarker.done(type)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseBooleanNodeTest(): ParseStatus {
        val booleanNodeTestMarker = matchTokenTypeWithMarker(XPathTokenType.K_BOOLEAN_NODE)
        if (booleanNodeTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                booleanNodeTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseStringLiteral(builder)) {
                type = XQueryElementType.NAMED_BOOLEAN_NODE_TEST
            } else if (getTokenType() !== XPathTokenType.PARENTHESIS_CLOSE) {
                errorOnTokenType(XPathTokenType.STAR, XQueryBundle.message("parser.error.expected-either", "StringLiteral", ")"))
                type = XQueryElementType.ANY_BOOLEAN_NODE_TEST
                status = ParseStatus.MATCHED_WITH_ERRORS
            } else {
                type = XQueryElementType.ANY_BOOLEAN_NODE_TEST
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            booleanNodeTestMarker.done(type)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseAnyMapNodeTest(): ParseStatus {
        return parseMapNodeTest(true)
    }

    private fun parseMapNodeTest(isAnyOnly: Boolean = false): ParseStatus {
        val mapNodeTestMarker = matchTokenTypeWithMarker(XPathTokenType.K_OBJECT_NODE)
        if (mapNodeTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                mapNodeTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (isAnyOnly && getTokenType() !== XPathTokenType.PARENTHESIS_CLOSE) {
                error(XPathBundle.message("parser.error.expected", ")"))
                type = XQueryElementType.ANY_MAP_NODE_TEST
                status = ParseStatus.MATCHED_WITH_ERRORS

                // object-node() tests in a document-node test do not allow `StringLiteral` or `*`
                // tokens, but accept them here to recover when used incorrectly.
                parseStringLiteral(builder)
                builder.matchTokenType(XPathTokenType.STAR)
            } else if (parseStringLiteral(builder)) {
                type = XQueryElementType.NAMED_MAP_NODE_TEST
            } else if (errorOnTokenType(XPathTokenType.STAR, XQueryBundle.message("parser.error.expected-either", "StringLiteral", ")"))) {
                type = XQueryElementType.ANY_MAP_NODE_TEST
                status = ParseStatus.MATCHED_WITH_ERRORS
            } else {
                type = XQueryElementType.ANY_MAP_NODE_TEST
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            mapNodeTestMarker.done(type)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseNullNodeTest(): ParseStatus {
        val nullNodeTestMarker = matchTokenTypeWithMarker(XPathTokenType.K_NULL_NODE)
        if (nullNodeTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                nullNodeTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseStringLiteral(builder)) {
                type = XQueryElementType.NAMED_NULL_NODE_TEST
            } else if (getTokenType() !== XPathTokenType.PARENTHESIS_CLOSE) {
                errorOnTokenType(XPathTokenType.STAR, XQueryBundle.message("parser.error.expected-either", "StringLiteral", ")"))
                type = XQueryElementType.ANY_NULL_NODE_TEST
                status = ParseStatus.MATCHED_WITH_ERRORS
            } else {
                type = XQueryElementType.ANY_NULL_NODE_TEST
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            nullNodeTestMarker.done(type)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    private fun parseNumberNodeTest(): ParseStatus {
        val numberNodeTestMarker = matchTokenTypeWithMarker(XPathTokenType.K_NUMBER_NODE)
        if (numberNodeTestMarker != null) {
            var status = ParseStatus.MATCHED

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_OPEN)) {
                numberNodeTestMarker.rollbackTo()
                return ParseStatus.NOT_MATCHED
            }

            val type: IElementType
            parseWhiteSpaceAndCommentTokens(builder)
            if (parseStringLiteral(builder)) {
                type = XQueryElementType.NAMED_NUMBER_NODE_TEST
            } else if (getTokenType() !== XPathTokenType.PARENTHESIS_CLOSE) {
                errorOnTokenType(XPathTokenType.STAR, XQueryBundle.message("parser.error.expected-either", "StringLiteral", ")"))
                type = XQueryElementType.ANY_NUMBER_NODE_TEST
                status = ParseStatus.MATCHED_WITH_ERRORS
            } else {
                type = XQueryElementType.ANY_NUMBER_NODE_TEST
            }

            parseWhiteSpaceAndCommentTokens(builder)
            if (!builder.matchTokenType(XPathTokenType.PARENTHESIS_CLOSE)) {
                error(XPathBundle.message("parser.error.expected", ")"))
                status = ParseStatus.MATCHED_WITH_ERRORS
            }

            numberNodeTestMarker.done(type)
            return status
        }
        return ParseStatus.NOT_MATCHED
    }

    // endregion
    // region Lexical Structure :: Terminal Symbols

    override fun parseStringLiteral(builder: PsiBuilder, type: IElementType): Boolean {
        val stringMarker = builder.matchTokenTypeWithMarker(XPathTokenType.STRING_LITERAL_START)
        while (stringMarker != null) {
            if (
                builder.matchTokenType(XPathTokenType.STRING_LITERAL_CONTENTS) ||
                builder.matchTokenType(XQueryTokenType.PREDEFINED_ENTITY_REFERENCE) ||
                builder.matchTokenType(XQueryTokenType.CHARACTER_REFERENCE) ||
                builder.matchTokenType(XPathTokenType.ESCAPED_CHARACTER)
            ) {
                //
            } else if (builder.matchTokenType(XPathTokenType.STRING_LITERAL_END)) {
                stringMarker.done(type)
                return true
            } else if (builder.matchTokenType(XQueryTokenType.PARTIAL_ENTITY_REFERENCE)) {
                error(XQueryBundle.message("parser.error.incomplete-entity"))
            } else if (
                builder.errorOnTokenType(
                    XQueryTokenType.EMPTY_ENTITY_REFERENCE,
                    XQueryBundle.message("parser.error.empty-entity")
                ) ||
                builder.matchTokenType(XPathTokenType.BAD_CHARACTER)
            ) {
                //
            } else {
                stringMarker.done(type)
                builder.error(XPathBundle.message("parser.error.incomplete-string"))
                return true
            }
        }
        return false
    }

    override fun parseWhiteSpaceAndCommentTokens(builder: PsiBuilder): Boolean {
        var skipped = false
        while (true) {
            if (
                builder.tokenType === XPathTokenType.WHITE_SPACE ||
                builder.tokenType === XQueryTokenType.XML_WHITE_SPACE
            ) {
                skipped = true
                advanceLexer()
            } else if (
                parseComment(builder) ||
                errorOnTokenType(
                    XQueryTokenType.ENTITY_REFERENCE_NOT_IN_STRING,
                    XQueryBundle.message("parser.error.misplaced-entity")
                )
            ) {
                skipped = true
            } else {
                return skipped
            }
        }
    }

    // endregion
    // region Lexical Structure :: Terminal Symbols :: EQName

    override val URI_QUALIFIED_NAME: IElementType = XQueryElementType.URI_QUALIFIED_NAME
    override val BRACED_URI_LITERAL: IElementType = XQueryElementType.BRACED_URI_LITERAL

    override fun parseBracedURILiteral(builder: PsiBuilder): Boolean {
        val marker = builder.matchTokenTypeWithMarker(XPathTokenType.BRACED_URI_LITERAL_START)
        while (marker != null) {
            if (
                builder.matchTokenType(XPathTokenType.STRING_LITERAL_CONTENTS) ||
                builder.matchTokenType(XQueryTokenType.PREDEFINED_ENTITY_REFERENCE) ||
                builder.matchTokenType(XQueryTokenType.CHARACTER_REFERENCE)
            ) {
                //
            } else if (builder.matchTokenType(XPathTokenType.BRACED_URI_LITERAL_END)) {
                marker.done(XQueryElementType.BRACED_URI_LITERAL)
                return true
            } else if (builder.matchTokenType(XQueryTokenType.PARTIAL_ENTITY_REFERENCE)) {
                builder.error(XQueryBundle.message("parser.error.incomplete-entity"))
            } else if (
                builder.errorOnTokenType(XQueryTokenType.EMPTY_ENTITY_REFERENCE, XQueryBundle.message("parser.error.empty-entity")) ||
                builder.matchTokenType(XPathTokenType.BAD_CHARACTER)
            ) {
                //
            } else {
                marker.done(XQueryElementType.BRACED_URI_LITERAL)
                builder.error(XPathBundle.message("parser.error.incomplete-braced-uri-literal"))
                return true
            }
        }
        return false
    }

    // endregion
    // region Lexical Structure :: Terminal Symbols :: QName

    override val NCNAME: IElementType = XQueryElementType.NCNAME
    override val QNAME: IElementType = XQueryElementType.QNAME

    private fun parseNCName(type: IElementType): Boolean {
        if (getTokenType() is INCNameType) {
            val ncnameMarker = mark()
            advanceLexer()
            ncnameMarker.done(type)
            return true
        }
        return false
    }

    override fun parseQNameSeparator(builder: PsiBuilder, type: IElementType?): Boolean {
        if (
            builder.tokenType === XPathTokenType.QNAME_SEPARATOR ||
            builder.tokenType === XQueryTokenType.XML_ATTRIBUTE_QNAME_SEPARATOR ||
            builder.tokenType === XQueryTokenType.XML_TAG_QNAME_SEPARATOR
        ) {
            if (type === XQueryElementType.NCNAME || type === XQueryElementType.PREFIX) {
                val errorMarker = mark()
                advanceLexer()
                errorMarker.error(XPathBundle.message("parser.error.expected-ncname-not-qname"))
            } else if (type != null) {
                advanceLexer()
            }
            return true
        }
        return false
    }

    // endregion
}
