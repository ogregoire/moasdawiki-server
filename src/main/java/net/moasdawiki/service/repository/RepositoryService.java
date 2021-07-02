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

package net.moasdawiki.service.repository;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.util.DateUtils;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

/**
 * Access to the files in the wiki repository.
 */
public class RepositoryService {

	/**
	 * Path of the file list cache file.
	 *
	 * Row format:
	 * File path in repository '\t' modification timestamp in ISO 8601 format "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
	 */
	public static final String FILELIST_CACHE_FILEPATH = "/filelist.cache";

	@NotNull
	protected final Logger logger;

	/**
	 * User repository base folder.
	 */
	@NotNull
	protected final File repositoryBase;

	/**
	 * User repository base folder path.
	 */
	@NotNull
	protected final String repositoryBasePath;

	/**
	 * Shadow repository base folder.
	 * null -> none.
	 */
	@Nullable
	private final File shadowRepositoryBase;

	/**
	 * Shadow repository base folder path.
	 * null -> none.
	 */
	@Nullable
	protected final String shadowRepositoryBasePath;

	/**
	 * Metadata cache for all files in the repository.
	 * Map: File path in repository -> {@link AnyFile}.
	 */
	@NotNull
	protected final Map<String, AnyFile> fileMap;

	/**
	 * Is repository scanning allowed to update the cache content?
	 * Is set to false for the App as the cache file is updates by synchronization.
	 */
	private final boolean scanRepository;

	/**
	 * Constructor.
	 */
	public RepositoryService(@NotNull Logger logger, @NotNull File repositoryBase,
							 @Nullable File shadowRepositoryBase, boolean scanRepository) {
		super();
		this.logger = logger;
		this.repositoryBase = repositoryBase;
		this.repositoryBasePath = repositoryBase.getAbsolutePath();
		this.shadowRepositoryBase = shadowRepositoryBase;
		if (shadowRepositoryBase != null) {
			this.shadowRepositoryBasePath = shadowRepositoryBase.getAbsolutePath();
		} else {
			this.shadowRepositoryBasePath = null;
		}
		this.fileMap = new HashMap<>();
		this.scanRepository = scanRepository;
		logger.write("Repository base path: " + this.repositoryBasePath);
		logger.write("Shadow repository base path: " + this.shadowRepositoryBasePath);
		reset();
	}

	/**
	 * Rereads the cache file.
	 * Is called in App environment after synchronization with server.
	 */
	public void reset() {
		if (!readCacheFile()) {
			rebuildCache();
		}
	}

	/**
	 * Read the file list cache file.
	 *
	 * @return true if the cache file was read successfully
	 */
	protected boolean readCacheFile() {
		String cacheContent;
		AnyFile fileListCacheFile = new AnyFile(FILELIST_CACHE_FILEPATH);
		try {
			cacheContent = readTextFile(fileListCacheFile);
		} catch (ServiceException e) {
			logger.write("Error reading cache file " + FILELIST_CACHE_FILEPATH);
			return false;
		}

		try {
			Map<String, AnyFile> newFileMap = parseCacheContent(cacheContent);
			fileMap.clear();
			fileMap.putAll(newFileMap);
			logger.write("Repository cache filled from cache file, " + newFileMap.size() + " files known");
			return true;
		} catch (Exception e) {
			logger.write("Error parsing cache file " + FILELIST_CACHE_FILEPATH, e);
			return false;
		}
	}

