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

package net.moasdawiki.service.wiki;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.wiki.parser.WikiParser;
import net.moasdawiki.service.wiki.structure.*;
import net.moasdawiki.util.DateUtils;
import net.moasdawiki.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 * Serviceschicht für den Zugriff auf alle Wikidateien im Wiki-Repository.<br>
 * <br>
 * Behält eingelesene Wikiseiten im Cache, inkl. Parsebaum. Es wird vermieden,
 * unnötig viele Wikiseiten einzulesen, um den Serverstart zu beschleunigen.<br>
 * <br>
 * Liest die Parent-Liste aus der Cachedatei. Wenn die Cachedatei nicht
 * existiert, wird sie automatisch generiert und dazu alle Wikiseiten eingelesen und geparst.
 */
public class WikiServiceImpl implements WikiService {

	private static final String PAGE_SUFFIX = ".txt";

	@NotNull
	private final Logger logger;

	@NotNull
	private final RepositoryService repositoryService;

	/**
	 * Pfad der Cachedatei mit der Vater-Kind-Liste aller Wikidateien.<br>
	 * <br>
	 * Zeilenformat: Dateipfad im Repository '\t' Dateipfad der Kindseite 1 '\t' Dateipfad der Kindseite 2 usw.
	 */
	public static final String CHILD_PARENT_CACHE_FILEPATH = "/parentrelations.cache";

	/**
	 * Cacht die Kind-Vater-Beziehungen aller Wikiseiten im Repository.
	 * Wird beim Starten der Anwendung aus der Cachedatei geladen und bei jeder Änderung einer
	 * Wikiseite aktualisiert.
	 */
	final Map<String, Set<String>> childParentMap;

	/**
	 * Enthält alle Wikidateien, die im Repository sind, dient als Cache. Die
	 * Felder wikiText und wikiPage (Parsebaum) können <code>null</code> sein,
	 * sie werden dann beim ersten Aufruf von {@link #getWikiFile(String)}
	 * gefüllt. Nicht <code>null</code>.<br>
	 * Map: Dateipfad ohne Endung -> WikiFile.
	 */
	@NotNull
	final Map<String, WikiFile> wikiFileMap;

	/**
	 * Liste der zuletzt angesehenen Wikiseiten, chronologisch aufsteigend
	 * sortiert, d.h. die zuletzt besuchte Wikiseite befindet sich am
	 * Listenende. Ist nach dem Serverstart zunächst leer. Nicht
	 * <code>null</code>.
	 */
	@NotNull
	final LinkedList<String> viewHistory;

	/**
	 * Konstruktor.
	 */
	public WikiServiceImpl(@NotNull Logger logger, @NotNull RepositoryService repositoryService) {
		this.logger = logger;
		this.repositoryService = repositoryService;
		this.childParentMap = new HashMap<>();
		this.wikiFileMap = new HashMap<>();
		this.viewHistory = new LinkedList<>();

		// Initialize cache
		reset();
	}

	public void reset() {
		childParentMap.clear();
		wikiFileMap.clear();
		viewHistory.clear();
		if (!readChildParentCacheFile()) {
			rebuildCache();
		}
	}

	/**
	 * Read the cache file and update the wikiFileMap.
	 */
	private boolean readChildParentCacheFile() {
		// Read cache file content
		AnyFile parentRelationsCacheFile = new AnyFile(CHILD_PARENT_CACHE_FILEPATH);
		String cacheContent;
		try {
			cacheContent = repositoryService.readTextFile(parentRelationsCacheFile);
		} catch (ServiceException e) {
			logger.write("Error reading cache file " + parentRelationsCacheFile.getFilePath());
			return false;
		}

		// Parse cache content
		try (BufferedReader reader = new BufferedReader(new StringReader(cacheContent))) {
			// Read timestamp from first line, for backwards compatibility only
			reader.readLine();

			// Parse parent mappings
			Map<String, Set<String>> parsedMap = StringUtils.parseMap(reader);
			childParentMap.putAll(parsedMap);
			logger.write(parsedMap.size() + " keys read from parent relations cache file");
		} catch (Exception e) {
			logger.write("Error reading cache file " + parentRelationsCacheFile.getFilePath(), e);
			return false;
		}
		return true;
	}

	/**
	 * Write the content in wikiFileMap into the cache file.
	 */
	private void writeChildParentCacheFile() {
		// Write timestamp in first line
		StringBuilder sb = new StringBuilder();
		String timestampStr = DateUtils.formatUtcDate(new Date());
		sb.append(timestampStr).append('\n');

		// Write list sorted alphabetically
		String mapStr = StringUtils.serializeMap(childParentMap);
		sb.append(mapStr);

		// Write file
		AnyFile parentRelationsCacheFile = new AnyFile(CHILD_PARENT_CACHE_FILEPATH);
		try {
			repositoryService.writeTextFile(parentRelationsCacheFile, sb.toString());
		} catch (ServiceException e) {
			// in case of error only log error
			logger.write("Error writing cache file " + parentRelationsCacheFile.getFilePath(), e);
		}
	}

