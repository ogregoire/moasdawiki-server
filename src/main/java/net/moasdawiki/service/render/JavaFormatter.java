/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2020 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.moasdawiki.service.render;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stellt Java-Code formatiert inkl. Syntaxhervorhebung in HTML dar.
 * Wird für das @@-Tag benötigt.
 *
 * Ist nicht Thread-safe.
 *
 * @author Herbert Reiter
 */
public class JavaFormatter {

    static final String[] KEYWORDS = {"return", "if", "else", "switch", "case", "default", "for", "while", "do", "break", "continue", "try", "catch",
            "finally", "throw", "new", "instanceof", "void", "public", "protected", "private", "static", "final", "class", "interface"};
    private static final Set<String> KEYWORDS_SET = new HashSet<>(Arrays.asList(KEYWORDS));

    @NotNull
    private final String codeText;

    private int readCount;

	/**
	 * Mehrzeiliger Kommentar?
	 */
	private boolean insideMultilineComment;

    /**
     * Konstruktor.
     */
    public JavaFormatter(@NotNull String codeText) {
        this.codeText = codeText;
        this.readCount = 0;
        this.insideMultilineComment = false;
    }

    /**
     * Formatiert den Java-Code.
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
     * Liefert das nächste Token.
	 *
	 * Bezeichner und Kommentare werden jeweils als ein Token behandelt.
	 * Ein Zeilenumbruch wird stets normiert als '\n' zurückgegeben, '\r' wird entfernt.
     *
     * @return <code>null</code> --> Ende erreicht.
     */
    @Nullable
    private Token nextToken() {
        StringBuilder identifier = null;
        StringBuilder comment = null;
        StringBuilder string = null;
        char stringQuote = '\0'; // '/", nur wenn string != null

        while (readCount < codeText.length()) {
            char ch = codeText.charAt(readCount);
            readCount++;
            char chLookahead = '\0';
            if (readCount < codeText.length()) {
                chLookahead = codeText.charAt(readCount);
            }

            //noinspection StatementWithEmptyBody
            if (ch == '\r') {
                // ignorieren, nur \n wird als Zeilenumbruch gewertet
            }

            // innerhalb Java-Bezeichner
            else if (identifier != null && !Character.isJavaIdentifierPart(ch)) {
                // Bezeichner ist zu Ende
                readCount--; // nächstes Zeichen noch nicht konsumieren
                return new Token(identifier.toString(), TokenType.IDENTIFIER);
            }

            // innerhalb Java-String
            else if (string != null && ch == '\n') {
                // String muss am Zeilenende abgebrochen werden
                readCount--; // Zeilenumbruch noch nicht konsumieren
                return new Token(string.toString(), TokenType.STRING);
            } else if (string != null && ch == stringQuote) {
                // String ist zu Ende
                string.append(ch);
                return new Token(string.toString(), TokenType.STRING);
            } else if (string != null) {
                string.append(ch);
                if (ch == '\\' && chLookahead != '\0') {
                    string.append(chLookahead);
                    readCount++; // nächstes Zeichen auch konsumieren
                }
            }

            // innerhalb Kommentar
            else if (insideMultilineComment && ch == '*' && chLookahead == '/') {
                // Ende des mehrzeiligen Kommentars erreicht
                if (comment == null) {
                    comment = new StringBuilder();
                }
                comment.append("*/");
                readCount++; // zweites Zeichen konsumieren
                insideMultilineComment = false;
                return new Token(comment.toString(), TokenType.COMMENT);
            } else if (insideMultilineComment && comment != null && ch == '\n') {
                // Zeilenende erreicht
                readCount--; // Zeilenumbruch noch nicht konsumieren
                return new Token(comment.toString(), TokenType.COMMENT);
            } else if (insideMultilineComment && ch != '\n') {
                // weiter im mehrzeiligen Kommentar
                if (comment == null) {
                    comment = new StringBuilder();
                }
                comment.append(ch);
            } else if (!insideMultilineComment && comment != null && ch == '\n') {
                // Ende des einzeiligen Kommentars
                readCount--; // Zeilenumbruch noch nicht konsumieren
                return new Token(comment.toString(), TokenType.COMMENT);
            } else if (!insideMultilineComment && comment != null) {
                // weiter im einzeiligen Kommentar
                comment.append(ch);
            }

            // Kommentar beginnt
            else if (ch == '/' && chLookahead == '*') {
                // Beginn eines mehrzeiligen Kommentars
                //noinspection ConstantConditions
                if (comment == null) {
                    comment = new StringBuilder();
                }
                comment.append("/*");
                readCount++; // zweites Zeichen konsumieren
                insideMultilineComment = true;
            } else if (ch == '/' && chLookahead == '/') {
                // Beginn eines einzeiligen Kommentars
                //noinspection ConstantConditions
                if (comment == null) {
                    comment = new StringBuilder();
                }
                comment.append(ch);
            }

            // Java-Bezeichner beginnt
            else if (Character.isJavaIdentifierPart(ch)) {
                if (identifier == null) {
                    identifier = new StringBuilder();
                }
                identifier.append(ch);
            }

            // Java-String beginnt
            else if (ch == '\"' || ch == '\'') {
                //noinspection ConstantConditions
                if (string == null) {
                    string = new StringBuilder();
                    stringQuote = ch;
                }
                string.append(ch);
            }

            // sonstige Zeichen
            else if (ch == '\n') {
                return new Token("\n", TokenType.LINE_BREAK);
            } else {
                // irgendein Sonderzeichen
                return new Token("" + ch, TokenType.ANY);
            }
        }

        // Ende erreicht, offene Token abschließen
        if (identifier != null) {
            return new Token(identifier.toString(), TokenType.IDENTIFIER);
        } else if (comment != null) {
            return new Token(comment.toString(), TokenType.COMMENT);
        } else if (string != null) {
            return new Token(string.toString(), TokenType.STRING);
        } else {
            // kein weiteres Token mehr
            return null;
        }
    }

    /**
     * Token-Typ
     */
    private enum TokenType {
        COMMENT, IDENTIFIER, STRING, LINE_BREAK, ANY
    }

    /**
     * Enthält ein einzelnes Token.
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
