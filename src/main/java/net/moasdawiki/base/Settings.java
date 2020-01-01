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

package net.moasdawiki.base;

import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Reads the settings file and provides methods to get settings. Multiple
 * settings with the same key are combined to a list value.
 * 
 * @author Herbert Reiter
 */
@SuppressWarnings("SameReturnValue")
public class Settings {

	private static final String PROGRAM_NAME = "MoasdaWiki";
	private static final String CONFIG_FILE_SERVER = "/config.txt";
	private static final String CONFIG_FILE_APP = "/config-app.txt";

	private static final String SERVERPORT = "port";
	private static final String REPOSITORY_PAGESUFFIX = "repository.pagesuffix";
	private static final String REPOSITORY_PAGESUFFIX_DEFAULT = ".txt";
	private static final String MESSAGE_FILE = "messagefile";
	private static final String PAGE_STARTPAGE = "page.startpage";
	private static final String PAGE_STARTPAGE_DEFAULT = "/wiki/Startpage";
	private static final String PAGE_NAVIGATION = "page.navigation";
	private static final String PAGE_HTML_HEADER = "page.html.header";
	private static final String PAGE_HEADER = "page.header";
	private static final String PAGE_FOOTER = "page.footer";
	private static final String PAGE_TEMPLATES = "page.templates";
	private static final String PAGE_TEMPLATES_DEFAULT = "/wiki/Templates";
	private static final String PAGE_INDEX_NAME = "page.index.name";
	private static final String PAGE_INDEX_DEFAULT = "page.index.default";
	private static final String REPOSITORY_ROOT = "repository.root";
	private static final String REPOSITORY_ROOT_DEFAULT = "root";
	private static final String PLUGIN = "plugin";
	private static final String SYNCHRONIZATION_EXCLUDE = "synchronization.exclude";
	private static final String AUTHENTICATION_ONLYLOCALHOST = "authentication.onlylocalhost";

	private final Logger logger;

	/**
	 * Contains all settings. The map value is either a string or a list of
	 * strings. Not <code>null</code>.
	 */
	@NotNull
	private final Map<String, Object> settings;

	/**
	 * Constructor.
	 */
	public Settings(@NotNull Logger logger, @NotNull RepositoryService repositoryService, @NotNull String configFileName) {
		this.logger = logger;
		this.settings = new HashMap<>();
		readSettings(repositoryService, configFileName);
	}

	/**
	 * Reads the configuration file.
	 */
	private void readSettings(@NotNull RepositoryService repositoryService, @NotNull String configFileName) {
		try {
			AnyFile anyFile = new AnyFile(configFileName, null);
			String settingsContent = repositoryService.readTextFile(anyFile);
			BufferedReader reader = new BufferedReader(new StringReader(settingsContent));
			String line;
			while ((line = reader.readLine()) != null) {
				extractMapping(line);
			}
			reader.close();
		} catch (Exception e) {
			logger.write("Cannot read settings file: " + e.getMessage());
		}
	}

	/**
	 * Parses a single line of the configuration file.
	 */
	private void extractMapping(@NotNull String line) {
		if (line.startsWith("#") || line.startsWith("//")) {
			return; // ignore line comment
		}

		int pos = line.indexOf('=');
		if (pos == -1) {
			// no valid key-value-pair with '=' delimiter
			return;
		}

		// extract key and value
		String key = line.substring(0, pos).trim();
		String value = line.substring(pos + 1).trim();

		// add to settings map
		Object previousValue = settings.get(key);
		if (previousValue instanceof String) {
			// key is already mapped to a String value
			// --> create String list and add second value
			List<String> values = new ArrayList<>();
			values.add((String) previousValue);
			values.add(value);
			settings.put(key, values);
		} else if (previousValue instanceof List<?>) {
			// add value to existing String list
			@SuppressWarnings("unchecked")
			List<String> values = (List<String>) previousValue;
			values.add(value);
		} else {
			// first assignment of key
			settings.put(key, value);
		}
	}