	private synchronized void rebuildCache() {
		logger.write("Rebuilding cache");
		readAllWikiFiles();
		logger.write("Finished rebuilding cache");
	}

	/**
	 * Reads all wiki pages from scratch.
	 */
	private void readAllWikiFiles() {
		Set<AnyFile> repositoryFiles = repositoryService.getFiles();
		for (AnyFile repositoryFile : repositoryFiles) {
			String repositoryPath = repositoryFile.getFilePath();
			if (!isWikiFilePath(repositoryPath)) {
				// no Wiki file, to be ignored
				continue;
			}

			String wikiFilePath = repositoryPath2WikiFilePath(repositoryPath);
			try {
				getWikiFileInternal(wikiFilePath, false);
			} catch (ServiceException e) {
				logger.write("Error reading wiki file '" + wikiFilePath + "', ignoring it");
			}
		}
		writeChildParentCacheFile();
	}

	@Override
	@NotNull
	public synchronized Set<String> getWikiFilePaths() {
        Set<String> result = new HashSet<>();
	    Set<AnyFile> files = repositoryService.getFiles();
	    for (AnyFile anyFile : files) {
	        String filePath = anyFile.getFilePath();
	        if (isWikiFilePath(filePath)) {
	            result.add(repositoryPath2WikiFilePath(filePath));
            }
        }
		return result;
	}

	@Override
	public synchronized boolean existsWikiFile(@NotNull String wikiFilePath) {
		if (wikiFileMap.containsKey(wikiFilePath)) {
			return true;
		}
		String filePath = wikiFilePath2RepositoryPath(wikiFilePath);
		return repositoryService.getFile(filePath) != null;
	}

	/**
	 * Gibt die angeforderte Wikidatei zurück. Erstellt eine Kopie, damit das
	 * Original nicht verändert werden kann. Falls noch nicht vorhanden, wird
	 * vorher der Wikitext eingelesen und geparst. Im zurückgegebenen
	 * WikiFile-Objekt sind alle Felder gefüllt.
	 */
	@NotNull
	@Override
	public synchronized WikiFile getWikiFile(@NotNull String wikiFilePath) throws ServiceException {
		WikiFile wikiFile = getWikiFileInternal(wikiFilePath, true);
		return wikiFile.cloneTyped();
	}

	@Override
	public synchronized void deleteWikiFile(@NotNull String wikiFilePath) throws ServiceException {
		// Remove from internal cache
		uninstallParentAndChildLinks(wikiFilePath);
		wikiFileMap.remove(wikiFilePath);
		viewHistory.remove(wikiFilePath);

		// Delete from repository
		String filePath = wikiFilePath2RepositoryPath(wikiFilePath);
		AnyFile anyFile = new AnyFile(filePath);
		repositoryService.deleteFile(anyFile);

		// Persist cache
		writeChildParentCacheFile();
	}

	@NotNull
	@Override
	public synchronized WikiText readWikiText(@NotNull String wikiFilePath, @Nullable Integer fromPos, @Nullable Integer toPos) throws ServiceException {
		WikiFile wikiFile = getWikiFileInternal(wikiFilePath, true);
		if (fromPos != null && toPos != null) {
			// Auf Ausschnitt der Wikiseite reduzieren
			try {
				return new WikiText(wikiFile.getWikiText().substring(fromPos, toPos), fromPos, toPos);
			} catch (IndexOutOfBoundsException e) {
				throw new ServiceException("Invalid values for fromPos=" + fromPos + " and toPos=" + toPos, e);
			}
		} else {
			return new WikiText(wikiFile.getWikiText());
		}
	}

