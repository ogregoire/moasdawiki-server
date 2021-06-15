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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Formats XML and HTML code with syntax highlighting in HTML
 * <p>
 * Not thread-safe!
 */
public class XmlFormatter {

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
     * Expected token type on next {@link #nextToken()} call.
     */
    @NotNull
    private TokenType nextTokenType;

    /**
     * Constructor.
     */
    public XmlFormatter(@NotNull String content) {
        this.content = content;
        this.readCount = 0;
        this.nextTokenType = TokenType.TEXT;
    }

    /**
     * Format XML code.
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        Token token;
        while ((token = nextToken()) != null) {
            switch (token.tokenType) {
                case LINE_BREAK:
                    sb.append("<br>\n");
                    break;
                case COMMENT:
                    sb.append("<span class=\"code-xml-comment\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case SPECIAL_CHARACTER:
                    sb.append("<span class=\"code-xml-special-character\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case TAG:
                    sb.append("<span class=\"code-xml-tag\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case ATTRIBUTE_NAME:
                    sb.append("<span class=\"code-xml-attribute-name\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case ATTRIBUTE_VALUE:
                    sb.append("<span class=\"code-xml-attribute-value\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case ESCAPED_CHARACTER:
                    sb.append("<span class=\"code-xml-escaped-character\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                default:
                    sb.append(escapeAndFormatHtml(token.tokenStr));
            }
        }

        return sb.toString();
    }

    @NotNull
    private String escapeAndFormatHtml(@NotNull String text) {
        text = EscapeUtils.escapeHtml(text);
        text = text.replaceAll("\\s", "&nbsp;");
        return text;
    }

    /**
     * Return next token.
     * <p>
     * Identifiers and comments are handled as a single token.
     * Line breaks result in a '\n' character, '\r' will be removed.
     *
     * @return <code>null</code> --> no more token available.
     */
    @Nullable
    private Token nextToken() {
        StringBuilder tokenText = null;
        TokenType tokenType = null; // only if tokenText != null
        char stringQuote = '\0'; // '"' or '\''; only relevant if tokenType == ATTRIBUTE_VALUE

        while (readCount < content.length()) {
            char ch = content.charAt(readCount);
            readCount++;
            char chLookahead1 = '\0';
            if (readCount < content.length()) {
                chLookahead1 = content.charAt(readCount);
            }
            char chLookahead2 = '\0';
            if (readCount + 1 < content.length()) {
                chLookahead2 = content.charAt(readCount + 1);
            }
            char chLookahead3 = '\0';
            if (readCount + 2 < content.length()) {
                chLookahead3 = content.charAt(readCount + 2);
            }

            //noinspection StatementWithEmptyBody
            if (ch == '\r') {
                // ignore '\r' character, line breaks are represented by '\n'
            }

            // inside multi-line comment
            else if (tokenText != null && tokenType == TokenType.COMMENT) {
                if (ch == '-' && chLookahead1 == '-') {
                    // end of comment
                    tokenText.append("--");
                    readCount++; // consume also next character
                    if (chLookahead2 == '>') {
                        readCount++; // consume also next character
                        tokenText.append('>');
                    }
                    nextTokenType = TokenType.TEXT;
                    return new Token(tokenText.toString(), TokenType.COMMENT);
                } else if (ch == '\n') {
                    // line break inside comment
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.COMMENT;
                    return new Token(tokenText.toString(), TokenType.COMMENT);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside CDATA
            else if (tokenText != null && tokenType == TokenType.CDATA) {
                if (ch == '\n') {
                    // line break
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.CDATA;
                    return new Token(tokenText.toString(), TokenType.TEXT);
                } else if (tokenText.length() > 0 && (ch == ']' && chLookahead1 == ']' && chLookahead2 == '>')) {
                    // end of CDATA, return rest
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.CDATA;
                    return new Token(tokenText.toString(), TokenType.TEXT);
                } else if (tokenText.length() == 0 && ch == ']' && chLookahead1 == ']' && chLookahead2 == '>') {
                    // end of CDATA, return end token
                    tokenText.append("]]>");
                    readCount += 2; // consume also next 2 characters
                    nextTokenType = TokenType.TEXT;
                    return new Token(tokenText.toString(), TokenType.TAG);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside tag declaration
            else if (tokenText != null && tokenType == TokenType.TAG) {
                if (!Character.isJavaIdentifierStart(ch)) {
                    // end of tag name
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.ATTRIBUTE_NAME;
                    return new Token(tokenText.toString(), TokenType.TAG);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside attribute name
            else if (tokenText != null && tokenType == TokenType.ATTRIBUTE_NAME) {
                if (!Character.isJavaIdentifierPart(ch)) {
                    // end of attribute name
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.ATTRIBUTE_NAME;
                    return new Token(tokenText.toString(), TokenType.ATTRIBUTE_NAME);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside attribute value
            else if (tokenText != null && tokenType == TokenType.ATTRIBUTE_VALUE) {
                if (ch == stringQuote && stringQuote != '\0') {
                    // end of attribute value
                    tokenText.append(ch);
                    nextTokenType = TokenType.ATTRIBUTE_NAME;
                    return new Token(tokenText.toString(), TokenType.ATTRIBUTE_VALUE);
                } else if ((ch == '\n' || (ch == '/' || ch == '?' || ch == '>' || ch == ' ') && stringQuote == '\0')) {
                    // end of attribute value
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.ATTRIBUTE_NAME;
                    return new Token(tokenText.toString(), TokenType.ATTRIBUTE_VALUE);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside escape sequence
            else if (tokenText != null && tokenType == TokenType.ESCAPED_CHARACTER) {
                if (ch == ';') {
                    // end of escape sequence
                    tokenText.append(ch);
                    nextTokenType = TokenType.TEXT;
                    return new Token(tokenText.toString(), TokenType.ESCAPED_CHARACTER);
                } else if (ch == ' ' || ch == '\n') {
                    // cancel escape sequence
                    readCount--; // don't consume next character yet
                    nextTokenType = TokenType.TEXT;
                    return new Token(tokenText.toString(), TokenType.ESCAPED_CHARACTER);
                } else {
                    tokenText.append(ch);
                }
            }

            // inside normal text or between tags
            else if (tokenText != null && tokenType == TokenType.TEXT) {
                if ((ch == '<' || ch == '&' || ch == '\n'
                        || (nextTokenType == TokenType.ATTRIBUTE_NAME || nextTokenType == TokenType.ATTRIBUTE_VALUE)
                        && !Character.isWhitespace(ch))) {
                    // end of normal text
                    readCount--; // don't consume next character yet
                    return new Token(tokenText.toString(), TokenType.TEXT);
                } else {
                    tokenText.append(ch);
                }
            }

            // line break
            else if (ch == '\n') {
                return new Token("\n", TokenType.LINE_BREAK);
            }

            // end of comment
            else if (nextTokenType == TokenType.COMMENT) {
                // comment continues after line break
                tokenText = new StringBuilder();
                tokenType = TokenType.COMMENT;
                // comment is read above, to detect the end condition "-->"
                // at the beginning of a line correctly
                readCount--;
            } else if (ch == '<' && chLookahead1 == '!' && chLookahead2 == '-' && chLookahead3 == '-') {
                tokenText = new StringBuilder();
                tokenText.append("<!--");
                readCount += 3; // consume 3 more characters
                tokenType = TokenType.COMMENT;
            }

            // begin of CDATA
            else if (nextTokenType == TokenType.CDATA) {
                // CDATA continues after line break
                tokenText = new StringBuilder();
                tokenType = TokenType.CDATA;
                // CDATA is read above, to detect the end condition "]]>"
                // at the begining of a line correctly
                readCount--;
            } else if (ch == '<' && chLookahead1 == '!' && readCount + 7 < content.length() && content.startsWith("![CDATA[", readCount)) {
                readCount += 8; // consume 8 more characters
                nextTokenType = TokenType.CDATA;
                return new Token("<![CDATA[", TokenType.TAG);
            }

            // begin of tag
            else if (ch == '<' || ch == '/' || ch == '?' || ch == '!') {
                nextTokenType = TokenType.TAG;
                return new Token("" + ch, TokenType.SPECIAL_CHARACTER);
            }

            // begin of normal text
            else if (ch == '>') {
                nextTokenType = TokenType.TEXT;
                return new Token("" + ch, TokenType.SPECIAL_CHARACTER);
            }

            // begin of escape sequence
            else if (ch == '&') {
                tokenText = new StringBuilder();
                tokenText.append(ch);
                tokenType = TokenType.ESCAPED_CHARACTER;
            }

            // begin of tag name
            else if (nextTokenType == TokenType.TAG && Character.isJavaIdentifierStart(ch)) {
                tokenText = new StringBuilder();
                tokenText.append(ch);
                tokenType = TokenType.TAG;
            }

            // begin of attribute name
            else if (nextTokenType == TokenType.ATTRIBUTE_NAME && Character.isJavaIdentifierStart(ch)) {
                tokenText = new StringBuilder();
                tokenText.append(ch);
                tokenType = TokenType.ATTRIBUTE_NAME;
            }

            // begin of attribute value
            else if (nextTokenType == TokenType.ATTRIBUTE_NAME && ch == '=') {
                nextTokenType = TokenType.ATTRIBUTE_VALUE;
                return new Token("" + ch, TokenType.SPECIAL_CHARACTER);
            } else if ((nextTokenType == TokenType.ATTRIBUTE_NAME || nextTokenType == TokenType.ATTRIBUTE_VALUE) && ch != '=' && !Character.isWhitespace(ch)) {
                tokenText = new StringBuilder();
                tokenText.append(ch);
                tokenType = TokenType.ATTRIBUTE_VALUE;
                if (ch == '\'' || ch == '"') {
                    stringQuote = ch;
                } else {
                    stringQuote = '\0';
                }
            }

            // begin of normal text
            else {
                tokenText = new StringBuilder();
                tokenText.append(ch);
                tokenType = TokenType.TEXT;
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
     * Token type
     */
    private enum TokenType {
        COMMENT, SPECIAL_CHARACTER, CDATA, TAG, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, ESCAPED_CHARACTER, LINE_BREAK, TEXT
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
