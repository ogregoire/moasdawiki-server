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

package net.moasdawiki.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple logger. Writes messages to the standard output.
 */
public class Logger {

	@Nullable
	private final PrintStream out;

	/**
	 * Formatter for log timestamp.
	 */
	@NotNull
	private final DateFormat dateFormat;

	/**
	 * Constructor.
	 */
	public Logger(@Nullable PrintStream out) {
		this.out = out;
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	}

	/**
	 * Writes a message to the log.
	 */
	public void write(@NotNull String message) {
		if (out == null) {
			return;
		}
		out.print("Log ");
		out.print(dateFormat.format(new Date()));
		out.print(" ");
		out.print(message);
		out.println();
	}

	/**
	 * Write a message and stack trace to the log.
	 */
	public void write(@NotNull String message, @NotNull Throwable e) {
		if (out == null) {
			return;
		}

		// write timestamp and message
		write(message + ": (" + e.getClass().getCanonicalName() + ") " + e.getMessage());

		// write stack trace
		e.printStackTrace(out);
	}
}
