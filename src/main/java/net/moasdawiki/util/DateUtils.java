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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.moasdawiki.base.ServiceException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper methods to convert date formats.
 * 
 * @author Herbert Reiter
 */
public abstract class DateUtils {

	private static final DateFormat UTC_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	static {
		UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Parses a string in ISO 8601 date format: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
	 * 
	 * @param dateStr String to parse
	 * @return Date object. <code>null</code> -> dateStr was <code>null</code>.
	 * @throws ServiceException if string has invalid format
	 */
	@Nullable
	@Contract(value = "null -> null; !null -> !null", pure = true)
	public static Date parseUtcDate(@Nullable String dateStr) throws ServiceException {
		if (dateStr == null) {
			return null;
		}
		if (dateStr.isEmpty()) {
			throw new ServiceException("Invalid date format, string is empty");
		}

		try {
			synchronized (UTC_FORMAT) {
				return UTC_FORMAT.parse(dateStr);
			}
		} catch (ParseException e) {
			throw new ServiceException("Invalid date format", e);
		}
	}

	/**
	 * Formats a date corresponding ISO 8601 format: "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
	 * 
	 * @param date Date to format
	 * @return String representation. <code>null</code> if date param was <code>null</code>.
	 */
	@Nullable
	@Contract(value = "null -> null; !null -> !null", pure = true)
	public static String formatUtcDate(@Nullable Date date) {
		if (date == null) {
			return null;
		}

		synchronized (UTC_FORMAT) {
			return UTC_FORMAT.format(date);
		}
	}

	/**
	 * Formats a date with the given format using {@link SimpleDateFormat}.
	 * This is just a convenience method.
	 * 
	 * @param date Date to format
	 * @param format Format string, see {@link DateFormat}
	 * @return String representation
	 */
	@NotNull
	public static String formatDate(@NotNull Date date, @NotNull String format) {
		DateFormat df = new SimpleDateFormat(format);
		return df.format(date);
	}
}
