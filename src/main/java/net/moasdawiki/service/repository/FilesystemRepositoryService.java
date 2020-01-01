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
 * Serviceschicht für den Zugriff auf alle Dateien im Wiki-Repository, einem
 * Unterordner im Dateisystem. Das Repository enthält sämtlichen Wikiseiten
 * sowie Bilder, CSS, JavaScript und weitere Binärdateien (z.B. PDF).<br>
 * <br>
 * Wenn eine Repository-Cachedatei vorhanden ist, wird die Dateiliste aus der
 * Cachedatei eingelesen anstatt das Repository zu scannen. Andernfalls wird das
 * Repository gescannt und die Cachedatei automatisch angelegt.
 * 
 * @author Herbert Reiter
 */
public class FilesystemRepositoryService implements RepositoryService {

	/**
	 * Pfad der Cachedatei mit der Liste aller Repository-Dateien.<br>
	 * <br>
	 * Zeilenformat: Dateipfad im Repository '\t' Änderungs-Zeitstempel im ISO 8601-Format "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
	 */
	public static final String FILELIST_CACHE_FILEPATH = "/filelist.cache";

	@NotNull
	protected final Logger logger;

	/**
	 * Basisordner des Repositories. Nicht <code>null</code>.
	 */
	@NotNull
	protected final File repositoryBase;

	/**
	 * Absoluter Pfad des Basisordners. Nicht <code>null</code>.
	 */
	@NotNull
	protected final String repositoryBasePath;

	/**
	 * Cacht die Metadaten aller Dateien im Repository-Ordner.<br>
	 * Map: Dateipfad innerhalb des Repositories inkl. Dateiendung -> {@link AnyFile}.
	 */
	@NotNull
	protected final Map<String, AnyFile> fileMap;

	/**
	 * Konstruktor.
	 * 
	 * @param logger Logger. Nicht <code>null</code>.
	 * @param repositoryBase Basisordner des Repositories. Nicht
	 *        <code>null</code>.
	 */
	public FilesystemRepositoryService(@NotNull Logger logger, @NotNull File repositoryBase) {
		super();
		this.logger = logger;
		this.repositoryBase = repositoryBase;
		this.repositoryBasePath = repositoryBase.getAbsolutePath();
		fileMap = new HashMap<>();
		logger.write("Repository base path: " + this.repositoryBasePath);
	}

	public void init() {
		if (!readCacheFile()) {
			rebuildCache();
		}
	}

