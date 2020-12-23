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
 * Stellt eine Properties-Datei formatiert inkl. Syntaxhervorhebung in HTML dar.
 * Wird für das @@-Tag benötigt.
 *
 * Ist nicht Thread-safe.
 */
public class PropertiesFormatter {

	@NotNull
	private final String propertiesText;

	private int readCount;

	@NotNull
	private TokenType nextTokenType;

	/**
	 * Konstruktor.
	 */
	public PropertiesFormatter(@NotNull String propertiesText) {
		this.propertiesText = propertiesText;
		this.readCount = 0;
		this.nextTokenType = TokenType.KEY;
	}

	/**
	 * Formatiert die Properties.
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
				sb.append("<span class=\"code-properties-comment\">");
				sb.append(escapeAndFormatHtml(token.tokenStr));
				sb.append("</span>");
				break;
			case KEY:
				sb.append("<span class=\"code-properties-key\">");
				sb.append(escapeAndFormatHtml(token.tokenStr));
				sb.append("</span>");
				break;
			case DELIMITER:
				sb.append("<span class=\"code-properties-delimiter\">");
				sb.append(escapeAndFormatHtml(token.tokenStr));
				sb.append("</span>");
				break;
			case VALUE:
				sb.append("<span class=\"code-properties-value\">");
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
	 * Ein Zeilenumbruch wird stets normiert als '\n' zurückgegeben, '\r' wird entfernt.
	 * 
	 * @return <code>null</code> --> Ende erreicht.
	 */
	@SuppressWarnings("ConstantConditions")
	@Nullable
	private Token nextToken() {
		StringBuilder tokenText = null;
		TokenType tokenType = null; // nur wenn tokenText != null

		while (readCount < propertiesText.length()) {
			char ch = propertiesText.charAt(readCount);
			readCount++;
			char chLookahead1 = '\0';
			if (readCount < propertiesText.length()) {
				chLookahead1 = propertiesText.charAt(readCount);
			}

			//noinspection StatementWithEmptyBody
			if (ch == '\r') {
				// ignorieren, nur \n wird als Zeilenumbruch gewertet
			}

			// innerhalb Kommentarzeile
			else if (tokenText != null && tokenType == TokenType.COMMENT && ch == '\n') {
				// Ende der Kommentarzeile
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.KEY;
				return new Token(tokenText.toString(), TokenType.COMMENT);
			} else if (tokenText != null && tokenType == TokenType.COMMENT) {
				tokenText.append(ch);
			}

			// innerhalb Schlüssel
			else if (tokenText != null && tokenType == TokenType.KEY && (ch == ' ' || ch == '=' || ch == ':' || ch == '\n')) {
				// Schlüssel zu Ende
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.DELIMITER;
				return new Token(tokenText.toString(), TokenType.KEY);
			} else if (tokenText != null && tokenType == TokenType.KEY) {
				tokenText.append(ch);
			}

			// innerhalb Wert des Schlüssels
			else if (tokenText != null && tokenType == TokenType.VALUE && ch == '\n') {
				// Wert zu Ende
				readCount--; // nächstes Zeichen noch nicht konsumieren
				nextTokenType = TokenType.KEY;
				return new Token(tokenText.toString(), TokenType.VALUE);
			} else if (tokenText != null && tokenType == TokenType.VALUE) {
				tokenText.append(ch);
			}

			// innerhalb white-space
			else if (tokenText != null && tokenType == TokenType.WHITE_SPACE && ch != ' ' && ch != '\t') {
				// zu Ende
				readCount--; // nächstes Zeichen noch nicht konsumieren
				return new Token(tokenText.toString(), TokenType.WHITE_SPACE);
			} else if (tokenText != null && tokenType == TokenType.WHITE_SPACE) {
				tokenText.append(ch);
			}

			// Zeilenumbruch
			else if (ch == '\n') {
				return new Token("\n", TokenType.LINE_BREAK);
			}

			// Kommentarzeile beginnt
			else if (nextTokenType == TokenType.KEY && (ch == '#' || ch == ';' || ch == '/' && chLookahead1 == '/')) {
				tokenText = new StringBuilder();
				tokenType = TokenType.COMMENT;
				tokenText.append(ch);
			}

			// white-space beginnt
			else if (tokenText == null && (ch == ' ' || ch == '\t')) {
				tokenText = new StringBuilder();
				tokenType = TokenType.WHITE_SPACE;
				tokenText.append(ch);
			}

			// Schlüssel beginnt
			else if (nextTokenType == TokenType.KEY) {
				tokenText = new StringBuilder();
				tokenType = TokenType.KEY;
				tokenText.append(ch);
			}

			// Trennzeichen
			else if (nextTokenType == TokenType.DELIMITER && (ch == '=' || ch == ':')) {
				nextTokenType = TokenType.VALUE;
				return new Token("" + ch, TokenType.DELIMITER);
			}

			// Schlüssel-Wert beginnt
			else if (nextTokenType == TokenType.DELIMITER || nextTokenType == TokenType.VALUE) {
				tokenText = new StringBuilder();
				tokenType = TokenType.VALUE;
				tokenText.append(ch);
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
		COMMENT, KEY, VALUE, DELIMITER, LINE_BREAK, WHITE_SPACE
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