	/**
	 * Returns a string value.
	 * 
	 * @param key Setting key, not <code>null</code>.
	 * @return Setting value. <code>null</code> --> no setting for the key.
	 */
	@Nullable
	public String getString(@NotNull String key) {
		return getString(key, null);
	}

	/**
	 * Returns a string value. If the key has no value, the given default value
	 * is returned instead.
	 * 
	 * @param key Setting key, not <code>null</code>.
	 * @param defaultValue Default value.
	 * @return Setting value or default value. Not <code>null</code> if default
	 *         value is not <code>null</code>.
	 */
	@Nullable
	@Contract(value = "_, !null -> !null", pure = true)
	public String getString(@NotNull String key, @Nullable String defaultValue) {
		Object value = settings.get(key);
		if (value instanceof String) {
			return (String) value;
		}
		if (value instanceof List<?>) {
			List<?> values = (List<?>) value;
			if (values.size() >= 1) {
				return (String) values.get(0);
			}
		}
		return defaultValue;
	}

	/**
	 * Returns a string array value.
	 * 
	 * @param key Setting key, not <code>null</code>.
	 * @param defaultValues Default values.
	 * @return Setting value or default value. Not <code>null</code>.
	 */
	@NotNull
	public String[] getStringArray(@NotNull String key, String... defaultValues) {
		Object value = settings.get(key);
		if (value instanceof List<?>) {
			List<?> valueList = (List<?>) value;
			if (!valueList.isEmpty() && valueList.get(0) instanceof String) {
				//noinspection SuspiciousToArrayCall
				return valueList.toArray(new String[0]);
			} else {
				return new String[0];
			}
		} else if (value instanceof String) {
			String[] result = new String[1];
			result[0] = (String) value;
			return result;
		}
		return defaultValues;
	}

	/**
	 * Returns an int value.
	 * 
	 * @param key Setting key, not <code>null</code>.
	 * @param defaultValue Default value.
	 * @return Setting value or default value.
	 */
	@SuppressWarnings("unused")
	public int getInt(@NotNull String key, int defaultValue) {
		return getInteger(key, defaultValue);
	}

	/**
	 * Returns an integer value.
	 * 
	 * @param key Setting key, not <code>null</code>.
	 * @param defaultValue Default value. <code>null</code> --> no default
	 *        value.
	 * @return Setting value or default value. <code>null</code> --> setting not
	 *         existing or not an integer.
	 */
	@Nullable
	@Contract(value = "_, !null -> !null")
	public Integer getInteger(@NotNull String key, @Nullable Integer defaultValue) {
		Integer value = defaultValue;
		String strValue = getString(key);
		try {
			if (strValue != null) {
				value = Integer.parseInt(strValue);
			}
		} catch (NumberFormatException e) {
			if (logger != null) {
				logger.write("Setting value for '" + key + "' is not a number: '" + strValue + "'");
			}
		}
		return value;
	}

	/**
	 * Returns a boolean value.
	 * 
	 * @param key Setting key, not <code>null</code>.
	 * @param defaultValue Default value.
	 * @return Setting value or default value.
	 */
	public boolean getBoolean(@NotNull String key, boolean defaultValue) {
		boolean value = defaultValue;
		String strValue = getString(key);
		if ("true".equalsIgnoreCase(strValue)) {
			value = true;
		} else if ("false".equalsIgnoreCase(strValue)) {
			value = false;
		} else {
			logger.write("Setting value for '" + key + "' is not true or false: '" + strValue + "'");
		}
		return value;
	}

	/**
	 * Returns the program name.
	 */
	@NotNull
	public String getProgramName() {
		return PROGRAM_NAME;
	}

	/**
	 * Returns the version number.
	 */
	@NotNull
	public String getVersion() {
		Package aPackage = Settings.class.getPackage();
		String implementationVersion = aPackage.getImplementationVersion();
		if (implementationVersion != null) {
			return implementationVersion;
		} else {
			return "SNAPSHOT";
		}
	}

	/**
	 * Returns the program name inclusive version number.
	 */
	@NotNull
	public String getProgramNameVersion() {
		return getProgramName() + " " + getVersion();
	}

