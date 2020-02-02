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

import java.io.File;

/**
 * Helper methods for web paths and file paths.
 *
 * @author Herbert Reiter
 */
public abstract class PathUtils {

	/**
	 * Converts all occurrences of the path separator '/' into the path separator of the current operating system.
	 * 
	 * @see #convertFilePath2WebPath(String) 
	 */
	@Contract(value = "null -> null; !null -> !null", pure = true)
	@Nullable
	public static String convertWebPath2FilePath(@Nullable String webPath) {
		return convertPath(webPath, '/', File.separatorChar);
	}

	/**
	 * Converts all occurrences of the OS path separator to '/'.
	 * 
	 * @see #convertWebPath2FilePath(String)
	 */
	@Contract(value = "null -> null; !null -> !null", pure = true)
	@Nullable
	public static String convertFilePath2WebPath(@Nullable String filePath) {
		return convertPath(filePath, File.separatorChar, '/');
	}

	/**
	 * Replaces all occurrences of a single character by another character.
	 */
	@Contract(value = "null, _, _ -> null; !null, _, _ -> !null", pure = true)
	@Nullable
	static String convertPath(@Nullable String originPath, char oldChar, char newChar) {
		if (originPath == null) {
			return null;
		}

		if (oldChar == newChar) {
			return originPath;
		}

		StringBuilder sb = new StringBuilder(originPath);
		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == oldChar) {
				sb.setCharAt(i, newChar);
			}
		}
		return sb.toString();
	}

	/**
	 * Concatenates two web paths and puts a single '/' in between.
	 *
	 * @param firstPath First part. null = "".
	 * @param secondPath Second part. null = "".
	 * @return Concatenated path.
	 */
	@NotNull
	public static String concatWebPaths(@Nullable String firstPath, @Nullable String secondPath) {
		return concatPaths(firstPath, secondPath, '/');
	}

	/**
	 * Concatenates two file paths and puts a single OS file path separator in between.
	 *
	 * @param firstPath First part. null = "".
	 * @param secondPath Second part. null = "".
	 * @return Concatenated path.
	 */
	@NotNull
	public static String concatFilePaths(@Nullable String firstPath, @Nullable String secondPath) {
		return concatPaths(firstPath, secondPath, File.separatorChar);
	}

	/**
	 * Concatenates two file paths and puts a single file path separator in between.
	 *
	 * @param firstPath First part. null = "".
	 * @param secondPath Second part. null = "".
	 * @param separatorChar Path separator, e.g. '/'.
	 * @return Concatenated path.
	 */
	@NotNull
	private static String concatPaths(@Nullable String firstPath, @Nullable String secondPath, char separatorChar) {
		StringBuilder sb = new StringBuilder();
		if (firstPath != null) {
			sb.append(firstPath);
		}

		// Add separator
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != separatorChar) {
			sb.append(separatorChar);
		}

		// Avoid a double separator
		if (secondPath != null && secondPath.length() > 0) {
			if (secondPath.charAt(0) == separatorChar) {
				sb.append(secondPath, 1, secondPath.length());
			} else {
				sb.append(secondPath);
			}
		}

		return sb.toString();
	}

	/**
	 * Makes a relative web path absolute by prefixing basePath. If the web path is already absolute, nothing is changed.
	 * In both cases the result path starts with '/'.
	 *
	 * @param path Path. null = "".
	 * @param basePath Base path in case of a relative path. null = "".
	 * @return Absolute path.
	 */
	@NotNull
	public static String makeWebPathAbsolute(@Nullable String path, @Nullable String basePath) {
		if (path != null && path.startsWith("/")) {
			return path;
		} else {
			String newPath = concatWebPaths(basePath, path);
			if (newPath.length() == 0 || newPath.charAt(0) != '/') {
				// als absolut kennzeichnen
				newPath = "/" + newPath;
			}
			return resolveDots(newPath);
		}
	}

	/**
	 * Interprets back stepping sub paths "..".
	 *
	 * @param path Path
	 * @return Interpreted path, without "..".
	 */
	@NotNull
	static String resolveDots(@NotNull String path) {
		if (!path.contains("..")) {
			return path; // nothing to do
		}

		StringBuilder sb = new StringBuilder(path);
		int pos = sb.indexOf("..");
		while (pos >= 0) {
			// Check for "/../"
			if ((pos == 0 || sb.charAt(pos - 1) == '/') && (pos + 2 >= sb.length() || sb.charAt(pos + 2) == '/')) {
				// Determine leftmost position to be removed (inclusive),
				// the leading separator is kept
				int pos1;
				if (pos >= 2) {
					// super folder exists
					pos1 = sb.lastIndexOf("" + '/', pos - 2) + 1;
				} else if (pos == 1) {
					pos1 = 1; // keep leading '/'
				} else {
					pos1 = 0; // remove from beginning
				}
				// Determine rightmost position to be removed (exclusive)
				int pos2;
				if (pos + 3 <= sb.length()) {
					pos2 = pos + 3; // also remove trailing '/'
				} else {
					pos2 = pos + 2;
				}
				// Remove substring
				sb.delete(pos1, pos2);
			}

			// Continue with next occurrence
			pos = sb.indexOf("..");
		}

		return sb.toString();
	}

	/**
	 * Returns the web path part before the last separator '/'.
	 */
	@Contract(value = "null -> null; !null -> !null", pure = true)
	@Nullable
	public static String extractWebFolder(@Nullable String webpath) {
		if (webpath == null) {
			return null;
		}

		int index = webpath.lastIndexOf('/');
		if (index >= 0) {
			return webpath.substring(0, index + 1);
		} else {
			return "/";
		}
	}

	/**
	 * Returns the web path part after the last separator '/'.
	 */
	@Contract(value = "null -> null; !null -> !null", pure = true)
	@Nullable
	public static String extractWebName(@Nullable String webpath) {
		if (webpath == null) {
			return null;
		}

		int index = webpath.lastIndexOf('/');
		if (index >= 0) {
			return webpath.substring(index + 1);
		} else {
			return webpath;
		}
	}
}
