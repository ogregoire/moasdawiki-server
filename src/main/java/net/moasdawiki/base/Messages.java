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

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import org.jetbrains.annotations.NotNull;

/**
 * Reads the message file and provides internationalization support for internal
 * services and plugins. Messages can contain placeholders <code>{n}</code> for
 * arguments.<br>
 * <br>
 * The message key <code>wiki.messageformat.locale</code> defines the Java
 * locale to be used for date and number formatting.
 * 
 * @author Herbert Reiter
 */
public class Messages {

	/**
	 * Message key that refers to a Java locale that is used for date and number
	 * formatting. Only a language is supported, no country or variant.
	 */
	private static final String MESSAGEFORMAT_LOCALE_KEY = "wiki.messageformat.locale";

	private final Logger logger;
	private final Settings settings;
	private final RepositoryService repositoryService;

	/**
	 * Messages read from message file, not <code>null</code>.<br>
	 * Map: message key -> message
	 */
	@NotNull
	private final Map<String, String> messages;

	/**
	 * Locale for MessageFormat. Not <code>null</code>.
	 */
	@NotNull
	private Locale messageFormatLocale;

	/**
	 * Constructor.
	 */
	public Messages(@NotNull Logger logger, @NotNull Settings settings, @NotNull RepositoryService repositoryService) {
		this.logger = logger;
		this.settings = settings;
		this.repositoryService = repositoryService;

		messages = new HashMap<>();
		messageFormatLocale = Locale.ENGLISH; // default
		readMessages();
		setMessageFormatLocale();
	}

	/**
	 * Reads the messages from the message file.
	 */
	private void readMessages() {
		String messagesFilePath = settings.getMessageFile();
		if (messagesFilePath == null) {
			// no message file configured
			return;
		}

		try {
			AnyFile anyFile = new AnyFile(messagesFilePath, null);
			String settingsContent = repositoryService.readTextFile(anyFile);
			BufferedReader reader = new BufferedReader(new StringReader(settingsContent));
			String line;
			while ((line = reader.readLine()) != null) {
				extractMapping(line);
			}
			reader.close();
		} catch (Exception e) {
			logger.write("Cannot read message file: " + e.getMessage());
		}
	}

	/**
	 * Scans a single line.
	 */
	private void extractMapping(@NotNull String line) {
		if (line.startsWith("#") || line.startsWith("//")) {
			return; // ignore comment line
		}

		int pos = line.indexOf('=');
		if (pos == -1) {
			// no valid assignment delimited by '='
			return;
		}

		// extract key and value
		String key = line.substring(0, pos).trim();
		String value = line.substring(pos + 1).trim();
		messages.put(key, value);
	}

	/**
	 * Reads the locale for the MessageFormat.
	 */
	private void setMessageFormatLocale() {
		String localeStr = messages.get(MESSAGEFORMAT_LOCALE_KEY);
		if (localeStr != null) {
			messageFormatLocale = new Locale(localeStr);
		}
	}

	/**
	 * Returns the message for a given key. If the message contains placeholders
	 * <code>{n}</code> they are replaced by the corresponding argument value,
	 * for syntax see {@link MessageFormat}. If there is no message for a key,
	 * the last key segment (after the last '.' ,if any) is returned instead.
	 * 
	 * @param key message key, not <code>null</code>
	 * @param arguments arguments to be filled in placeholders
	 * @return message, not <code>null</code>
	 */
	@NotNull
	public String getMessage(@NotNull String key, Object... arguments) {
		// get message
		String msg = messages.get(key);

		if (msg != null) {
			// fill placeholders, format date and numbers
			MessageFormat mf = new MessageFormat(msg, messageFormatLocale);
			msg = mf.format(arguments);

		} else {
			// no message found --> use last part of key
			int dotpos = key.lastIndexOf('.');
			if (dotpos >= 0) {
				msg = key.substring(dotpos + 1);
			} else {
				msg = key;
			}
		}
		return msg;
	}
}