	/**
	 * Parse the cache file content.
	 */
	@Contract(value = "_ -> new", pure = true)
	@NotNull
	protected Map<String, AnyFile> parseCacheContent(@NotNull String cacheContent) throws ServiceException {
		try {
			Map<String, AnyFile> result = new HashMap<>();
			BufferedReader reader = new BufferedReader(new StringReader(cacheContent));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					// ignore empty line at end of file
					continue;
				}
				int pos1 = line.indexOf('\t');
				if (pos1 < 0) {
					throw new ServiceException("Invalid file format, cancel parsing");
				}

				String filePath = line.substring(0, pos1).trim();
				String contentTimestampStr = line.substring(pos1).trim();
				Date contentTimestamp = DateUtils.parseUtcDate(contentTimestampStr);

				AnyFile newAnyFile = new AnyFile(filePath, contentTimestamp);
				result.put(filePath, newAnyFile);
			}
			reader.close();
			return result;
		} catch (IOException e) {
			throw new ServiceException("Error parsing cache file content", e);
		}
	}

	/**
	 * Write the file list cache file.
	 */
	protected void writeCacheFile() {
		List<String> filePathList = new ArrayList<>(fileMap.keySet());
		Collections.sort(filePathList);

		StringBuilder sb = new StringBuilder();
		for (String filePath : filePathList) {
			AnyFile anyFile = fileMap.get(filePath);
			sb.append(filePath);

			sb.append('\t');

			String contentTimestampStr = DateUtils.formatUtcDate(anyFile.getContentTimestamp());
			sb.append(contentTimestampStr);

			sb.append('\n');
		}
		String cacheContent = sb.toString();

		AnyFile fileListCacheFile = new AnyFile(FILELIST_CACHE_FILEPATH);
		try {
			writeTextFile(fileListCacheFile, cacheContent);
		} catch (ServiceException e) {
			// only log error, do not escalate
			logger.write("Error writing cache file " + FILELIST_CACHE_FILEPATH, e);
		}
	}

	/**
	 * Rebuild internal cache.
	 */
	private void rebuildCache() {
		if (!scanRepository) {
			return;
		}

		List<File> files = new ArrayList<>();
		if (shadowRepositoryBase != null) {
			// scan shadow repository before the user repository
			// to give user file higher priority in case of duplicate paths
			listFilesInFilesystem(shadowRepositoryBase, files);
		}
		listFilesInFilesystem(repositoryBase, files);
		logger.write("Rebuilding repository cache, found " + files.size() + " files in repository");

		Map<String, AnyFile> newFileMap = new HashMap<>();
		for (File file : files) {
			String filesystemFilePath = file.getAbsolutePath();

			String filePath = filesystem2RepositoryPath(filesystemFilePath);
			if (filePath == null) {
				// ignore invalid file name
				continue;
			}
			Date fileTimestamp = new Date(file.lastModified());
			AnyFile newAnyFile = new AnyFile(filePath, fileTimestamp);
			newFileMap.put(filePath, newAnyFile);
		}

		fileMap.clear();
		fileMap.putAll(newFileMap);

		writeCacheFile();
	}

	/**
	 * List all files in the repository, also scans sub-folders.
	 */
	protected synchronized void listFilesInFilesystem(@NotNull File folder, @NotNull List<File> fileList) {
		File[] files = folder.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				listFilesInFilesystem(file, fileList);
			} else {
				fileList.add(file);
			}
		}
	}

	/**
	 * Return the {@link AnyFile} object for a repository file.
	 *
	 * @return null --> file not found
	 */
	@Contract(pure = true)
	@Nullable
	public synchronized AnyFile getFile(@NotNull String filePath) {
		return fileMap.get(filePath);
	}

	/**
	 * List all files in the repository.
	 */
	@Contract(value = "-> new", pure = true)
	@NotNull
	public synchronized Set<AnyFile> getFiles() {
		return new HashSet<>(fileMap.values());
	}

	/**
	 * List all files modified after the given date (exact match excluded).
	 *
	 * @param modifiedAfter date, files have to be newer;
	 *                      <code>null</code> --> no filter, list all files.
	 */
	@Contract(value = "_ -> new", pure = true)
	@NotNull
	public synchronized Set<AnyFile> getModifiedAfter(Date modifiedAfter) {
		Set<AnyFile> result = new HashSet<>();
		for (AnyFile anyFile : fileMap.values()) {
			if (modifiedAfter == null || modifiedAfter.before(anyFile.getContentTimestamp())) {
				result.add(anyFile);
			}
		}
		return result;
	}

	/**
	 * List the latest modifies files.
	 *
	 * @param count Maximum number of files to be returned.
	 *              -1 -> no filter, list all pages
	 * @param filter Filter for files that match the suffix.
	 */
	@Contract(pure = true)
	@NotNull
	public List<AnyFile> getLastModifiedFiles(int count, @NotNull Predicate<AnyFile> filter) {
		List<AnyFile> fileList = new ArrayList<>(fileMap.values());
		fileList.removeIf(filter.negate());
		fileList.sort((anyFile1, anyFile2) -> {
			Date timestamp1 = anyFile1.getContentTimestamp();
			Date timestamp2 = anyFile2.getContentTimestamp();
			return timestamp2.compareTo(timestamp1);
		});
		return fileList.subList(0, Math.min(fileList.size(), count));
	}

	/**
	 * Deletes a file from the repository.
	 */
	public synchronized void deleteFile(@NotNull AnyFile anyFile) throws ServiceException {
		String filePath = anyFile.getFilePath();
		try {
			String filename = repository2FilesystemPath(filePath, false);
			File file = new File(filename);
			if (!file.delete()) {
				String message = "Error deleting file '" + filePath + "', because the file system denied the action";
				logger.write(message);
				throw new ServiceException(message);
			}
		} catch (SecurityException e) {
			String message = "Error deleting file '" + filePath + "', because of a security violation";
			logger.write(message, e);
			throw new ServiceException(message, e);
		}

		// update cache
		fileMap.remove(filePath);
		writeCacheFile();
		logger.write("File '" + filePath + "' deleted");
	}

	/**
	 * Read the content of a text file.
	 * Throws an exception if the file doesn't exist.
	 */
	@NotNull
	public synchronized String readTextFile(@NotNull AnyFile anyFile) throws ServiceException {
		byte[] contentBytes = readBinaryFile(anyFile);
		return new String(contentBytes, StandardCharsets.UTF_8);
	}

	/**
	 * Write the content of a text file.
	 */
	@NotNull
	public synchronized AnyFile writeTextFile(@NotNull AnyFile anyFile, @NotNull String content) throws ServiceException {
		byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
		return writeBinaryFile(anyFile, contentBytes, null);
	}

	/**
	 * Read the content of a binary file from the user or the shadow repository.
	 * Throws an exception if the file doesn't exist.
	 */
	public synchronized byte @NotNull [] readBinaryFile(@NotNull AnyFile anyFile) throws ServiceException {
		String filePath = anyFile.getFilePath();
		filePath = PathUtils.makeWebPathAbsolute(filePath, null);
		String filename = repository2FilesystemPath(filePath, false);
		File file = new File(filename);
		if (!file.exists() && shadowRepositoryBase != null) {
			// use shadow repository as fallback
			filename = repository2FilesystemPath(filePath, true);
			if (filename != null) {
				file = new File(filename);
			}
		}
		if (!file.exists()) {
			String message = "File not found in repository: " + file.getAbsolutePath();
			logger.write(message);
			throw new ServiceException(message);
		}

		// update cache
		if (!fileMap.containsKey(filePath)) {
			logger.write("Detected new file '" + filePath + "' in repository, adding to cache");
			Date fileTimestamp = new Date(file.lastModified());
			AnyFile newAnyFile = new AnyFile(filePath, fileTimestamp);
			fileMap.put(filePath, newAnyFile);

			if (!FILELIST_CACHE_FILEPATH.equals(filePath)) {
				// Don't write cache file while it is read,
				// otherwise it will be overwritten with empty content.
				writeCacheFile();
			}
		}

		logger.write("Reading file '" + filePath + "' from repository");
		try (FileInputStream is = new FileInputStream(file)) {
			byte[] fileContent = new byte[is.available()];
			//noinspection ResultOfMethodCallIgnored
			is.read(fileContent);
			return fileContent;
		} catch (IOException e) {
			String message = "Error reading file '" + filePath + "'";
			logger.write(message, e);
			throw new ServiceException(message, e);
		}
	}

	/**
	 * Write the content of a binary file to the user repository.
	 * If the file already exists it will be overwritten.
	 */
	@NotNull
	public synchronized AnyFile writeBinaryFile(@NotNull AnyFile anyFile, byte @NotNull [] content, @Nullable Date contentTimestamp) throws ServiceException {
		String filePath = anyFile.getFilePath();
		filePath = PathUtils.makeWebPathAbsolute(filePath, null);
		String filename = repository2FilesystemPath(filePath, false);
		File file = new File(filename);

		createFolders(file);
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(content);
		} catch (SecurityException e) {
			String message = "Error saving file '" + filePath + "', because of a security violation";
			logger.write(message, e);
			throw new ServiceException(message, e);
		} catch (IOException e) {
			String message = "Error saving file '" + filePath + "'";
			logger.write(message, e);
			throw new ServiceException(message, e);
		}

		if (contentTimestamp == null) {
			contentTimestamp = new Date(file.lastModified());
		}
		AnyFile newAnyFile = new AnyFile(filePath, contentTimestamp);
		fileMap.put(filePath, newAnyFile);
		logger.write("Content for file '" + filePath + "' successfully written");

		if (!FILELIST_CACHE_FILEPATH.equals(filePath)) {
			// avoid endless loop
			writeCacheFile();
		}
		return newAnyFile;
	}

	/**
	 * Create all sub-folders required to write a file.
	 */
	protected void createFolders(@NotNull File file) throws ServiceException {
		File fileFolder = file.getParentFile();
		if (!fileFolder.exists() && !fileFolder.mkdirs()) {
			String message = "Error creating folder '" + fileFolder.getAbsolutePath() + "'";
			logger.write(message);
			throw new ServiceException(message);
		}
	}

	/**
	 * Convert a repository file path to an absolute file system path.
	 * Escapes special characters.
	 *
	 * Example:<br>
	 * Repository path:  <tt>/path/to/file</tt><br>
	 * File system path: <tt>/path/to/repository/path/to/file</tt>
	 */
	@Contract(value = "null,_ -> null; !null,false -> !null", pure = true)
	@Nullable
	protected String repository2FilesystemPath(@Nullable String repositoryPath, boolean useShadowRepository) {
		if (repositoryPath == null) {
			return null;
		}

		// Invalid characters in Windows: "*/:<>?\|
		// Invalid characters in Linux:   /
		// Escape character: '%'
		// Folder separator: '/'
		// "." and ".." are escaped
		final String forbiddenChars = "\"%*:<>?\\|";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < repositoryPath.length(); i++) {
			char c = repositoryPath.charAt(i);
			if (c < 32 || c > 255 || forbiddenChars.indexOf(c) >= 0
					|| c == '.' && (i - 1 >= 0 && i + 2 <= repositoryPath.length() && "/./".contentEquals(repositoryPath.subSequence(i - 1, i + 2))
							|| i - 1 >= 0 && i + 3 <= repositoryPath.length() && "/../".contentEquals(repositoryPath.subSequence(i - 1, i + 3))
							|| i - 2 >= 0 && i + 2 <= repositoryPath.length() && "/../".contentEquals(repositoryPath.subSequence(i - 2, i + 2)))) {
				// convert to "%wxyz" representation
				String hex = Integer.toString(c, 16);
				sb.append('%');
				for (int k = hex.length(); k < 4; k++) {
					sb.append('0');
				}
				sb.append(hex);
			} else {
				sb.append(c);
			}
		}

		String filePath = PathUtils.convertWebPath2FilePath(sb.toString());
		if (useShadowRepository) {
			return PathUtils.concatFilePaths(shadowRepositoryBasePath, filePath);
		} else {
			return PathUtils.concatFilePaths(repositoryBasePath, filePath);
		}
	}

	/**
	 * Convert an absolute file system path to a repository file path.
	 * Unescapes special characters.
	 *
	 * Example:<br>
	 * File system path: <tt>/path/to/repository/path/to/file</tt>
	 * Repository path:  <tt>/path/to/file</tt><br>
	 *
	 * @return Repository file path;
	 *         <code>null</code> -> path is outside of the repository or invalid.
	 */
	@Contract(value = "null -> null", pure = true)
	@Nullable
	protected String filesystem2RepositoryPath(@Nullable String filesystemPath) {
		if (filesystemPath == null) {
			return null;
		}

		// cut off path to repository root
		StringBuilder sb = new StringBuilder(filesystemPath);
		if (filesystemPath.startsWith(repositoryBasePath)) {
			sb.delete(0, repositoryBasePath.length());
		} else if (shadowRepositoryBasePath != null && filesystemPath.startsWith(shadowRepositoryBasePath)) {
			sb.delete(0, shadowRepositoryBasePath.length());
		} else {
			logger.write("filePath2PagePath: Invalid file name '" + filesystemPath + "', doesn't exist in repository '" + repositoryBasePath + "'");
			return null;
		}
		if (sb.length() == 0 || sb.charAt(0) != File.separatorChar) {
			sb.insert(0, File.separatorChar);
		}

		// Unescape "%wxyz"
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			if (c == '%' && i + 4 < sb.length()) {
				try {
					String hex = sb.substring(i + 1, i + 5);
					char d = (char) Integer.parseInt(hex, 16);
					sb.setCharAt(i, d);
					sb.delete(i + 1, i + 5);
				} catch (NumberFormatException e) {
					logger.write("filePath2PagePath: Invalid file name '" + filesystemPath + "'", e);
					return null;
				}
			}
		}

		return PathUtils.convertFilePath2WebPath(sb.toString());
	}
}
