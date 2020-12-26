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
 * Provides access to the wiki pages as a parsed syntax tree.
 */
public class WikiService {

	private static final String PAGE_SUFFIX = ".txt";

	@NotNull
	private final Logger logger;

	@NotNull
	private final RepositoryService repositoryService;

	/**
	 * Path of the parent-child cache file.
	 *
	 * Row format: file path in repository '\t' file path of child page 1 '\t' file path of child page 2 etc.
	 */
	public static final String CHILD_PARENT_CACHE_FILEPATH = "/parentrelations.cache";

	/**
	 * Child-parent relation of all wiki pages in the repository.
	 * Is loaded on application start and is kept up to date after every change of a wiki page.
	 */
	@NotNull
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
	 * List of last visited wiki pages since application start.
	 */
	@NotNull
	final LinkedList<String> viewHistory;

	/**
	 * Constructor.
	 */
	public WikiService(@NotNull Logger logger, @NotNull RepositoryService repositoryService) {
		this.logger = logger;
		this.repositoryService = repositoryService;
		this.childParentMap = new HashMap<>();
		this.wikiFileMap = new HashMap<>();
		this.viewHistory = new LinkedList<>();

		// Initialize cache
		reset();
	}

	/**
	 * Reset the cache content, same as application restart.
	 */
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
	 * Read all wiki pages from scratch.
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

	/**
	 * List of all wiki pages.
	 */
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

	/**
	 * Check if the wiki page exists.
	 */
	public synchronized boolean existsWikiFile(@NotNull String wikiFilePath) {
		if (wikiFileMap.containsKey(wikiFilePath)) {
			return true;
		}
		String filePath = wikiFilePath2RepositoryPath(wikiFilePath);
		return repositoryService.getFile(filePath) != null;
	}

	/**
	 * Return the parsed wiki page.
	 * Throws an Exception if the wiki page doesn't exist.
	 */
	@NotNull
	public synchronized WikiFile getWikiFile(@NotNull String wikiFilePath) throws ServiceException {
		WikiFile wikiFile = getWikiFileInternal(wikiFilePath, true);
		return wikiFile.cloneTyped();
	}

	/**
	 * Delete a wiki page.
	 */
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

	/**
	 * Return the raw text of a wiki page or a section of it.
	 * Throws an Exception if the wiki page doesn't exist.
	 *
	 * @param wikiFilePath wiki file path
	 * @param fromPos position of the first character of a section;
	 *                <code>null</code> --> return whole page.
	 * @param toPos position after the last character of a section;
	 *              <code>null</code> --> return whole page.
	 */
	@NotNull
	public synchronized WikiText readWikiText(@NotNull String wikiFilePath, @Nullable Integer fromPos, @Nullable Integer toPos) throws ServiceException {
		WikiFile wikiFile = getWikiFileInternal(wikiFilePath, true);
		if (fromPos != null && toPos != null) {
			// reduce to section
			try {
				return new WikiText(wikiFile.getWikiText().substring(fromPos, toPos), fromPos, toPos);
			} catch (IndexOutOfBoundsException e) {
				throw new ServiceException("Invalid values for fromPos=" + fromPos + " and toPos=" + toPos, e);
			}
		} else {
			return new WikiText(wikiFile.getWikiText());
		}
	}

	/**
	 * Write the raw wiki text of a wiki page or a section of it.
	 * Update internal caches.
	 *
	 * @param wikiFilePath wiki file path
	 * @param wikiText raw text
	 */
	@NotNull
	public synchronized WikiFile writeWikiText(@NotNull String wikiFilePath, @NotNull WikiText wikiText) throws ServiceException {
		uninstallParentAndChildLinks(wikiFilePath);

		// replace section
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

		// write file
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

		// parse wiki text
		PageElement pageContent = parseWikiText(newText);
		WikiPage wikiPage = new WikiPage(wikiFilePath, pageContent, 0, newText.length());
		WikiFile newWikiFile = new WikiFile(wikiFilePath, newText, wikiPage, newRepositoryFile);
		wikiFileMap.put(wikiFilePath, newWikiFile);

		if (installParentAndChildLinks(newWikiFile)) {
			writeChildParentCacheFile();
		}

		logger.write("Wiki file '" + wikiFilePath + "' successfully written, " + newText.length() + " characters");
		return newWikiFile;
	}