	/**
	 * Liest die Liste aller Dateien im Repository aus der Cachedatei ein.
	 * 
	 * @return Konnte die Cachedatei erfolgreich eingelesen werden?
	 */
	protected boolean readCacheFile() {
		String cacheContent;
		AnyFile fileListCacheFile = new AnyFile(FILELIST_CACHE_FILEPATH, null);
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

	@NotNull
	protected Map<String, AnyFile> parseCacheContent(@NotNull String cacheContent) throws ServiceException {
		try {
			Map<String, AnyFile> result = new HashMap<>();
			BufferedReader reader = new BufferedReader(new StringReader(cacheContent));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					// Leerzeile (am Ende) ignorieren
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
	 * Schreibt die Cachedatei mit der Dateiliste.
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
			if (contentTimestampStr != null) {
				sb.append(contentTimestampStr);
			}

			sb.append('\n');
		}
		String cacheContent = sb.toString();

		// Datei schreiben
		AnyFile fileListCacheFile = new AnyFile(FILELIST_CACHE_FILEPATH, null);
		try {
			writeTextFile(fileListCacheFile, cacheContent);
		} catch (ServiceException e) {
			// nur Fehlermeldung loggen, dieser Fehler kann ansonsten toleriert werden
			logger.write("Error writing cache file " + FILELIST_CACHE_FILEPATH, e);
		}
	}

	public void rebuildCache() {
		List<File> files = new ArrayList<>();
		listFilesInFilesystem(repositoryBase, files);
		logger.write("Rebuilding repository cache, found " + files.size() + " files in repository folder");

		Map<String, AnyFile> newFileMap = new HashMap<>();
		for (File file : files) {
			String filesystemFilePath = file.getAbsolutePath();

			// Seitenname und Zeitstempel bestimmen
			String filePath = filesystem2RepositoryPath(filesystemFilePath);
			if (filePath == null) {
				// ungültiger Dateiname --> ignorieren
				continue;
			}
			Date fileTimestamp = new Date(file.lastModified());

			// Datei noch nicht im Cache vorhanden oder nicht mehr aktuell?
			AnyFile newAnyFile = new AnyFile(filePath, fileTimestamp);
			newFileMap.put(filePath, newAnyFile);
		}

		// Cache aktualisieren
		fileMap.clear();
		fileMap.putAll(newFileMap);

		// Cachedatei aktualisieren
		writeCacheFile();
	}

	/**
	 * Listet rekursiv alle Dateien im Repository-Ordner auf.
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

	@Nullable
	@Override
	public synchronized AnyFile getFile(@NotNull String filePath) {
		return fileMap.get(filePath);
	}

	@NotNull
	@Override
	public synchronized Set<AnyFile> getFiles() {
		return new HashSet<>(fileMap.values());
	}

	@NotNull
	@Override
	public synchronized Set<AnyFile> getModifiedAfter(Date modifiedAfter) {
		Set<AnyFile> result = new HashSet<>();
		for (AnyFile anyFile : fileMap.values()) {
			if (anyFile.getContentTimestamp() != null && (modifiedAfter == null || modifiedAfter.before(anyFile.getContentTimestamp()))) {
				result.add(anyFile);
			}
		}
		return result;
	}

	@NotNull
	@Override
	public List<AnyFile> getLastModifiedFiles(int count, @NotNull Predicate<AnyFile> filter) {
		List<AnyFile> fileList = new ArrayList<>(fileMap.values());
		fileList.removeIf(filter.negate());
		fileList.sort((anyFile1, anyFile2) -> {
			Date timestamp1 = anyFile1.getContentTimestamp();
			Date timestamp2 = anyFile2.getContentTimestamp();
			if (timestamp1 != null && timestamp2 != null) {
				return timestamp2.compareTo(timestamp1);
			} else if (timestamp1 != null) {
				// timestamp2 == null
				return -1;
			} else if (timestamp2 != null) {
				// timestamp1 == null
				return 1;
			} else {
				// both are null
				return 0;
			}
		});
		return fileList.subList(0, Math.min(fileList.size(), count));
	}

	@Override
	public synchronized void deleteFile(@NotNull AnyFile anyFile) throws ServiceException {
		String filePath = anyFile.getFilePath();
		try {
			// Datei im Dateisystem löschen
			String filename = repository2FilesystemPath(filePath);
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

		// Cache aktualisieren
		fileMap.remove(filePath);
		writeCacheFile();
		logger.write("File '" + filePath + "' deleted");
	}

	@Override
	@NotNull
	public synchronized String readTextFile(@NotNull AnyFile anyFile) throws ServiceException {
		byte[] contentBytes = readBinaryFile(anyFile);
		return new String(contentBytes, StandardCharsets.UTF_8);
	}

	@NotNull
	@Override
	public synchronized AnyFile writeTextFile(@NotNull AnyFile anyFile, @NotNull String content) throws ServiceException {
		byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
		return writeBinaryFile(anyFile, contentBytes, null);
	}

	@Override
	@NotNull
	public synchronized byte[] readBinaryFile(@NotNull AnyFile anyFile) throws ServiceException {
		String filePath = anyFile.getFilePath();
		filePath = PathUtils.makeWebPathAbsolute(filePath, null);
		String filename = repository2FilesystemPath(filePath);
		File file = new File(filename);

		// Ggf. Cache aktualisieren
		if (!fileMap.containsKey(filePath) && file.exists()) {
			logger.write("Detected new file '" + filePath + "' in repository, adding to cache");
			Date fileTimestamp = new Date(file.lastModified());
			AnyFile newAnyFile = new AnyFile(filePath, fileTimestamp);
			fileMap.put(filePath, newAnyFile);

			// Cache schreiben
			if (!FILELIST_CACHE_FILEPATH.equals(filePath)) {
				// Cachedatei selbst ausnehmen, damit die Cachedatei nicht schon
				// vor dem Einlesen derselben mit leerem Inhalt überschrieben wird
				writeCacheFile();
			}
		}

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

	@NotNull
	public synchronized AnyFile writeBinaryFile(@NotNull AnyFile anyFile, @NotNull byte[] content, @Nullable Date contentTimestamp) throws ServiceException {
		String filePath = anyFile.getFilePath();
		filePath = PathUtils.makeWebPathAbsolute(filePath, null);
		String filename = repository2FilesystemPath(filePath);
		File file = new File(filename);

		// Datei schreiben
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

		// Cache aktualisieren
		if (contentTimestamp == null) {
			contentTimestamp = new Date(file.lastModified());
		}
		AnyFile newAnyFile = new AnyFile(filePath, contentTimestamp);
		fileMap.put(filePath, newAnyFile);
		logger.write("Content for file '" + filePath + "' successfully written");

		// Cache schreiben
		if (!FILELIST_CACHE_FILEPATH.equals(filePath)) {
			// Cachedatei selbst ausnehmen, damit keine Endlosrekursion entsteht
			writeCacheFile();
		}
		return newAnyFile;
	}

	/**
	 * Legt alle Unterordner im Repository an, falls sie noch nicht existieren,
	 * damit die angegebene Datei angelegt werden kann.
	 * 
	 * @param file Datei, deren Ordner angelegt werden soll. Nicht
	 *        <code>null</code>.
	 * @throws ServiceException Wenn ein Fehler auftitt
	 */
	protected void createFolders(File file) throws ServiceException {
		File fileFolder = file.getParentFile();
		if (!fileFolder.exists() && !fileFolder.mkdirs()) {
			String message = "Error creating folder '" + fileFolder.getAbsolutePath() + "'";
			logger.write(message);
			throw new ServiceException(message);
		}
	}

	/**
	 * Generiert aus einem Dateipfad im Repository den zugehörigen absoluten
	 * Dateipfad im Dateisystem. Dabei werden Sonderzeichen umgewandelt.
	 * Enthaltene Ordner-Trennzeichen '/' werden automatisch in das
	 * entsprechende betriebssystemspezifische Trennzeichen umgewandelt.<br>
	 * <br>
	 * Beispiel:<br>
	 * Repositorypfad: <tt>/pfad/zur/datei</tt><br>
	 * Dateisystempfad: <tt>/pfad/zum/repository/pfad/zur/datei</tt>
	 * 
	 * @param repositoryPath Relativer Pfad innerhalb des Repositories.
	 * @return Absoluter Pfad im Dateisystem.
	 */
	@Contract(value = "null -> null; !null -> !null", pure = true)
	@Nullable
	protected String repository2FilesystemPath(String repositoryPath) {
		if (repositoryPath == null) {
			return null;
		}

		// in Windows nicht erlaubte Zeichen: "*/:<>?\|
		// in Linux nicht erlaubte Zeichen: /
		// '%' wird als Escape-Zeichen verwendet und ebenfalls allein verboten
		// '/' wird als Ordnertrennzeichen interpretiert
		// "." und ".." werden escaped
		final String forbiddenChars = "\"%*:<>?\\|";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < repositoryPath.length(); i++) {
			char c = repositoryPath.charAt(i);
			if (c < 32 || c > 255 || forbiddenChars.indexOf(c) >= 0
					|| c == '.' && (i - 1 >= 0 && i + 2 <= repositoryPath.length() && "/./".contentEquals(repositoryPath.subSequence(i - 1, i + 2))
							|| i - 1 >= 0 && i + 3 <= repositoryPath.length() && "/../".contentEquals(repositoryPath.subSequence(i - 1, i + 3))
							|| i - 2 >= 0 && i + 2 <= repositoryPath.length() && "/../".contentEquals(repositoryPath.subSequence(i - 2, i + 2)))) {
				// in Darstellung "%wxyz" umwandeln
				String hex = Integer.toString(c, 16);
				sb.append('%');
				for (int k = hex.length(); k < 4; k++) {
					sb.append('0'); // mit führenden Nullen auffüllen
				}
				sb.append(hex);
			} else {
				sb.append(c); // Zeichen direkt übernehmen
			}
		}

		// Pfad zusammenbauen
		String filePath = PathUtils.convertWebPath2FilePath(sb.toString());
		return PathUtils.concatFilePaths(repositoryBasePath, filePath);
	}

