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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Formats Java code with syntax highlighting in HTML.
 *
 * Not thread-safe!
 */
public class JavaFormatter {

    static final String[] KEYWORDS = {"return", "if", "else", "switch", "case", "default", "for", "while", "do", "break", "continue", "try", "catch",
            "finally", "throw", "new", "instanceof", "void", "public", "protected", "private", "static", "final", "class", "interface"};
    private static final Set<String> KEYWORDS_SET = new HashSet<>(Arrays.asList(KEYWORDS));

    @NotNull
    private final String codeText;

    private int readCount;
	private boolean insideMultilineComment;

    /**
     * Constructor.
     */
    public JavaFormatter(@NotNull String codeText) {
        this.codeText = codeText;
        this.readCount = 0;
        this.insideMultilineComment = false;
    }

    /**
     * Format Java code.
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
                    sb.append("<span class=\"code-java-comment\">");
                    sb.append(escapeAndFormatHtml(token.tokenStr));
                    sb.append("</span>");
                    break;
                case IDENTIFIER:
                    if (KEYWORDS_SET.contains(token.tokenStr)) {
                        sb.append("<span class=\"code-java-keyword\">");
                        sb.append(escapeAndFormatHtml(token.tokenStr));
                        sb.append("</span>");
                    } else {
                        sb.append(token.tokenStr);
                    }
                    break;
                case STRING:
                    sb.append("<span class=\"code-java-string\">");
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
    private String escapeAndFormatHtml(@Nullable String text) {
        text = EscapeUtils.escapeHtml(text);
        if (text != null) {
            text = text.replaceAll("\\s", "&nbsp;");
        }
        return text;
    }

    /**
     * Return next token.
	 *
     * Identifiers and comments are handled as a single token.
     * Line breaks result in a '\n' character, '\r' will be removed.
     *
     * @return <code>null</code> --> no more token available.
     */
    @Nullable
    private Token nextToken() {
        StringBuilder identifier = null;
        StringBuilder comment = null;
        StringBuilder string = null;
        char stringQuote = '\0'; // '"' only if string != null

        while (readCount < codeText.length()) {
            char ch = codeText.charAt(readCount);
            readCount++;
            char chLookahead = '\0';
            if (readCount < codeText.length()) {
                chLookahead = codeText.charAt(readCount);
            }

            //noinspection StatementWithEmptyBody
            if (ch == '\r') {
                // ignore '\r' character, line breaks are represented by '\n'
            }

            // inside Java identifier
            else if (identifier != null && !Character.isJavaIdentifierPart(ch)) {
                // end of identifier
                readCount--; // don't consume next character yet
                return new Token(identifier.toString(), TokenType.IDENTIFIER);
            }

            // inside Java String
            else if (string != null && ch == '\n') {
                // String stops at line end
                readCount--; // don't consume line break yet
                return new Token(string.toString(), TokenType.STRING);
            } else if (string != null && ch == stringQuote) {
                // end of String
                string.append(ch);
                return new Token(string.toString(), TokenType.STRING);
            } else if (string != null) {
                string.append(ch);
                if (ch == '\\' && chLookahead != '\0') {
                    string.append(chLookahead);
                    readCount++; // consume also next character
                }
            }

            // inside comment
            else if (insideMultilineComment && ch == '*' && chLookahead == '/') {
                // end of multi-line comment
                if (comment == null) {
                    comment = new StringBuilder();
                }
                comment.append("*/");
                readCount++; // consume also second character
                insideMultilineComment = false;
                return new Token(comment.toString(), TokenType.COMMENT);
            } else if (insideMultilineComment && comment != null && ch == '\n') {
                // end of line
                readCount--; // don't consume line break yet
                return new Token(comment.toString(), TokenType.COMMENT);
            } else if (insideMultilineComment && ch != '\n') {
                // multi-line comment continues
                if (comment == null) {
                    comment = new StringBuilder();
                }
                comment.append(ch);
            } else if (!insideMultilineComment && comment != null && ch == '\n') {
                // end of single-line comment
                readCount--; // don't consume line break yet
                return new Token(comment.toString(), TokenType.COMMENT);
            } else if (!insideMultilineComment && comment != null) {
                // single-line comment continues
                comment.append(ch);
            }

            // begin of comment
            else if (ch == '/' && chLookahead == '*') {
                // begin of multi-line comment
                //noinspection ConstantConditions
                if (comment == null) {
                    comment = new StringBuilder();
                }
                comment.append("/*");
                readCount++; // consume also second character
                insideMultilineComment = true;
            } else if (ch == '/' && chLookahead == '/') {
                // begin of single-line comment
                //noinspection ConstantConditions
                if (comment == null) {
                    comment = new StringBuilder();
                }
                comment.append(ch);
            }

            // begin of Java identifier
            else if (Character.isJavaIdentifierPart(ch)) {
                if (identifier == null) {
                    identifier = new StringBuilder();
                }
                identifier.append(ch);
            }

            // begin of Java String
            else if (ch == '\"' || ch == '\'') {
                //noinspection ConstantConditions
                if (string == null) {
                    string = new StringBuilder();
                    stringQuote = ch;
                }
                string.append(ch);
            }

            // other characters
            else if (ch == '\n') {
                return new Token("\n", TokenType.LINE_BREAK);
            } else {
                // any special character
                return new Token("" + ch, TokenType.ANY);
            }
        }

        // finally close open tokens
        if (identifier != null) {
            return new Token(identifier.toString(), TokenType.IDENTIFIER);
        } else if (comment != null) {
            return new Token(comment.toString(), TokenType.COMMENT);
        } else if (string != null) {
            return new Token(string.toString(), TokenType.STRING);
        } else {
            // no more token available
            return null;
        }
    }

    /**
     * Token type
     */
    private enum TokenType {
        COMMENT, IDENTIFIER, STRING, LINE_BREAK, ANY
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