	/**
	 * Returns the file path of the message file. <code>null</code> --> no
	 * message file configured.
	 */
	@Nullable
	public String getMessageFile() {
		return getString(MESSAGE_FILE);
	}

	/**
	 * Returns the host name of the wiki server. <code>null</code> --> unknown.
	 */
	@Nullable
	public String getServerHost() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * Returns the port of the wiki server.
	 */
	public int getServerPort() {
		return getInteger(SERVERPORT, 80);
	}

	/**
	 * Returns the file extension of the wiki page files in the repository.
	 * Default is ".txt".
	 */
	@NotNull
	public String getPageSuffix() {
		return getString(REPOSITORY_PAGESUFFIX, REPOSITORY_PAGESUFFIX_DEFAULT);
	}

	/**
	 * Wiki page to be shown as start page.
	 */
	@NotNull
	public String getStartpagePath() {
		return getString(PAGE_STARTPAGE, PAGE_STARTPAGE_DEFAULT);
	}

	/**
	 * Wiki page to be shown in navigation area. <code>null</code> --> no
	 * navigation area.
	 */
	@Nullable
	public String getNavigationPagePath() {
		return getString(PAGE_NAVIGATION);
	}

	/**
	 * Wiki page to be included as HTML header lines. <code>null</code> --> no
	 * additional HTML header lines.
	 */
	@Nullable
	public String getHtmlHeaderPagePath() {
		return getString(PAGE_HTML_HEADER);
	}

	/**
	 * Wiki page to be shown on top of all wiki pages. <code>null</code> --> no
	 * page header.
	 */
	@Nullable
	public String getHeaderPagePath() {
		return getString(PAGE_HEADER);
	}

	/**
	 * Wiki page to be shown on bottom of all wiki pages. <code>null</code> -->
	 * no page footer.
	 */
	@Nullable
	public String getFooterPagePath() {
		return getString(PAGE_FOOTER);
	}

	/**
	 * Parent wiki page of content templates.
	 */
	@Nullable
	public String getTemplatesPagePath() {
		return getString(PAGE_TEMPLATES, PAGE_TEMPLATES_DEFAULT);
	}

	/**
	 * Page name that is used as Index (Summary) of a repository folder.
	 */
	@Nullable
	public String getIndexPageName() {
		return getString(PAGE_INDEX_NAME);
	}

	/**
	 * Index page to be used as fallback if a repository folder has no own index
	 * page.
	 */
	@Nullable
	public String getIndexFallbackPagePath() {
		return getString(PAGE_INDEX_DEFAULT);
	}

	/**
	 * Repository subfolder that contains the files that are accessible via URL
	 * root path, e.g. favicon.ico.
	 */
	@NotNull
	public String getRootPath() {
		return getString(REPOSITORY_ROOT, REPOSITORY_ROOT_DEFAULT);
	}

	/**
	 * Class names of the plugins to be loaded.
	 */
	@NotNull
	public Set<String> getPluginClassNames() {
		String[] classNames = getStringArray(PLUGIN);
		return new HashSet<>(Arrays.asList(classNames));
	}

	/**
	 * Files and folders in the repository to be excluded from synchronization.
	 */
	@NotNull
	public Set<String> getSynchronizationExcludes() {
		String[] excludes = getStringArray(SYNCHRONIZATION_EXCLUDE);
		return new HashSet<>(Arrays.asList(excludes));
	}

	/**
	 * Should the wiki server only be accessible for clients running on local
	 * host?
	 */
	public boolean isOnlyLocalhostAccess() {
		return getBoolean(AUTHENTICATION_ONLYLOCALHOST, false);
	}

	/**
	 * Returns the current system time.
	 */
	@NotNull
	public Date getActualTime() {
		return new Date();
	}

	@NotNull
	public static String getConfigFileServer() {
		return CONFIG_FILE_SERVER;
	}

	@SuppressWarnings("unused")
	@NotNull
	public static String getConfigFileApp() {
		return CONFIG_FILE_APP;
	}
}
