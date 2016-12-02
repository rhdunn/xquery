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
package uk.co.reecedunn.intellij.plugin.xqdoc.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import uk.co.reecedunn.intellij.plugin.core.lexer.CodePointRange;

public class XQDocLexer extends LexerBase {
    private CodePointRange mTokenRange;
    private int mState;
    private int mNextState;
    private IElementType mType;

    public XQDocLexer() {
        mTokenRange = new CodePointRange();
    }

    // region States

    private static final int STATE_DEFAULT = 0;
    private static final int STATE_CONTENTS = 1;
    private static final int STATE_TAGGED_CONTENTS = 2;

    private void stateDefault() {
        int c = mTokenRange.getCodePoint();
        switch (c) {
            case CodePointRange.END_OF_BUFFER:
                mType = null;
                break;
            case '~':
                mTokenRange.match();
                mType = XQDocTokenType.XQDOC_COMMENT_MARKER;
                mNextState = STATE_CONTENTS;
                break;
            default:
                mTokenRange.seek(mTokenRange.getBufferEnd());
                mType = XQDocTokenType.COMMENT_CONTENTS;
                break;
        }
    }

    private void stateContents() {
        int c = mTokenRange.getCodePoint();
        switch (c) {
            case CodePointRange.END_OF_BUFFER:
                mType = null;
                break;
            case '\n': // U+000A
                mTokenRange.match();

                c = mTokenRange.getCodePoint();
                while (c == ' ' || c == '\t') { // U+0020 || U+0009
                    mTokenRange.match();
                    c = mTokenRange.getCodePoint();
                }

                if (c == ':') {
                    mTokenRange.match();
                }

                mType = XQDocTokenType.TRIM;
                break;
            case '@':
                mTokenRange.match();
                mType = XQDocTokenType.TAG_MARKER;
                mNextState = STATE_TAGGED_CONTENTS;
                break;
            default:
                while (true) switch (c) {
                    case CodePointRange.END_OF_BUFFER:
                    case '\n': // U+000A
                    case '@':
                        mType = XQDocTokenType.CONTENTS;
                        return;
                    default:
                        mTokenRange.match();
                        c = mTokenRange.getCodePoint();
                        break;
                }
        }
    }

    private void stateTaggedContents() {
        int c = mTokenRange.getCodePoint();
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
            while ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                mTokenRange.match();
                c = mTokenRange.getCodePoint();
            }
            mType = XQDocTokenType.TAG;
        } else {
            stateContents();
            mNextState = STATE_CONTENTS;
        }
    }

    // endregion
    // region Lexer

    @Override
    public final void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        mTokenRange.start(buffer, startOffset, endOffset);
        mNextState = initialState;
        advance();
    }

    @Override
    public final void advance() {
        mTokenRange.flush();
        mState = mNextState;
        switch (mState) {
            case STATE_DEFAULT:
                stateDefault();
                break;
            case STATE_CONTENTS:
                stateContents();
                break;
            case STATE_TAGGED_CONTENTS:
                stateTaggedContents();
                break;
            default:
                throw new AssertionError("Invalid state: " + mState);
        }
    }

    @Override
    public final int getState() {
        return mState;
    }

    @Override
    public final IElementType getTokenType() {
        return mType;
    }

    @Override
    public final int getTokenStart() {
        return mTokenRange.getStart();
    }

    @Override
    public final int getTokenEnd() {
        return mTokenRange.getEnd();
    }

    @Override
    @SuppressWarnings("NullableProblems") // jacoco Code Coverage reports an unchecked branch when @NotNull is used.
    public CharSequence getBufferSequence() {
        return mTokenRange.getBufferSequence();
    }

    @Override
    public final int getBufferEnd() {
        return mTokenRange.getBufferEnd();
    }

    // endregion
}
