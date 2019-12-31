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

package net.moasdawiki.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple logger. Writes messages to a log file or to the standard output if not
 * yet configured.
 * 
 * @author Herbert Reiter
 */
public class Logger {

	/**
	 * Formatter for log timestamp.
	 */
	@NotNull
	private final DateFormat dateFormat;

	/**
	 * Writer into log file. <code>null</code> --> write to standard output.
	 */
	@Nullable
	private PrintWriter writer;

	/**
	 * Constructor.
	 */
	public Logger() {
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	}

	/**
	 * Set log file to write the messages to.
	 */
	public void setLogfile(@NotNull String logfile) {
		try {
			writer = new PrintWriter(new FileWriter(logfile, true));
		} catch (IOException e) {
			write("Error opening log file '" + logfile + "': " + e.getMessage());
		}
	}

	/**
	 * Writes a message to the log.
	 */
	public void write(@NotNull String message) {
		if (writer != null) {
			// write to log file
			writer.print(dateFormat.format(new Date()));
			writer.print(' ');
			writer.print(message);
			writer.println();
			writer.flush();
		} else {
			// write to standard output otherwise
			System.out.print("Log ");
			System.out.print(dateFormat.format(new Date()));
			System.out.print(" ");
			System.out.print(message);
			System.out.println();
		}
	}

	/**
	 * Write a message and stack trace to the log.
	 */
	public void write(@NotNull String message, @NotNull Throwable e) {
		// write timestamp and message
		write(message + ": (" + e.getClass().getCanonicalName() + ") " + e.getMessage());

		// write stack trace
		if (writer != null) {
			e.printStackTrace(writer);
			writer.flush();
		} else {
			e.printStackTrace(System.out);
		}
	}
}
