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
import net.moasdawiki.service.wiki.structure.PageElement;
import net.moasdawiki.service.wiki.structure.PageElementList;
import net.moasdawiki.service.wiki.structure.Parent;
import net.moasdawiki.service.wiki.structure.WikiPage;
import net.moasdawiki.util.DateUtils;
import org.jetbrains.annotations.NotNull;

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
 * 
 * @author Herbert Reiter
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
	private final Map<String, Set<String>> childParentMap;

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
	private final LinkedList<String> viewHistory;

	/**
	 * Konstruktor.
	 */
	public WikiServiceImpl(@NotNull Logger logger, @NotNull RepositoryService repositoryService) {
		this.logger = logger;
		this.repositoryService = repositoryService;
		this.childParentMap = new HashMap<>();
		this.wikiFileMap = new HashMap<>();
		this.viewHistory = new LinkedList<>();

		// Cache initialisieren
		reset();
	}

	public void reset() {
		if (!readChildParentCacheFile()) {
			rebuildCache();
		}
	}

	/**
	 * Liest die Cachedatei mit den Kind-Vater-Beziehungen ein und aktualisiert die
	 * Einträge in der wikiFileMap.
	 */
	boolean readChildParentCacheFile() {
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
		Map<String, Set<String>> newParentChildMap = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new StringReader(cacheContent))) {
			int numLines = 0;

			// Zeitstempel in erster Zeile, für Rückwärtskompatibilität
			reader.readLine();
			numLines++;

			// Parent-Mappings in nachfolgenden Zeilen
			String line;
			while ((line = reader.readLine()) != null) {
				numLines++;
				String[] token = line.split("\\t");
				if (token.length == 0) {
					// Zeile ignorieren, evtl. Leerzeile am Dateiende
					continue;
				}
				String wikiFilePath = token[0].trim();
				Set<String> children = new HashSet<>(Arrays.asList(token).subList(1, token.length));
				newParentChildMap.put(wikiFilePath, children);
			}
			logger.write(numLines + " lines read from parent relations cache file");
		} catch (Exception e) {
			logger.write("Error reading cache file " + parentRelationsCacheFile.getFilePath(), e);
			return false;
		}

		// Update internal cache
		this.childParentMap.clear();
		this.childParentMap.putAll(newParentChildMap);

		// Update children references in wiki pages
		updateAllChildrenReferences();
		return true;
	}

	/**
	 * Aktualisiert die Kind-Beziehungen in allen Einträgen in wikiFileMap
	 * anhand des Vater-Kind-Caches.
	 */
	private void updateAllChildrenReferences() {
		// Vorher alle Kind-Beziehungen löschen
		for (String wikiFilePath : wikiFileMap.keySet()) {
			wikiFileMap.get(wikiFilePath).getChildren().clear();
		}

		// Kind-Beziehungen auffüllen
		for (String childFilePath : childParentMap.keySet()) {
			Set<String> parentFilePaths = childParentMap.get(childFilePath);
			for (String parentFilePath : parentFilePaths) {
				WikiFile wikiFile = wikiFileMap.get(parentFilePath);
				if (wikiFile != null) {
					wikiFile.getChildren().add(childFilePath);
				}
			}
		}
	}

	/**
	 * Schreibt alle Kind-Vater-Beziehungen in die Cachedatei.
	 */
	private void writeChildParentCacheFile() {
		// Zeitstempel in erste Zeile schreiben
		StringBuilder sb = new StringBuilder();
		String timestampStr = DateUtils.formatUtcDate(new Date());
		sb.append(timestampStr).append('\n');

		// Liste alphabetisch sortiert ausgeben
		List<String> filePathList = new ArrayList<>(childParentMap.keySet());
		Collections.sort(filePathList);
		for (String wikiFilePath : filePathList) {
			Set<String> parents = childParentMap.get(wikiFilePath);
			sb.append(wikiFilePath);
			for (String parentFilePath : parents) {
				sb.append('\t');
				sb.append(parentFilePath);
			}
			sb.append('\n');
		}
		String cacheContent = sb.toString();

		// Datei schreiben
		AnyFile parentRelationsCacheFile = new AnyFile(CHILD_PARENT_CACHE_FILEPATH);
		try {
			repositoryService.writeTextFile(parentRelationsCacheFile, cacheContent);
		} catch (ServiceException e) {
			// nur Fehlermeldung loggen, dieser Fehler kann ansonsten toleriert werden
			logger.write("Error writing cache file " + parentRelationsCacheFile.getFilePath(), e);
		}
	}

	@Override
	public synchronized void rebuildCache() {
		logger.write("Rebuilding cache");
		readAllWikiFiles();
		fixViewHistory();
		logger.write("Finished rebuilding cache");
	}

	/**
	 * Reads all wiki pages from scratch.
	 */
	private void readAllWikiFiles() {
		// Clear internal cache
		wikiFileMap.clear();
		childParentMap.clear();

		// Read all wiki files from scratch
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

	/**
	 * Entfernt nicht mehr vorhandene Wikidateien aus der Liste der zuletzt
	 * angezeigten Wikiseiten.
	 */
	private void fixViewHistory() {
		viewHistory.removeIf(wikiFilePath -> !wikiFileMap.containsKey(wikiFilePath));
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
	public synchronized WikiText readWikiText(@NotNull String wikiFilePath, Integer fromPos, Integer toPos) throws ServiceException {
		WikiFile wikiFile = getWikiFileInternal(wikiFilePath, true);

		WikiText wikiText = new WikiText();
		wikiText.text = wikiFile.getWikiText();

		// ggf. auf Ausschnitt der Wikiseite reduzieren
		if (fromPos != null && toPos != null) {
			try {
				wikiText.text = wikiText.text.substring(fromPos, toPos);
				wikiText.fromPos = fromPos;
				wikiText.toPos = toPos;
			} catch (IndexOutOfBoundsException e) {
				throw new ServiceException("Invalid values for fromPos=" + fromPos + " and toPos=" + toPos, e);
			}
		}
		return wikiText;
	}

	@NotNull
	@Override
	public synchronized WikiFile writeWikiText(@NotNull String wikiFilePath, @NotNull WikiText wikiText) throws ServiceException {
		// Alte Vater- und Kind-Verweise entfernen
		uninstallParentAndChildLinks(wikiFilePath);

		// Neuen Wikitext bestimmen, ggf. Seitenausschnitt ersetzen
		WikiFile oldWikiFile = wikiFileMap.get(wikiFilePath);
		String newText;
		if (wikiText.fromPos != null && wikiText.toPos != null) {
			if (oldWikiFile == null) {
				String message = "Cannot write page section for wiki file '" + wikiFilePath + "', because file doesn't exist";
				logger.write(message);
				throw new ServiceException(message);
			}
			StringBuilder mergedWikiText = new StringBuilder(oldWikiFile.getWikiText());
			mergedWikiText.replace(wikiText.fromPos, wikiText.toPos, wikiText.text);
			newText = mergedWikiText.toString();
		} else {
			newText = wikiText.text;
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
		installParentAndChildLinks(newWikiFile);
		writeChildParentCacheFile();

		logger.write("Wiki file '" + wikiFilePath + "' successfully written, " + newText.length() + " characters");
		return newWikiFile;
	}

	@NotNull
	@Override
	public Set<String> getModifiedByFileTimestamp(Date modifiedAfter) {
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
	public synchronized List<String> getLastModifiedWikiFiles(int count) {
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
	 * Returns the requestes wiki page. Takes it from the internal cache if available, otherwise
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
		installParentAndChildLinks(newWikiFile);

		// Persist parent child cache
		if (persistParentChildCache) {
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
			throw new ServiceException("File '" + wikiFilePath + "' does not exist");
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
	private boolean isWikiFilePath(String repositoryPath) {
		return repositoryPath.endsWith(PAGE_SUFFIX);
	}

	/**
	 * Schneidet die Dateiendung beim Namen einer Wikidatei ab.
	 * 
	 * @param repositoryPath Dateipfad innerhalb des Repositories. Nicht
	 *        <code>null</code>.
	 * @return Name der Wikidatei. Nicht <code>null</code>.
	 */
	private String repositoryPath2WikiFilePath(String repositoryPath) {
		if (isWikiFilePath(repositoryPath)) {
			return repositoryPath.substring(0, repositoryPath.length() - PAGE_SUFFIX.length());
		} else {
			return repositoryPath;
		}
	}

	/**
	 * Ergänzt den Namen einer Wikidatei um die Dateiendung.
	 * 
	 * @param wikiFilePath Name der Wikidatei. Nicht <code>null</code>.
	 * @return Dateipfad innerhalb des Repositories. Nicht <code>null</code>.
	 */
	private String wikiFilePath2RepositoryPath(String wikiFilePath) {
		return wikiFilePath + PAGE_SUFFIX;
	}

	/**
	 * Durchsucht die angegebene Wikiseite nach Parent-Beziehungen und fügt
	 * diese in die Liste der Parent-Links und im Child-Parent-Cache ein.
	 * Ergänzt zudem die Children-Links in anderen Wikiseiten.
	 */
	private void installParentAndChildLinks(@NotNull WikiFile wikiFile) {
		// Vater-Verweise aus der Wikiseite extrahieren
		Set<String> parentFilePaths = new HashSet<>();
		PageElementViewer<Parent> parentsCollector = parent -> {
			WikiPage wikiPage = WikiHelper.getContextWikiPage(parent, false);
			String wikiFilePath = WikiHelper.getAbsolutePagePath(parent.getParentPagePath(), wikiPage);
			parentFilePaths.add(wikiFilePath);
		};
		WikiHelper.viewPageElements(wikiFile.getWikiPage(), parentsCollector, Parent.class, false);

		// Vater-Verweise in der Wikiseite eintragen
		wikiFile.getParents().clear();
		wikiFile.getParents().addAll(parentFilePaths);

		// Vater-Verweise im Kind-Vater-Cache eintragen
		String wikiFilePath = wikiFile.getWikiFilePath();
		childParentMap.put(wikiFilePath, parentFilePaths);

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
	}

	/**
	 * Entfernt Vater-Verweise aus dem Cache und Children-Links von anderen Wikiseiten.
	 * Wird aufgerufen, wenn eine Wikiseite gelöscht oder durch eine neuere Version ersetzt wird.
	 */
	private void uninstallParentAndChildLinks(@NotNull String wikiFilePath) {
		// Vater-Verweise im Kind-Vater-Cache eintragen
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
