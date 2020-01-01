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

import java.util.List;

/**
 * Helper methods to generate JavaScript Strings.
 * 
 * @author Herbert Reiter
 */
public abstract class JavaScriptUtils {

	/**
	 * Generates a JSON array from a string list.
	 *
	 * @param list String list
	 * @return JavaScript array
	 */
	@NotNull
	public static String toArray(@NotNull List<String> list) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');

		for (int i = 0; i < list.size(); i++) {
			if (i >= 1) {
				sb.append(", ");
			}
			sb.append('"');
			sb.append(escapeJavaScript(list.get(i)));
			sb.append('"');
		}

		sb.append(']');
		return sb.toString();
	}

	/**
	 * Escapes all JavaScript special characters.
	 *
	 * @param str String to be escaped
	 * @return Escaped string. null -> Parameter was null.
	 * @see "http://www.ietf.org/rfc/rfc4627.txt"
	 */
	@Nullable
	@Contract(value = "null -> null; !null -> !null", pure = true)
	public static String escapeJavaScript(@Nullable String str) {
		if (str == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '\"') {
				sb.append("\\\"");
			} else if (c == '\'') {
				sb.append("\\'");
			} else if (c == '\\') {
				sb.append("\\\\");
			} else if (c == '\b') {
				sb.append("\\b");
			} else if (c == '\f') {
				sb.append("\\f");
			} else if (c == '\n') {
				sb.append("\\n");
			} else if (c == '\r') {
				sb.append("\\r");
			} else if (c == '\t') {
				sb.append("\\t");
			} else {
				// unverändert übernehmen
				sb.append(c);
			}
		}

		return sb.toString();
	}
}
