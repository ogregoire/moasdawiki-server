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
 * Formats a Ini file with syntax highlighting in HTML.
 *
 * For syntax definition see
 * https://en.wikipedia.org/wiki/INI_file
 *
 * Not thread-safe!
 */
public class IniFormatter {

	@NotNull
	private final String iniText;

	private int readCount;

	@NotNull
	private TokenType nextTokenType;

	/**
	 * Constructor.
	 */
	public IniFormatter(@NotNull String iniText) {
		this.iniText = iniText;
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
			case SECTION_BRACKET:
				sb.append("<span class=\"code-ini-section-bracket\">");
				sb.append(escapeAndFormatHtml(token.tokenStr));
				sb.append("</span>");
				break;
			case SECTION_NAME:
				sb.append("<span class=\"code-ini-section-name\">");
				sb.append(escapeAndFormatHtml(token.tokenStr));
				sb.append("</span>");
				break;
			case COMMENT:
				sb.append("<span class=\"code-ini-comment\">");
				sb.append(escapeAndFormatHtml(token.tokenStr));
				sb.append("</span>");
				break;
			case KEY:
				sb.append("<span class=\"code-ini-key\">");
				sb.append(escapeAndFormatHtml(token.tokenStr));
				sb.append("</span>");
				break;
			case DELIMITER:
				sb.append("<span class=\"code-ini-delimiter\">");
				sb.append(escapeAndFormatHtml(token.tokenStr));
				sb.append("</span>");
				break;
			case VALUE:
				sb.append("<span class=\"code-ini-value\">");
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

		while (readCount < iniText.length()) {
			char ch = iniText.charAt(readCount);
			readCount++;

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

			// inside of section name
			else if (tokenText != null && tokenType == TokenType.SECTION_NAME) {
				if (ch == ']' || ch == '\n') {
					// end of section name
					readCount--; // don't consume next character yet
					nextTokenType = TokenType.SECTION_BRACKET;
					return new Token(tokenText.toString(), TokenType.SECTION_NAME);
				} else {
					tokenText.append(ch);
				}
			}

			// inside of key
			else if (tokenText != null && tokenType == TokenType.KEY) {
				if (ch == ' ' || ch == '=' || ch == '\n') {
					// end of key
					readCount--; // don't consume next character yet
					nextTokenType = TokenType.DELIMITER;
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
				nextTokenType = TokenType.KEY;
				return new Token("\n", TokenType.LINE_BREAK);
			}

			// section name [
			else if (nextTokenType == TokenType.KEY && (ch == '[')) {
				nextTokenType = TokenType.SECTION_NAME;
				return new Token("[", TokenType.SECTION_BRACKET);
			}

			// begin of section name
			else if (nextTokenType == TokenType.SECTION_NAME && (ch != ']')) {
				tokenText = new StringBuilder();
				tokenType = TokenType.SECTION_NAME;
				tokenText.append(ch);
			}

			// section name [
			else if ((nextTokenType == TokenType.SECTION_BRACKET || nextTokenType == TokenType.SECTION_NAME) && (ch == ']')) {
				nextTokenType = TokenType.KEY;
				return new Token("]", TokenType.SECTION_BRACKET);
			}

			// begin of comment
			else if (nextTokenType == TokenType.KEY && (ch == '#' || ch == ';')) {
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
		COMMENT, SECTION_NAME, SECTION_BRACKET, KEY, VALUE, DELIMITER, LINE_BREAK, WHITE_SPACE
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
