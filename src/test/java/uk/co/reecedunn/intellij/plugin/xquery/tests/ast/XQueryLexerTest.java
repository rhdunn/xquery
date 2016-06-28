/*
 * Copyright (C) 2016 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xquery.tests.ast;

import com.intellij.psi.tree.IElementType;
import junit.framework.TestCase;
import uk.co.reecedunn.intellij.plugin.xquery.LanguageLevel;
import uk.co.reecedunn.intellij.plugin.xquery.XQueryTokenType;
import uk.co.reecedunn.intellij.plugin.xquery.lexer.XQueryLexer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class XQueryLexerTest extends TestCase {
    private void matchToken(XQueryLexer lexer, String text, int state, int start, int end, IElementType type) {
        lexer.advance();
        assertThat(lexer.getTokenText(), is(text));
        assertThat(lexer.getState(), is(state));
        assertThat(lexer.getTokenStart(), is(start));
        assertThat(lexer.getTokenEnd(), is(end));
        assertThat(lexer.getTokenType(), is(type));
    }

    private void checkEmptyBuffer(LanguageLevel level) {
        XQueryLexer lexer = new XQueryLexer(level);

        lexer.start("");
        matchToken(lexer, "", 0, 0, 0, null);
    }

    public void testEmptyBuffer() {
        checkEmptyBuffer(LanguageLevel.XQUERY_1_0);
        checkEmptyBuffer(LanguageLevel.XQUERY_3_0);
        checkEmptyBuffer(LanguageLevel.XQUERY_3_1);
    }

    // XQuery 1.0 -- A.2.1 [156] S
    // XQuery 3.0 -- A.2.1 [215] S
    // XQuery 3.1 -- A.2.1 [237] S

    public void checkWhiteSpace(LanguageLevel level) {
        XQueryLexer lexer = new XQueryLexer(level);

        lexer.start(" ");
        matchToken(lexer, " ", 0, 0, 1, XQueryTokenType.WHITE_SPACE);
        matchToken(lexer, "",  0, 1, 1, null);

        lexer.start("\t");
        matchToken(lexer, "\t", 0, 0, 1, XQueryTokenType.WHITE_SPACE);
        matchToken(lexer, "",   0, 1, 1, null);

        lexer.start("\r");
        matchToken(lexer, "\r", 0, 0, 1, XQueryTokenType.WHITE_SPACE);
        matchToken(lexer, "",   0, 1, 1, null);

        lexer.start("\n");
        matchToken(lexer, "\n", 0, 0, 1, XQueryTokenType.WHITE_SPACE);
        matchToken(lexer, "",   0, 1, 1, null);

        lexer.start("   \t  \r\n ");
        matchToken(lexer, "   \t  \r\n ", 0, 0, 9, XQueryTokenType.WHITE_SPACE);
        matchToken(lexer, "",             0, 9, 9, null);
    }

    public void testWhiteSpace() {
        checkWhiteSpace(LanguageLevel.XQUERY_1_0);
        checkWhiteSpace(LanguageLevel.XQUERY_3_0);
        checkWhiteSpace(LanguageLevel.XQUERY_3_1);
    }

    // XQuery 1.0 -- A.2.1 [141] IntegerLiteral
    // XQuery 3.0 -- A.2.1 [197] IntegerLiteral
    // XQuery 3.1 -- A.2.1 [219] IntegerLiteral

    public void checkIntegerLiteral(LanguageLevel level) {
        XQueryLexer lexer = new XQueryLexer(level);

        lexer.start("1234");
        matchToken(lexer, "1234", 0, 0, 4, XQueryTokenType.INTEGER_LITERAL);
        matchToken(lexer, "",     0, 4, 4, null);
    }

    public void testIntegerLiteral() {
        checkIntegerLiteral(LanguageLevel.XQUERY_1_0);
        checkIntegerLiteral(LanguageLevel.XQUERY_3_0);
        checkIntegerLiteral(LanguageLevel.XQUERY_3_1);
    }

    // XQuery 1.0 -- A.2.1 [142] DecimalLiteral
    // XQuery 3.0 -- A.2.1 [198] DecimalLiteral
    // XQuery 3.1 -- A.2.1 [220] DecimalLiteral

    public void checkDecimalLiteral(LanguageLevel level) {
        XQueryLexer lexer = new XQueryLexer(level);

        lexer.start("47.");
        matchToken(lexer, "47.", 0, 0, 3, XQueryTokenType.DECIMAL_LITERAL);
        matchToken(lexer, "",    0, 3, 3, null);

        lexer.start("1.234");
        matchToken(lexer, "1.234", 0, 0, 5, XQueryTokenType.DECIMAL_LITERAL);
        matchToken(lexer, "",      0, 5, 5, null);

        lexer.start(".25");
        matchToken(lexer, ".25", 0, 0, 3, XQueryTokenType.DECIMAL_LITERAL);
        matchToken(lexer, "",    0, 3, 3, null);

        lexer.start(".1.2");
        matchToken(lexer, ".1", 0, 0, 2, XQueryTokenType.DECIMAL_LITERAL);
        matchToken(lexer, ".2", 0, 2, 4, XQueryTokenType.DECIMAL_LITERAL);
        matchToken(lexer, "",   0, 4, 4, null);
    }

    public void testDecimalLiteral() {
        checkDecimalLiteral(LanguageLevel.XQUERY_1_0);
        checkDecimalLiteral(LanguageLevel.XQUERY_3_0);
        checkDecimalLiteral(LanguageLevel.XQUERY_3_1);
    }
}

/**
 * References:
 *   XQuery 1.0 (2ed) -- https://www.w3.org/TR/2010/REC-xquery-20101214/
 *   XQuery 3.0       -- https://www.w3.org/TR/2014/REC-xquery-30-20140408/
 *   XQuery 3.1       -- https://www.w3.org/TR/2015/CR-xquery-31-20151217/
 */