	/**
	 * List wiki pages modified after the given date.
	 *
	 * @param modifiedAfter date, files have to be newer;
	 *                      <code>null</code> --> no filter, list all files.
	 */
	@NotNull
	public Set<String> getModifiedAfter(@Nullable Date modifiedAfter) {
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

	/**
	 * List the latest modifies wiki pages.
	 * .
	 * @param count maximum number of pages.
	 *              -1 -> no filter, list all pages
	 */
	@NotNull
	public synchronized List<String> getLastModified(int count) {
		List<AnyFile> fileList = repositoryService.getLastModifiedFiles(count, anyFile -> isWikiFilePath(anyFile.getFilePath()));
		ArrayList<String> result = new ArrayList<>();
		for (int i = 0; i < fileList.size() && (count == -1 || i < count); i++) {
			String filePath = fileList.get(i).getFilePath();
			result.add(repositoryPath2WikiFilePath(filePath));
		}
		return result;
	}

	/**
	 * List the latest viewed wiki pages.
	 *
	 * @param count maximum number of pages.
	 * 	           -1 -> no filter, list all pages
	 */
	@NotNull
	public synchronized List<String> getLastViewedWikiFiles(int count) {
		ArrayList<String> result = new ArrayList<>();
		for (int i = 0; i < viewHistory.size() && (count == -1 || i < count); i++) {
			result.add(viewHistory.get(viewHistory.size() - 1 - i));
		}
		return result;
	}

	/**
	 * Add a wiki page to the list of viewed pages.
	 */
	public synchronized void addLastViewedWikiFile(@NotNull String wikiFilePath) {
		viewHistory.remove(wikiFilePath);
		viewHistory.add(wikiFilePath);
	}

	/**
	 * Parse a raw wiki text.
	 */
	@NotNull
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
	 * Check if the given file path refers to a wiki page.
	 */
	static boolean isWikiFilePath(@NotNull String repositoryPath) {
		return repositoryPath.endsWith(PAGE_SUFFIX);
	}

	/**
	 * Cut off the file path extension to convert a repository file path to a wiki page path.
	 *
	 * @param repositoryPath Repository file path
	 * @return Wiki page path
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
	 * Append the file path extension of wiki pages to convert a wiki page path to a repository file path.
	 *
	 * @param wikiFilePath Wiki page path
	 * @return Repository file path
	 */
	@NotNull
	static String wikiFilePath2RepositoryPath(@NotNull String wikiFilePath) {
		return wikiFilePath + PAGE_SUFFIX;
	}

	/**
	 * Scan a wiki file for parent relations (<code>{{parent:...}}</code>)
	 * and add them to the parent link list and the child-parent cache.
	 * Also add the child links to other wiki pages.
	 *
	 * @return true if the childParentMap cache was changed.
	 */
	private boolean installParentAndChildLinks(@NotNull WikiFile wikiFile) {
		// extract parent relations from wiki page
		Set<String> parentFilePaths = new HashSet<>();
		PageElementConsumer<Parent, Set<String>> consumer = (parent, context) -> {
			WikiPage wikiPage = WikiHelper.getContextWikiPage(parent, false);
			if (wikiPage != null) {
				String wikiFilePath = WikiHelper.getAbsolutePagePath(parent.getParentPagePath(), wikiPage);
				context.add(wikiFilePath);
			}
		};
		WikiHelper.traversePageElements(wikiFile.getWikiPage(), consumer, Parent.class, parentFilePaths, false);

		// add to parent list
		wikiFile.getParents().clear();
		wikiFile.getParents().addAll(parentFilePaths);

		// add to child-parent cache
		String wikiFilePath = wikiFile.getWikiFilePath();
		boolean cacheModified = !childParentMap.containsKey(wikiFilePath) || !parentFilePaths.containsAll(childParentMap.get(wikiFilePath));
		if (cacheModified) {
			childParentMap.put(wikiFilePath, parentFilePaths);
		}

		// add child links to other wiki pages
		for (String parentFilePath : parentFilePaths) {
			WikiFile parentWikiFile = wikiFileMap.get(parentFilePath);
			if (parentWikiFile != null) {
				parentWikiFile.getChildren().add(wikiFilePath);
			}
		}

		// add child links to this wiki page
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
	 * Remove parent relations from cache and child links from other wiki pages.
	 * Called if a wiki page is deleted or replaced by a newer version.
	 */
	private void uninstallParentAndChildLinks(@NotNull String wikiFilePath) {
		Set<String> parentFilePaths = childParentMap.remove(wikiFilePath);
		if (parentFilePaths == null) {
			return;
		}

		for (String parentFilePath : parentFilePaths) {
			WikiFile parentWikiFile = wikiFileMap.get(parentFilePath);
			if (parentWikiFile != null) {
				parentWikiFile.getChildren().remove(wikiFilePath);
			}
		}
	}
}
