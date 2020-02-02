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

package net.moasdawiki.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Helper methods to escape Strings.
 * 
 * @author Herbert Reiter
 */
public abstract class EscapeUtils {

	/**
	 * Escapes all special HTML characters that would be interpreted in HTML text and tag attributes.
	 *
	 * @param str String to escape
	 * @return Escaped string. null if str was null.
	 */
	@Contract(value = "null -> null; !null -> !null", pure = true)
	@Nullable
	public static String escapeHtml(@Nullable String str) {
		if (str == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '&') {
				sb.append("&amp;"); // muss als erstes kommen!
			} else if (c == '"') {
				sb.append("&quot;");
			} else if (c == '\'') {
				sb.append("&apos;");
			} else if (c == '<') {
				sb.append("&lt;");
			} else if (c == '>') {
				sb.append("&gt;");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Escapes all special characters that are not allowed in an URL. This method helps to generate valid URLs.
	 * Characters that are already escaped by '%' will not be escaped again.
	 *
	 * @see "RFC 2396"
	 */
	@Nullable
	@Contract(value = "null -> null; !null -> !null", pure = true)
	public static String encodeUrl(@Nullable String url) {
		if (url == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < url.length(); i++) {
			char ch = url.charAt(i);
			if ('a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z' || '0' <= ch && ch <= '9' || ch == '-' || ch == '_' || ch == '.' || ch == '!' || ch == '~'
					|| ch == '*' || ch == '\'' || ch == '(' || ch == ')' || ch == '/' || ch == '?' || ch == '&' || ch == '=' || ch == '#' || ch == '%') {
				sb.append(ch);
			} else if (ch == ' ') {
				sb.append('+');
			} else if (ch < 0x007F) {
				// other ASCII characters (7 bit)
				sb.append('%').append(char2Hex(ch));
			} else if (ch <= 0x07FF) {
				// 2 byte UTF-8 representation
				sb.append('%').append(char2Hex((char) (0xc0 | (ch >> 6))));
				sb.append('%').append(char2Hex((char) (0x80 | (ch & 0x3F))));
			} else {
				// 3 byte UTF-8 representation
				sb.append('%').append(char2Hex((char) (0xe0 | (ch >> 12))));
				sb.append('%').append(char2Hex((char) (0x80 | ((ch >> 6) & 0x3F))));
				sb.append('%').append(char2Hex((char) (0x80 | (ch & 0x3F))));
			}

		}
		return sb.toString();
	}

	/**
	 * Escapes all special characters for an URL parameter name or parameter value.
	 * The encoding itself is done by {@link URLEncoder#encode}, this is just a convenience method that supports
	 * null and catches Exceptions.
	 */
	@Nullable
	@Contract(value = "null -> null; !null -> !null", pure = true)
	public static String encodeUrlParameter(@Nullable String url) {
		if (url == null) {
			return null;
		}

		try {
			//noinspection CharsetObjectCanBeUsed
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return url; // no escaping in case of an error
		}
	}

	/**
	 * Converts a Wiki page name to its URL path representation.
	 * This is not the same as {@link URLEncoder#encode}.
	 *
	 * Afterwards, {@link #encodeUrl(String)} should be called to get a valid URL.
	 *
	 * @see #url2PagePath(String)
	 */
	@Nullable
	@Contract(value = "null -> null; !null -> !null", pure = true)
	public static String pagePath2Url(@Nullable String pagePath) {
		if (pagePath == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < pagePath.length(); i++) {
			char ch = pagePath.charAt(i);
			if (ch == '!' || ch == '%' || ch == '?' || ch == '#') {
				String hex = char2Hex(ch);
				sb.append('!'); // Escapezeichen
				sb.append(hex);
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * Inverse funtion of {@link #pagePath2Url(String)}.
	 */
	@Nullable
	@Contract(value = "null -> null; !null -> !null", pure = true)
	public static String url2PagePath(@Nullable String url) {
		if (url == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < url.length(); i++) {
			char ch = url.charAt(i);
			if (ch == '!' && i + 2 < url.length()) {
				// auch nächste zwei Zeichen einlesen
				String hex = url.substring(i + 1, i + 3);
				i += 2;
				int originalChar = Integer.parseInt(hex, 16);
				sb.append((char) originalChar);
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * Converts a character to its 2 byte hexadecimal representation.
	 */
	@NotNull
	static String char2Hex(char ch) {
		String hex = Integer.toHexString(ch);
		if (hex.length() == 1) {
			hex = '0' + hex;
		}
		return hex;
	}

	private static final String BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	/**
	 * Encodes a byte array by Base64.
	 */
	@NotNull
	public static String encodeBase64(@NotNull byte[] bytes) {
		// Padding with 0 to get a multiple of 3 length
		int paddingLength = 0;
		if (bytes.length % 3 > 0) {
			paddingLength = 3 - (bytes.length % 3);
		}
		byte[] sourceBytes = new byte[bytes.length + paddingLength];
		System.arraycopy(bytes, 0, sourceBytes, 0, bytes.length);
		for (int i = 0; i < paddingLength; i++) {
			sourceBytes[bytes.length + i] = 0;
		}

		// Encode byte array in pieces of 3 bytes
		StringBuilder result = new StringBuilder(sourceBytes.length / 3 * 4);
		for (int i = 0; i < sourceBytes.length; i += 3) {
			// Three 8 bit characters are converted to four 6 bit characters (24 bit in sum)
			int n1 = (sourceBytes[i] & 0xFC) >> 2;
			int n2 = ((sourceBytes[i] & 0x03) << 4) | ((sourceBytes[i + 1] & 0xF0) >> 4);
			int n3 = ((sourceBytes[i + 1] & 0x0F) << 2) | ((sourceBytes[i + 2] & 0xC0) >> 6);
			int n4 = sourceBytes[i + 2] & 0x3F;

			// Encode the four characters by Base64 alphabet
			result.append(BASE64_ALPHABET.charAt(n1));
			result.append(BASE64_ALPHABET.charAt(n2));
			result.append(BASE64_ALPHABET.charAt(n3));
			result.append(BASE64_ALPHABET.charAt(n4));
		}

		// In case of padding, append the number of padding bytes as '='
		for (int i = 0; i < paddingLength; i++) {
			result.setCharAt(result.length() - 1 - i, '=');
		}

		return result.toString();
	}

	/**
	 * Decodes a Base64 encoded String.
	 */
	public static byte[] decodeBase64(String base64EncodedStr) {
		// Get padding length
		int encodedBlocks = base64EncodedStr.length() / 4;
		int paddingLength = 0;
		if (base64EncodedStr.endsWith("==")) {
			paddingLength = 2;
		} else if (base64EncodedStr.endsWith("=")) {
			paddingLength = 1;
		}

		// Replace padding character '=' by 'A'
		base64EncodedStr = base64EncodedStr.substring(0, base64EncodedStr.length() - paddingLength);
		for (int i = 0; i < paddingLength; i++) {
			// noinspection StringConcatenationInLoop
			base64EncodedStr += 'A';
		}

		// Decode byte array in pieces of 4 characters
		byte[] resultWithPadding = new byte[encodedBlocks * 3];
		for (int block = 0; block < encodedBlocks; block++) {
			// Decode 6 bit numbers by Base64 alphabet
			int n1 = BASE64_ALPHABET.indexOf(base64EncodedStr.charAt(block * 4));
			int n2 = BASE64_ALPHABET.indexOf(base64EncodedStr.charAt(block * 4 + 1));
			int n3 = BASE64_ALPHABET.indexOf(base64EncodedStr.charAt(block * 4 + 2));
			int n4 = BASE64_ALPHABET.indexOf(base64EncodedStr.charAt(block * 4 + 3));

			// Four 6 bit characters are converted to three 8 bit characters (24 bit in sum)
			resultWithPadding[block * 3] = (byte) ((n1 << 2) | (n2 >> 4));
			resultWithPadding[block * 3 + 1] = (byte) ((n2 << 4) | (n3 >> 2));
			resultWithPadding[block * 3 + 2] = (byte) ((n3 << 6) | n4);
		}

		// Remove padding bytes
		byte[] result = new byte[encodedBlocks * 3 - paddingLength];
		System.arraycopy(resultWithPadding, 0, result, 0, result.length);
		return result;
	}
}
