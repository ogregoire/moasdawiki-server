/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
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
 * Formats a Properties file with syntax highlighting in HTML.
 *
 * Not thread-safe!
 */
public class PropertiesFormatter {

	@NotNull
	private final String propertiesText;

	private int readCount;

	@NotNull
	private TokenType nextTokenType;

	/**
	 * Constructor.
	 */
	public PropertiesFormatter(@NotNull String propertiesText) {
		this.propertiesText = propertiesText;
		this.readCount = 0;
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
	 * Return next token.
	 *
	 * Line breaks result in a '\n' character, '\r' will be removed.
	 *
	 * @return <code>null</code> --> no more token available.
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
				// ignore '\r' character, line breaks are represented by '\n'
			}

			// inside comment
			else if (tokenText != null && tokenType == TokenType.COMMENT && ch == '\n') {
				// end of comment
				readCount--; // don't consume next character yet
				nextTokenType = TokenType.KEY;
				return new Token(tokenText.toString(), TokenType.COMMENT);
			} else if (tokenText != null && tokenType == TokenType.COMMENT) {
				tokenText.append(ch);
			}

			// inside of key
			else if (tokenText != null && tokenType == TokenType.KEY && (ch == ' ' || ch == '=' || ch == ':' || ch == '\n')) {
				// end of key
				readCount--; // don't consume next character yet
				nextTokenType = TokenType.DELIMITER;
				return new Token(tokenText.toString(), TokenType.KEY);
			} else if (tokenText != null && tokenType == TokenType.KEY) {
				tokenText.append(ch);
			}

			// inside of value of a key
			else if (tokenText != null && tokenType == TokenType.VALUE && ch == '\n') {
				// end of value
				readCount--; // don't consume next character yet
				nextTokenType = TokenType.KEY;
				return new Token(tokenText.toString(), TokenType.VALUE);
			} else if (tokenText != null && tokenType == TokenType.VALUE) {
				tokenText.append(ch);
			}

			// inside white-space
			else if (tokenText != null && tokenType == TokenType.WHITE_SPACE && ch != ' ' && ch != '\t') {
				// end of white-space
				readCount--; // don't consume next character yet
				return new Token(tokenText.toString(), TokenType.WHITE_SPACE);
			} else if (tokenText != null && tokenType == TokenType.WHITE_SPACE) {
				tokenText.append(ch);
			}

			// line break
			else if (ch == '\n') {
				return new Token("\n", TokenType.LINE_BREAK);
			}

			// begin of comment
			else if (nextTokenType == TokenType.KEY && (ch == '#' || ch == ';' || ch == '/' && chLookahead1 == '/')) {
				tokenText = new StringBuilder();
				tokenType = TokenType.COMMENT;
				tokenText.append(ch);
			}

			// begin of white-space
			else if (tokenText == null && (ch == ' ' || ch == '\t')) {
				tokenText = new StringBuilder();
				tokenType = TokenType.WHITE_SPACE;
				tokenText.append(ch);
			}

			// begin of key
			else if (nextTokenType == TokenType.KEY) {
				tokenText = new StringBuilder();
				tokenType = TokenType.KEY;
				tokenText.append(ch);
			}

			// separator character
			else if (nextTokenType == TokenType.DELIMITER && (ch == '=' || ch == ':')) {
				nextTokenType = TokenType.VALUE;
				return new Token("" + ch, TokenType.DELIMITER);
			}

			// begin of value of a key
			else if (nextTokenType == TokenType.DELIMITER || nextTokenType == TokenType.VALUE) {
				tokenText = new StringBuilder();
				tokenType = TokenType.VALUE;
				tokenText.append(ch);
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
		COMMENT, KEY, VALUE, DELIMITER, LINE_BREAK, WHITE_SPACE
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
