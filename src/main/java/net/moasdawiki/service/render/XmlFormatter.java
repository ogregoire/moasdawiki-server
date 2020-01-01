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

import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stellt XML- und HTML-Code formatiert inkl. Syntaxhervorhebung in HTML dar.
 * Wird für das @@-Tag benötigt.<br>
 * <br>
 * Ist nicht Thread-safe.
 *
 * @author Herbert Reiter
 */
public class XmlFormatter {

	@NotNull
	private final String codeText;

	private int readCount;

	@NotNull
	private TokenType nextTokenType;

	/**
	 * Konstruktor.
	 */
	public XmlFormatter(@NotNull String codeText) {
		this.codeText = codeText;
		this.readCount = 0;
		this.nextTokenType = TokenType.TEXT;
	}

	/**
	 * Formatiert den XML-Code.
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
	 * Liefert das nächste Token. Bezeichner und Kommentare werden jeweils als
	 * ein Token behandelt. Ein Zeilenumbruch wird stets normiert als '\n'
	 * zurückgegeben, '\r' wird entfernt.
	 * 
	 * @return <code>null</code> --> Ende erreicht.
	 */
	@Nullable
	private Token nextToken() {
		StringBuilder tokenText = null;
		TokenType tokenType = null; // nur wenn tokenText != null
		char stringQuote = '\0'; // '/", nur wenn tokenType == ATTRIBUTE_VALUE

		while (readCount < codeText.length()) {
			char ch = codeText.charAt(readCount);
			readCount++;
			char chLookahead1 = '\0';
			if (readCount < codeText.length()) {
				chLookahead1 = codeText.charAt(readCount);
			}
			char chLookahead2 = '\0';
			if (readCount + 1 < codeText.length()) {
				chLookahead2 = codeText.charAt(readCount + 1);
			}
			char chLookahead3 = '\0';
			if (readCount + 2 < codeText.length()) {
				chLookahead3 = codeText.charAt(readCount + 2);
			}

			//noinspection StatementWithEmptyBody
			if (ch == '\r') {
				// ignorieren, nur \n wird als Zeilenumbruch gewertet
			}

			// innerhalb mehrzeiligem Kommentar
			else if (tokenText != null && tokenType == TokenType.COMMENT && ch == '-' && chLookahead1 == '-') {
				// Kommentar zu Ende
				tokenText.append("--");
				readCount++; // nächstes Zeichen auch konsumieren
				if (chLookahead2 == '>') {
					readCount++; // nächstes Zeichen auch konsumieren
					tokenText.append('>');
				}
				nextTokenType = TokenType.TEXT;
				return new Token(tokenText.toString(), TokenType.COMMENT);
			} else if (tokenText != null && tokenType == TokenType.COMMENT && ch == '\n') {
				// Zeilenwechsel innerhalb des Kommentars
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.COMMENT;
				return new Token(tokenText.toString(), TokenType.COMMENT);
			} else if (tokenText != null && tokenType == TokenType.COMMENT) {
				tokenText.append(ch);
			}

			// innerhalb CDATA
			else if (tokenText != null && tokenType == TokenType.CDATA && ch == '\n') {
				// Zeilenwechsel
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.CDATA;
				return new Token(tokenText.toString(), TokenType.TEXT);
			} else if (tokenText != null && tokenType == TokenType.CDATA && tokenText.length() > 0
					&& (ch == ']' && chLookahead1 == ']' && chLookahead2 == '>')) {
				// CDATA zu Ende, Resttext zurückgeben
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.CDATA;
				return new Token(tokenText.toString(), TokenType.TEXT);
			} else if (tokenText != null && tokenType == TokenType.CDATA && tokenText.length() == 0
					&& ch == ']' && chLookahead1 == ']' && chLookahead2 == '>') {
				// CDATA zu Ende, Ende-Token zurückgeben
				tokenText.append("]]>");
				readCount += 2; // nächste zwei Zeichen auch konsumieren
				nextTokenType = TokenType.TEXT;
				return new Token(tokenText.toString(), TokenType.TAG);
			} else if (tokenText != null && tokenType == TokenType.CDATA) {
				tokenText.append(ch);
			}

			// innerhalb DOCTYPE
			else if (tokenText != null && tokenType == TokenType.DOCTYPE && ch == '\n') {
				// Zeilenwechsel
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.DOCTYPE;
				return new Token(tokenText.toString(), TokenType.TAG);
			} else if (tokenText != null && tokenType == TokenType.DOCTYPE && ch == '>') {
				// DOCTYPE zu Ende
				tokenText.append(ch);
				nextTokenType = TokenType.TEXT;
				return new Token(tokenText.toString(), TokenType.TAG);
			} else if (tokenText != null && tokenType == TokenType.DOCTYPE) {
				tokenText.append(ch);
			}

			// innerhalb Tagklammer
			else if (tokenText != null && tokenType == TokenType.TAG && (ch == ' ' || ch == '\n')) {
				// Tag-Name zu Ende
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.ATTRIBUTE_NAME;
				return new Token(tokenText.toString(), TokenType.TAG);
			} else if (tokenText != null && tokenType == TokenType.TAG && ch == '>') {
				// Tag zu Ende
				tokenText.append(ch);
				nextTokenType = TokenType.TEXT;
				return new Token(tokenText.toString(), TokenType.TAG);
			} else if (tokenText != null && tokenType == TokenType.TAG && (ch == '/' || ch == '?') && chLookahead1 == '>') {
				// Tag zu Ende
				tokenText.append(ch);
				tokenText.append(chLookahead1);
				readCount++; // nächstes Zeichen auch konsumieren
				nextTokenType = TokenType.TEXT;
				return new Token(tokenText.toString(), TokenType.TAG);
			} else if (tokenText != null && tokenType == TokenType.TAG) {
				tokenText.append(ch);
			}

			// innerhalb Attributname
			else if (tokenText != null && tokenType == TokenType.ATTRIBUTE_NAME && (ch == '=' || Character.isWhitespace(ch) || ch == '\n')) {
				// Attributname zu Ende
				readCount--; // nächstes Zeichen noch nicht konsumieren
				// auf ATTRIBUTE_VALUE erst durch '=' umschalten
				nextTokenType = TokenType.ATTRIBUTE_NAME;
				return new Token(tokenText.toString(), TokenType.ATTRIBUTE_NAME);
			} else if (tokenText != null && tokenType == TokenType.ATTRIBUTE_NAME && (ch == '/' || ch == '?' || ch == '>')) {
				// Attributname zu Ende
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.TAG;
				return new Token(tokenText.toString(), TokenType.ATTRIBUTE_NAME);
			} else if (tokenText != null && tokenType == TokenType.ATTRIBUTE_NAME) {
				tokenText.append(ch);
			}

			// innerhalb Attributwert
			else if (tokenText != null && tokenType == TokenType.ATTRIBUTE_VALUE && ch == stringQuote && stringQuote != '\0') {
				// Attributwert zu Ende
				tokenText.append(ch);
				nextTokenType = TokenType.ATTRIBUTE_NAME;
				return new Token(tokenText.toString(), TokenType.ATTRIBUTE_VALUE);
			} else if (tokenText != null && tokenType == TokenType.ATTRIBUTE_VALUE
					&& (ch == '\n' || (ch == '/' || ch == '?' || ch == '>' || ch == ' ') && stringQuote == '\0')) {
				// Attributwert zu Ende
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.ATTRIBUTE_NAME;
				return new Token(tokenText.toString(), TokenType.ATTRIBUTE_VALUE);
			} else if (tokenText != null && tokenType == TokenType.ATTRIBUTE_VALUE) {
				tokenText.append(ch);
			}

			// innerhalb Escape-Sequenz
			else if (tokenText != null && tokenType == TokenType.ESCAPED_CHARACTER && ch == ';') {
				// Escape-Sequenz zu Ende
				tokenText.append(ch);
				nextTokenType = TokenType.TEXT;
				return new Token(tokenText.toString(), TokenType.ESCAPED_CHARACTER);
			} else if (tokenText != null && tokenType == TokenType.ESCAPED_CHARACTER && (ch == ' ' || ch == '\n')) {
				// Escape-Sequenz abgebrochen
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.TEXT;
				return new Token(tokenText.toString(), TokenType.ESCAPED_CHARACTER);
			} else if (tokenText != null && tokenType == TokenType.ESCAPED_CHARACTER) {
				tokenText.append(ch);
			}

			// innerhalb normalem Text oder Tag-Zwischenraum
			else if (tokenText != null && tokenType == TokenType.TEXT && (ch == '<' || ch == '&' || ch == '\n'
					|| (nextTokenType == TokenType.ATTRIBUTE_NAME || nextTokenType == TokenType.ATTRIBUTE_VALUE) && !Character.isWhitespace(ch))) {
				// normaler Text ist zu Ende
				readCount--; // nächstes Zeichen noch nicht konsumieren
				return new Token(tokenText.toString(), TokenType.TEXT);
			} else if (tokenText != null && tokenType == TokenType.TEXT) {
				tokenText.append(ch);
			}

			// Zeilenumbruch
			else if (ch == '\n') {
				return new Token("\n", TokenType.LINE_BREAK);
			}

			// Kommentar beginnt
			else if (nextTokenType == TokenType.COMMENT) {
				// Kommentar geht nach einem Zeilenwechsel weiter
				tokenText = new StringBuilder();
				tokenType = TokenType.COMMENT;
				// Kommentar selbst wird weiter oben eingelesen, damit auch die
				// Endebedingung "-->" gleich am Zeilenanfang korrekt erkannt
				// wird
				readCount--;
			} else if (ch == '<' && chLookahead1 == '!' && chLookahead2 == '-' && chLookahead3 == '-') {
				tokenText = new StringBuilder();
				tokenText.append("<!--");
				readCount += 3; // weitere 3 Zeichen konsumieren
				tokenType = TokenType.COMMENT;
			}

			// CDATA beginnt
			else if (nextTokenType == TokenType.CDATA) {
				// CDATA geht nach einem Zeilenwechsel weiter
				tokenText = new StringBuilder();
				tokenType = TokenType.CDATA;
				// CDATA selbst wird weiter oben eingelesen, damit auch die
				// Endebedingung "]]>" gleich am Zeilenanfang korrekt erkannt
				// wird
				readCount--;
			} else if (ch == '<' && chLookahead1 == '!' && readCount + 7 < codeText.length() && codeText.startsWith("![CDATA[", readCount)) {
				readCount += 8; // weitere 8 Zeichen konsumieren
				nextTokenType = TokenType.CDATA;
				return new Token("<![CDATA[", TokenType.TAG);
			}
			
			// DOCTYPE beginnt
			else if (nextTokenType == TokenType.DOCTYPE) {
				// DOCTYPE geht nach einem Zeilenwechsel weiter
				tokenText = new StringBuilder();
				tokenType = TokenType.DOCTYPE;
				// DOCTYPE selbst wird weiter oben eingelesen, damit auch die
				// Endebedingung ">" gleich am Zeilenanfang korrekt erkannt
				// wird
				readCount--;
			} else if (ch == '<' && chLookahead1 == '!' && readCount + 7 < codeText.length() && codeText.startsWith("!DOCTYPE", readCount)) {
				tokenText = new StringBuilder();
				tokenText.append("<!DOCTYPE");
				readCount += 8; // weitere 8 Zeichen konsumieren
				tokenType = TokenType.DOCTYPE;
			}
			
			// Tag beginnt
			else if (ch == '<') {
				tokenText = new StringBuilder();
				tokenText.append(ch);
				tokenType = TokenType.TAG;
			} else if ((nextTokenType == TokenType.TAG || nextTokenType == TokenType.ATTRIBUTE_NAME || nextTokenType == TokenType.ATTRIBUTE_VALUE)
					&& (ch == '/' || ch == '?' || ch == '>')) {
				tokenText = new StringBuilder();
				tokenType = TokenType.TAG;
				// Tag selbst wird weiter oben eingelesen, um doppelten Code zu
				// vermeiden
				readCount--;
			}

			// Escape-Sequenz beginnt
			else if (ch == '&') {
				tokenText = new StringBuilder();
				tokenText.append(ch);
				tokenType = TokenType.ESCAPED_CHARACTER;
			}

			// Attributname beginnt
			else if (nextTokenType == TokenType.ATTRIBUTE_NAME && Character.isJavaIdentifierStart(ch)) {
				tokenText = new StringBuilder();
				tokenText.append(ch);
				tokenType = TokenType.ATTRIBUTE_NAME;
			}

			// Attributwert beginnt
			else if (nextTokenType == TokenType.ATTRIBUTE_NAME && ch == '=') {
				tokenText = new StringBuilder();
				tokenText.append(ch);
				tokenType = TokenType.TEXT;
				nextTokenType = TokenType.ATTRIBUTE_VALUE;
			} else if (nextTokenType == TokenType.ATTRIBUTE_VALUE && ch != '=' && !Character.isWhitespace(ch)) {
				tokenText = new StringBuilder();
				tokenText.append(ch);
				tokenType = TokenType.ATTRIBUTE_VALUE;
				if (ch == '\'' || ch == '"') {
					stringQuote = ch;
				} else {
					stringQuote = '\0';
				}
			}

			// normaler Text beginnt
			else {
				tokenText = new StringBuilder();
				tokenText.append(ch);
				tokenType = TokenType.TEXT;
			}
		}

		// Ende erreicht, offene Token abschließen
		if (tokenText != null) {
			return new Token(tokenText.toString(), tokenType);
		} else {
			// kein weiteres Token mehr
			return null;
		}
	}

	/**
	 * Token-Typ
	 */
	private enum TokenType {
		COMMENT, CDATA, DOCTYPE, TAG, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, ESCAPED_CHARACTER, LINE_BREAK, TEXT
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
