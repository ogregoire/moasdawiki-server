/*
 * MoasdaWiki Server
 *
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3 as
 * published by the Free Software Foundation (AGPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.moasdawiki.service.render;

import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Formats YAML code with syntax highlighting in HTML.
 * <p>
 * For syntax definition see
 * https://yaml.org/
 * <p>
 * Not thread-safe!
 */
public class YamlFormatter {

    /**
     * Text to be formatted.
     */
    @NotNull
    private final String content;

    /**
     * Number of characters read from content.
     */
    private int readCount;

    /**
     * Current position in a line (after line break)
     */
    private int posInLine;

    /**
     * Indention of the last key
     */
    private int indentionOfLastKey;

    /**
     * Expected token type on next {@link #nextToken()} call.
     */
    @NotNull
    private TokenType nextTokenType;

    /**
     * Constructor.
     */
    public YamlFormatter(@NotNull String content) {
        this.content = content;
        this.readCount = 0;
        this.posInLine = 0;
        this.indentionOfLastKey = 0;
        this.nextTokenType = TokenType.KEY;
    }

    /**
     * Format Properties.
     */
    @NotNull
    public String format() {
        StringBuilder sb = new StringBuilder();
        Token token;
        while ((token = nextToken()) != null) {
            switch (token.tokenType) {
                case LINE_BREAK:
                    sb.append("<br>\n");
                    break;
                case COMMENT:
                    sb.append("<span class=\"code-yaml-comment\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case DOCUMENT_SEPARATOR:
                    sb.append("<span class=\"code-yaml-document-separator\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case SPECIAL_CHARACTER:
                    sb.append("<span class=\"code-yaml-special-character\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case KEY:
                    sb.append("<span class=\"code-yaml-key\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case VALUE:
                    sb.append("<span class=\"code-yaml-value\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case MULTILINE_TEXT:
                    sb.append("<span class=\"code-yaml-multiline-text\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                default:
                    sb.append(escapeAndFormatHtml(token.tokenStr));
            }
        }
        return sb.toString();
    }

    @Nullable
    @Contract(pure = true)
    private String escapeAndFormatHtml(@Nullable String text) {
        text = EscapeUtils.escapeHtml(text);
        if (text != null) {
            text = text.replaceAll("\\s", "&nbsp;");
        }
        return text;
    }

    /**
     * Return next token.
     * <p>
     * Line breaks result in a '\n' character, '\r' will be removed.
     *
     * @return <code>null</code> --> no more token available.
     */
    @Nullable
    private Token nextToken() {
        StringBuilder tokenText = null;
        TokenType tokenType = null;

        while (readCount < content.length()) {
            char ch = content.charAt(readCount);
            readCount++;
            posInLine++;

            //noinspection StatementWithEmptyBody
            if (ch == '\r') {
                // ignore '\r' character, line breaks are represented by '\n'
            }

            // inside comment
            else if (tokenText != null && tokenType == TokenType.COMMENT) {
                if (ch == '\n') {
                    // end of comment
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.KEY;
                    return new Token(tokenText.toString(), TokenType.COMMENT);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside of key
            else if (tokenText != null && tokenType == TokenType.KEY) {
                if (ch == ':' || ch == '\n') {
                    // end of key
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.SPECIAL_CHARACTER;
                    return new Token(tokenText.toString(), TokenType.KEY);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside of value of a key
            else if (tokenText != null && tokenType == TokenType.VALUE) {
                if (ch == '\n') {
                    // end of value
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.KEY;
                    return new Token(tokenText.toString(), TokenType.VALUE);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside of value of multiline text
            else if (tokenText != null && tokenType == TokenType.MULTILINE_TEXT) {
                if (ch == '\n') {
                    // end of line
                    readCount--; // don't consume next character yet
                    if (multilineTextContinuesInNextLine()) {
                        // multiline text continues in next line
                        nextTokenType = TokenType.MULTILINE_TEXT;
                    } else {
                        // end of multiline text
                        nextTokenType = TokenType.KEY;
                    }
                    return new Token(tokenText.toString(), TokenType.MULTILINE_TEXT);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside white-space
            else if (tokenText != null && tokenType == TokenType.WHITE_SPACE) {
                if (ch != ' ' && ch != '\t') {
                    // end of white-space
                    readCount--; // don't consume next character yet
                    return new Token(tokenText.toString(), TokenType.WHITE_SPACE);
                } else {
                    tokenText.append(ch);
                }
            }

            // line break
            else if (ch == '\n') {
                posInLine = 0;
                if (nextTokenType != TokenType.MULTILINE_TEXT) {
                    nextTokenType = TokenType.KEY;
                }
                return new Token("\n", TokenType.LINE_BREAK);
            }

            // "-"
            else if (nextTokenType == TokenType.KEY && ch == '-') {
                if (readCount + 2 <= content.length() && content.substring(readCount - 1, readCount + 2).equals("---")) {
                    // document separator, rest of line is interpreted as comment
                    readCount += 2; // consume another 2 characters
                    nextTokenType = TokenType.COMMENT;
                    return new Token("---", TokenType.DOCUMENT_SEPARATOR);
                } else {
                    // "-" before key
                    return new Token("-", TokenType.SPECIAL_CHARACTER);
                }
            }

            // begin of comment
            else if ((nextTokenType == TokenType.KEY && ch == '#') || nextTokenType == TokenType.COMMENT) {
                tokenText = new StringBuilder();
                tokenType = TokenType.COMMENT;
                tokenText.append(ch);
            }

            // ":" after key
            else if (nextTokenType == TokenType.SPECIAL_CHARACTER && ch == ':') {
                nextTokenType = TokenType.VALUE;
                return new Token(":", TokenType.SPECIAL_CHARACTER);
            }

            // multiline text continues
            else if (nextTokenType == TokenType.MULTILINE_TEXT) {
                tokenText = new StringBuilder();
                tokenType = TokenType.MULTILINE_TEXT;
                tokenText.append(ch);
            }

            // ":" without key (syntactically incorrect)
            else if (tokenText == null && ch == ':') {
                nextTokenType = TokenType.VALUE;
                return new Token(":", TokenType.SPECIAL_CHARACTER);
            }

            // begin of white-space
            else if (tokenText == null && (ch == ' ' || ch == '\t')) {
                tokenText = new StringBuilder();
                tokenType = TokenType.WHITE_SPACE;
                tokenText.append(ch);
            }

            // begin of key
            else if (nextTokenType == TokenType.KEY) {
                indentionOfLastKey = posInLine;
                tokenText = new StringBuilder();
                tokenType = TokenType.KEY;
                tokenText.append(ch);
            }

            // begin of value
            else if (nextTokenType == TokenType.VALUE) {
                if (ch == '>' || ch == '|') {
                    tokenText = new StringBuilder();
                    tokenType = TokenType.MULTILINE_TEXT;
                    tokenText.append(ch);
                } else {
                    tokenText = new StringBuilder();
                    tokenType = TokenType.VALUE;
                    tokenText.append(ch);
                }
            }
        }

        // finally close open tokens
        if (tokenText != null) {
            return new Token(tokenText.toString(), tokenType);
        } else {
            // no more token available
            return null;
        }
    }

    /**
     * Checks if the multiline text continues in the next line.
     */
    private boolean multilineTextContinuesInNextLine() {
        int localReadCount = readCount;
        int localPosInLine = 0;
        while (localReadCount < content.length()) {
            char ch = content.charAt(localReadCount);
            localReadCount++;
            localPosInLine++;
            if (!Character.isWhitespace(ch)) {
                return localPosInLine > indentionOfLastKey;
            }
        }
        return true;
    }

    /**
     * Token type
     */
    private enum TokenType {
        COMMENT, DOCUMENT_SEPARATOR, SPECIAL_CHARACTER, KEY, VALUE, MULTILINE_TEXT, WHITE_SPACE, LINE_BREAK
    }

    /**
     * Represents a single token
     */
    private static class Token {
        @NotNull
        private final String tokenStr;

        @NotNull
        private final TokenType tokenType;

        public Token(@NotNull String tokenStr, @NotNull TokenType tokenType) {
            this.tokenStr = tokenStr;
            this.tokenType = tokenType;
        }
    }
}