	@NotNull
	@Override
	public synchronized WikiFile writeWikiText(@NotNull String wikiFilePath, @NotNull WikiText wikiText) throws ServiceException {
		// Alte Vater- und Kind-Verweise entfernen
		uninstallParentAndChildLinks(wikiFilePath);

		// Neuen Wikitext bestimmen, ggf. Seitenausschnitt ersetzen
		String newText;
		if (wikiText.getFromPos() != null && wikiText.getToPos() != null) {
			WikiFile oldWikiFile = wikiFileMap.get(wikiFilePath);
			if (oldWikiFile == null) {
				String message = "Cannot write page section for wiki file '" + wikiFilePath + "', because file doesn't exist";
				logger.write(message);
				throw new ServiceException(message);
			}
			StringBuilder mergedWikiText = new StringBuilder(oldWikiFile.getWikiText());
			mergedWikiText.replace(wikiText.getFromPos(), wikiText.getToPos(), wikiText.getText());
			newText = mergedWikiText.toString();
		} else {
			newText = wikiText.getText();
		}

		// Datei schreiben
		AnyFile newRepositoryFile;
		try {
			String repositoryPath = wikiFilePath2RepositoryPath(wikiFilePath);
			AnyFile oldRepositoryFile = new AnyFile(repositoryPath);
			newRepositoryFile = repositoryService.writeTextFile(oldRepositoryFile, newText);
		} catch (ServiceException e) {
			String message = "Error saving wiki file '" + wikiFilePath + "'";
			logger.write(message, e);
			throw new ServiceException(message, e);
		}

		// Wikiseite parsen
		PageElement pageContent = parseWikiText(newText);
		WikiPage wikiPage = new WikiPage(wikiFilePath, pageContent, 0, newText.length());
		WikiFile newWikiFile = new WikiFile(wikiFilePath, newText, wikiPage, newRepositoryFile);
		wikiFileMap.put(wikiFilePath, newWikiFile);

		// Vater- und Kind-Verweise setzen
		if (installParentAndChildLinks(newWikiFile)) {
			writeChildParentCacheFile();
		}

		logger.write("Wiki file '" + wikiFilePath + "' successfully written, " + newText.length() + " characters");
		return newWikiFile;
	}

	@NotNull
	@Override
	public Set<String> getModifiedAfter(Date modifiedAfter) {
		Set<AnyFile> modifiedFiles = repositoryService.getModifiedAfter(modifiedAfter);
		if (modifiedFiles.isEmpty()) {
			return Collections.emptySet();
		}

		Set<String> result = new HashSet<>();
		for (AnyFile anyFile : modifiedFiles) {
			String repositoryPath = anyFile.getFilePath();
			if (!isWikiFilePath(repositoryPath)) {
				// keine Wikiseite
				continue;
			}
			String wikiFilePath = repositoryPath2WikiFilePath(repositoryPath);
			result.add(wikiFilePath);
		}
		return result;
	}

	@NotNull
	@Override
	public synchronized List<String> getLastModified(int count) {
		List<AnyFile> fileList = repositoryService.getLastModifiedFiles(count, anyFile -> isWikiFilePath(anyFile.getFilePath()));
		ArrayList<String> result = new ArrayList<>();
		for (int i = 0; i < fileList.size() && (count == -1 || i < count); i++) {
			String filePath = fileList.get(i).getFilePath();
			result.add(repositoryPath2WikiFilePath(filePath));
		}
		return result;
	}

	@NotNull
	@Override
	public synchronized List<String> getLastViewedWikiFiles(int count) {
		ArrayList<String> result = new ArrayList<>();
		for (int i = 0; i < viewHistory.size() && (count == -1 || i < count); i++) {
			result.add(viewHistory.get(viewHistory.size() - 1 - i));
		}
		return result;
	}

	@Override
	public synchronized void addLastViewedWikiFile(@NotNull String wikiFilePath) {
		viewHistory.remove(wikiFilePath);
		viewHistory.add(wikiFilePath);
	}

	@NotNull
	@Override
	public PageElementList parseWikiText(@NotNull String wikiText) throws ServiceException {
		try (Reader reader = new StringReader(wikiText)) {
			WikiParser wikiParser = new WikiParser(reader);
			return wikiParser.parse();
		} catch (Exception e) {
			throw new ServiceException("Error parsing wiki text", e);
		}
	}

	/**
	 * Returns the requested wiki page. Takes it from the internal cache if available, otherwise
	 * tries to read the file from the repository and updates the cache.
	 */
	@NotNull
	private WikiFile getWikiFileInternal(@NotNull String wikiFilePath, boolean persistParentChildCache) throws ServiceException {
		// Take wiki page from cache if available
		WikiFile wikiFile = wikiFileMap.get(wikiFilePath);
		if (wikiFile != null) {
			return wikiFile;
		}

		// Read wiki page from repository
		WikiFile newWikiFile = getWikiFileFromRepository(wikiFilePath);

		// Update wikiFileMap
		wikiFileMap.put(wikiFilePath, newWikiFile);
		if (installParentAndChildLinks(newWikiFile) && persistParentChildCache) {
			writeChildParentCacheFile();
		}

		return newWikiFile;
	}