	/**
	 * Wandelt einen absoluten Dateipfad im Dateisystem um in einen relativen
	 * Pfad innerhalb des Repositories. Enthaltene betriebssystemspezifische
	 * Ordner-Trennzeichen werden automatisch in das Trennzeichen '/'
	 * umgewandelt.<br>
	 * <br>
	 * Beispiel:<br>
	 * Dateisystempfad: <tt>/pfad/zum/repository/pfad/zur/datei</tt><br>
	 * Repositorypfad: <tt>/pfad/zur/datei</tt>
	 * 
	 * @param filesystemPath Absoluter Pfad im Dateisystem.
	 * @return Relativer Pfad innerhalb des Repositories. <code>null</code> ->
	 *         Pfad befindet sich nicht innerhalb des Repositories oder ist
	 *         ungültig.
	 */
	@Nullable
	protected String filesystem2RepositoryPath(@Nullable String filesystemPath) {
		if (filesystemPath == null) {
			return null;
		}

		if (!filesystemPath.startsWith(repositoryBasePath)) {
			logger.write("filePath2PagePath: Invalid file name '" + filesystemPath + "', doesn't exist in repository '" + repositoryBasePath + "'");
			return null;
		}

		// Pfad zum Repository abschneiden
		StringBuilder sb = new StringBuilder(filesystemPath);
		sb.delete(0, repositoryBasePath.length());
		if (sb.length() == 0 || sb.charAt(0) != File.separatorChar) {
			// führendes Separatorzeichen einfügen
			sb.insert(0, File.separatorChar);
		}

		// Escaping "%wxyz" zurückübersetzen
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			if (c == '%' && i + 4 < sb.length()) {
				try {
					// Darstellung "%wxyz" zurückübersetzen in ein Zeichen
					String hex = sb.substring(i + 1, i + 5); // Hex-Zeichen ohne
					// '%'
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
