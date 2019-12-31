/*
 * Copyright (c) 2008 - 2019 Dr. Herbert Reiter (support@moasdawiki.net)
 * 
 * This file is part of MoasdaWiki.
 * 
 * MoasdaWiki is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * MoasdaWiki is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MoasdaWiki. If not, see <http://www.gnu.org/licenses/>.
 */

package net.moasdawiki.util;

import net.moasdawiki.base.ServiceException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods to modify Strings.
 * 
 * @author Herbert Reiter
 */
public abstract class StringUtils {

	public static final String[] EMPTY_STRING_ARRAY = {};

	/**
	 * Checks if the value is numeric. Doesn't support a leading sign.
	 *
	 * @param s String to check
	 * @return <code>true</code> if numeric.
	 */
	public static boolean isNumeric(@Nullable String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}

		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Parses a String to an Integer. Converts Exceptions.
	 *
	 * @param s String to parse
	 * @return Corresponding Integer value. <code>null</code> if no valid value.
	 * @throws ServiceException in case of an parse error
	 */
	@Contract(value = "null -> null", pure = true)
	@Nullable
	public static Integer parseInteger(@Nullable String s) throws ServiceException {
		if (s == null || s.isEmpty()) {
			return null;
		}

		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new ServiceException("Invalid integer", e);
		}
	}

	/**
	 * Concatenates the String list with the given separator.
	 *
	 * @param stringList String list
	 * @param separator Separator
	 * @return Concatenated string
	 */
	@NotNull
	public static String concat(@Nullable List<String> stringList, @Nullable String separator) {
		if (stringList == null || stringList.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (String str : stringList) {
			if (sb.length() > 0 && separator != null) {
				sb.append(separator);
			}
			sb.append(str);
		}
		return sb.toString();
	}

	/**
	 * Concatenates the String array with the given separator.
	 * 
	 * @param strings String array
	 * @param separator Separator
	 * @return Concatenated string
	 */
	@NotNull
	public static String concat(@Nullable String[] strings, @Nullable String separator) {
		if (strings == null || strings.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (String str : strings) {
			if (sb.length() > 0 && separator != null) {
				sb.append(separator);
			}
			sb.append(str);
		}
		return sb.toString();
	}

	/**
	 * Splits a String at all occurrences of a white-space character.
	 *
	 * @param str String to split
	 * @return String pieces
	 */
	@NotNull
	public static String[] splitByWhitespace(@Nullable String str) {
		if (str == null || str.isEmpty()) {
			return EMPTY_STRING_ARRAY;
		}

		List<String> result = new ArrayList<>();
		boolean insideToken = false;
		int fromPos = 0;
		int i = 0;
		while (i < str.length()) {
			char ch = str.charAt(i);
			if (insideToken) {
				if (Character.isWhitespace(ch)) {
					// token completed
					result.add(str.substring(fromPos, i));
					insideToken = false;
				}
			} else {
				if (!Character.isWhitespace(ch)) {
					// begin of next token
					fromPos = i;
					insideToken = true;
				}
			}
			i++;
		}

		// take last token
		if (insideToken) {
			result.add(str.substring(fromPos));
		}

		return result.toArray(EMPTY_STRING_ARRAY);
	}

	/**
	 * Returns the given string, but null is replaced by "".
	 *
	 * @param str String
	 * @return String, not null
	 */
	@Contract(value = "!null -> param1", pure = true)
	@NotNull
	public static String nullToEmpty(@Nullable String str) {
		if (str == null) {
			return "";
		}
		return str;
	}

	/**
	 * Returns the given string, but "" is replaced by null.
	 *
	 * @param str String
	 * @return String or null
	 */
	@Contract(value = "null -> null", pure = true)
	@Nullable
	public static String emptyToNull(@Nullable String str) {
		if (str == null || str.isEmpty()) {
			return null;
		}
		return str;
	}
}