	/**
	 * Reads the wiki file from repository and parses it. The method doesn't use or modify the internal cache.
	 */
	@NotNull
	private WikiFile getWikiFileFromRepository(@NotNull String wikiFilePath) throws ServiceException {
		String filePath = wikiFilePath2RepositoryPath(wikiFilePath);
		AnyFile anyFile = repositoryService.getFile(filePath);
		if (anyFile == null) {
			throw new ServiceException("File '" + filePath + "' does not exist");
		}

		String wikiText = repositoryService.readTextFile(anyFile);
		PageElement pageContent = parseWikiText(wikiText);
		logger.write("Wiki file '" + anyFile.getFilePath() + "' parsed");

		WikiPage wikiPage = new WikiPage(wikiFilePath, pageContent, 0, wikiText.length());
		return new WikiFile(wikiFilePath, wikiText, wikiPage, anyFile);
	}

	/**
	 * Gibt zurück, ob der angegebene Dateiname den Suffix einer Wikidatei hat.
	 */
	static boolean isWikiFilePath(@NotNull String repositoryPath) {
		return repositoryPath.endsWith(PAGE_SUFFIX);
	}

	/**
	 * Schneidet die Dateiendung beim Namen einer Wikidatei ab.
	 * 
	 * @param repositoryPath Dateipfad innerhalb des Repositories.
	 * @return Name der Wikidatei.
	 */
	@NotNull
	static String repositoryPath2WikiFilePath(@NotNull String repositoryPath) {
		if (isWikiFilePath(repositoryPath)) {
			return repositoryPath.substring(0, repositoryPath.length() - PAGE_SUFFIX.length());
		} else {
			return repositoryPath;
		}
	}

	/**
	 * Ergänzt den Namen einer Wikidatei um die Dateiendung.
	 * 
	 * @param wikiFilePath Name der Wikidatei.
	 * @return Dateipfad innerhalb des Repositories.
	 */
	@NotNull
	static String wikiFilePath2RepositoryPath(@NotNull String wikiFilePath) {
		return wikiFilePath + PAGE_SUFFIX;
	}

	/**
	 * Durchsucht die angegebene Wikiseite nach Parent-Beziehungen und fügt
	 * diese in die Liste der Parent-Links und im Child-Parent-Cache ein.
	 * Ergänzt zudem die Children-Links in anderen Wikiseiten.
	 *
	 * @return true if the childParentMap cache was changed.
	 */
	private boolean installParentAndChildLinks(@NotNull WikiFile wikiFile) {
		// Vater-Verweise aus der Wikiseite extrahieren
		Set<String> parentFilePaths = new HashSet<>();
		PageElementConsumer<Parent, Set<String>> consumer = (parent, context) -> {
			WikiPage wikiPage = WikiHelper.getContextWikiPage(parent, false);
			if (wikiPage != null) {
				String wikiFilePath = WikiHelper.getAbsolutePagePath(parent.getParentPagePath(), wikiPage);
				context.add(wikiFilePath);
			}
		};
		WikiHelper.traversePageElements(wikiFile.getWikiPage(), consumer, Parent.class, parentFilePaths, false);

		// Vater-Verweise in der Wikiseite eintragen
		wikiFile.getParents().clear();
		wikiFile.getParents().addAll(parentFilePaths);

		// Vater-Verweise im Kind-Vater-Cache eintragen
		String wikiFilePath = wikiFile.getWikiFilePath();
		boolean cacheModified = !childParentMap.containsKey(wikiFilePath) || !parentFilePaths.containsAll(childParentMap.get(wikiFilePath));
		if (cacheModified) {
			childParentMap.put(wikiFilePath, parentFilePaths);
		}

		// Children-Links in anderen Wikiseiten aktualisieren
		for (String parentFilePath : parentFilePaths) {
			WikiFile parentWikiFile = wikiFileMap.get(parentFilePath);
			if (parentWikiFile != null) {
				parentWikiFile.getChildren().add(wikiFilePath);
			}
		}

		// Children-Links in dieser Wikiseite ergänzen
		wikiFile.getChildren().clear();
		for (String childFilePath : childParentMap.keySet()) {
			Set<String> parents = childParentMap.get(childFilePath);
			if (parents.contains(wikiFilePath)) {
				wikiFile.getChildren().add(childFilePath);
			}
		}

		return cacheModified;
	}

	/**
	 * Entfernt Vater-Verweise aus dem Cache und Children-Links von anderen Wikiseiten.
	 * Wird aufgerufen, wenn eine Wikiseite gelöscht oder durch eine neuere Version ersetzt wird.
	 */
	private void uninstallParentAndChildLinks(@NotNull String wikiFilePath) {
		// Vater-Verweise aus Kind-Vater-Cache entfernen
		Set<String> parentFilePaths = childParentMap.remove(wikiFilePath);
		if (parentFilePaths == null) {
			return;
		}

		// Children-Links in anderen Wikiseiten entfernen
		for (String parentFilePath : parentFilePaths) {
			WikiFile parentWikiFile = wikiFileMap.get(parentFilePath);
			if (parentWikiFile != null) {
				parentWikiFile.getChildren().remove(wikiFilePath);
			}
		}
	}
}
