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

package net.moasdawiki.service.search;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.util.DateUtils;
import net.moasdawiki.util.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Search index to speed up full-text search in wiki pages.
 *
 * Limitations:
 * <ul>
 *     <li>No support for regular expressions.</li>
 *     <li>No support for non-letter or non-digit characters.</li>
 * </ul>
 */
public class SearchIndex {

    static final String SEARCH_INDEX_FILEPATH = "/search-index.cache";

    /**
     * File format mark in the first line of the cache file.
     * If the mark doesn't match (e.g. after the implementation has changed)
     * the cache file is ignored for reading.
     */
    private static final String SEARCH_INDEX_FORMAT_MARK = "Version 3";

    @NotNull
    private final Logger logger;

    @NotNull
    private final RepositoryService repositoryService;

    @NotNull
    private final WikiService wikiService;

    @NotNull
    private final SearchIgnoreList searchIgnoreList;

    /**
     * Map: Word -> Set of wiki file paths.
     */
    @NotNull
    private final Map<String, Set<String>> word2WikiFilePathMap;

    /**
     * Last update of the search index in {@link #word2WikiFilePathMap}.
     * null -> cache not loaded yet.
     */
    @Nullable
    private Date lastUpdate;

    /**
     * Is repository scanning allowed to update the cache content?
     * Is set to false for the App as the cache file is updated by synchronization.
     */
    private final boolean repositoryScanAllowed;

    /**
     * Constructor.
     */
    public SearchIndex(@NotNull Logger logger, @NotNull RepositoryService repositoryService,
                       @NotNull WikiService wikiService, @NotNull SearchIgnoreList searchIgnoreList,
                       boolean repositoryScanAllowed) {
        this.logger = logger;
        this.repositoryService = repositoryService;
        this.wikiService = wikiService;
        this.searchIgnoreList = searchIgnoreList;
        this.word2WikiFilePathMap = new HashMap<>();
        this.repositoryScanAllowed = repositoryScanAllowed;
    }

    /**
     * Drop the cache content and reread/rebuild the search index on next access.
     * Is called in App environment after synchronization with server.
     */
    public void reset() {
        word2WikiFilePathMap.clear();
        lastUpdate = null;
    }

    /**
     * Returns wiki file paths identified by the search index.
     *
     * @param words Words that have to match all.
     * @return Wiki file paths that match the given words.
     */
    @Contract("_ -> new")
    @NotNull
    public Set<String> searchWikiFilePaths(@NotNull Set<String> words) {
        ensureCacheUpdated();

        Set<String> result = new HashSet<>();
        boolean firstMatch = true;
        for (String word : words) {
            if (!searchIgnoreList.isValidWord(word)) {
                continue;
            }

            Set<String> wikiFilePaths = getWordMapping(word);
            if (firstMatch) {
                result.addAll(wikiFilePaths);
                firstMatch = false;
            } else {
                result.retainAll(wikiFilePaths);
            }
        }
        logger.write("Found " + result.size() + " wiki pages in search index");
        return result;
    }

    /**
     * Lazy loads and updates the cache content.
     */
    private void ensureCacheUpdated() {
        if (lastUpdate == null) {
            readCacheFile();
        }
        if (repositoryScanAllowed) {
            cleanOldEntries();
            updateIndex();
        }
    }

    /**
     * Removes all dangling wiki file references.
     */
    void cleanOldEntries() {
        Iterator<String> keyIt = word2WikiFilePathMap.keySet().iterator();
        while (keyIt.hasNext()) {
            // remove dangling references
            String key = keyIt.next();
            Set<String> wikiFilePaths = word2WikiFilePathMap.get(key);
            Iterator<String> it = wikiFilePaths.iterator();
            while (it.hasNext()) {
                String wikiFilePath = it.next();
                if (!wikiService.existsWikiFile(wikiFilePath)) {
                    it.remove();
                    logger.write("Removed old wiki page '" + wikiFilePath + "' from search index");
                }
            }

            // remove keys with no reference
            if (wikiFilePaths.isEmpty()) {
                keyIt.remove();
            }
        }
    }

    /**
     * Updates the search index.
     */
    void updateIndex() {
        Set<String> wikiFilePaths = wikiService.getModifiedAfter(lastUpdate);
        logger.write("Scanning " + wikiFilePaths.size() + " files to rebuild search index");
        for (String wikiFilePath : wikiFilePaths) {
            try {
                WikiFile wikiFile = wikiService.getWikiFile(wikiFilePath);
                addNormalizedWordMappings(wikiFile.getWikiText(), wikiFile.getWikiFilePath());
                if (lastUpdate == null || lastUpdate.before(wikiFile.getRepositoryFile().getContentTimestamp())) {
                    lastUpdate = wikiFile.getRepositoryFile().getContentTimestamp();
                }
            }
            catch (ServiceException e) {
                // ignore file not found error
            }
        }
        if (lastUpdate == null) {
            // ensure that lastUpdate is not null when calling writeCacheFile()
            lastUpdate = new Date();
        }
        if (!wikiFilePaths.isEmpty()) {
            logger.write("Added " + wikiFilePaths.size() + " wiki pages to search index, contains now " + word2WikiFilePathMap.size() + " words");
            writeCacheFile();
        }
    }

    /**
     * Scans the wiki file and adds the word mappings to the search index.
     * Normalizes the word.
     */
    void addNormalizedWordMappings(@NotNull String text, @NotNull String wikiFilePath) {
        List<String> words = splitStringToWords(text);
        for (String word : words) {
            String normalizedWord = normalizeWord(word);
            if (searchIgnoreList.isValidWord(normalizedWord)) {
                addWordMapping(normalizedWord, wikiFilePath);
            }
        }
    }

    /**
     * Adds a single word mapping to the index.
     */
    void addWordMapping(@NotNull String word, @NotNull String wikiFilePath) {
        Set<String> wikiFilePaths = word2WikiFilePathMap.computeIfAbsent(word, k -> new HashSet<>());
        wikiFilePaths.add(wikiFilePath);
    }

    /**
     * Returns the mappings for a single word.
     */
    @Contract(pure = true)
    @NotNull
    Set<String> getWordMapping(@NotNull String word) {
        String normalizedWord = normalizeWord(word);
        Set<String> wikiFilePaths = word2WikiFilePathMap.get(normalizedWord);
        if (wikiFilePaths == null) {
            wikiFilePaths = Collections.emptySet();
        }
        return wikiFilePaths;
    }

    /**
     * Returns the map. For testing purpose only.
     */
    @Contract(pure = true)
    @NotNull
    Map<String, Set<String>> getWord2WikiFilePathMap() {
        return word2WikiFilePathMap;
    }

    /**
     * Splits a string into a list of words.
     * Words contain only alphabetic und numeric characters,
     * everything else is seen as separator characters.
     */
    @Contract(pure = true, value = "_ -> new")
    @NotNull
    static List<String> splitStringToWords(@NotNull String text) {
        List<String> result = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        IntStream codePointStream = text.codePoints();
        PrimitiveIterator.OfInt it = codePointStream.iterator();
        while (it.hasNext()) {
            int codePoint = it.nextInt();
            if (Character.isLetterOrDigit(codePoint)) {
                // inside word
                sb.appendCodePoint(codePoint);
            } else if (sb.length() > 0) {
                // separator found -> end of word
                result.add(sb.toString());
                sb.setLength(0);
            }
        }

        // add last word
        if (sb.length() > 0) {
            result.add(sb.toString());
        }

        return result;
    }

    /**
     * Normalizes a word.
     */
    @Contract(pure = true)
    @NotNull
    static String normalizeWord(@NotNull String str) {
        str = StringUtils.unicodeNormalize(str);
        return str.toLowerCase();
    }

    /**
     * For testing purpose only.
     */
    @Contract(pure = true)
    @Nullable
    Date getLastUpdate() {
        return lastUpdate;
    }

    /**
     * For testing purpose only.
     */
    void setLastUpdate(@Nullable Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * Reads the search index from a cache file.
     */
    void readCacheFile() {
        try {
            // Read cache file content
            AnyFile searchIndexCacheFile = new AnyFile(SEARCH_INDEX_FILEPATH);
            String cacheContent = repositoryService.readTextFile(searchIndexCacheFile);

            try (BufferedReader reader = new BufferedReader(new StringReader(cacheContent))) {
                // Read format mark in first line
                String mark = reader.readLine();
                if (!SEARCH_INDEX_FORMAT_MARK.equals(mark)) {
                    logger.write("Search index cache file has wrong format, expected mark '"
                            + SEARCH_INDEX_FORMAT_MARK + "' but has mark '" + mark + "'");
                    return;
                }

                // Read timestamp in second line
                String timestampStr = reader.readLine();
                Date cacheFileTimestamp = DateUtils.parseUtcDate(timestampStr);

                // Parse search index from cache file
                Map<String, Set<String>> parsedMap = StringUtils.parseMap(reader);

                word2WikiFilePathMap.putAll(parsedMap);
                lastUpdate = cacheFileTimestamp;
                logger.write(parsedMap.size() + " keys read from search index cache file");
            }
        } catch (ServiceException | IOException e) {
            logger.write("Error reading search index cache file");
        }
    }

    /**
     * Writes the search index into a cache file.
     */
    void writeCacheFile() {
        StringBuilder sb = new StringBuilder();

        // Write format mark as first line
        sb.append(SEARCH_INDEX_FORMAT_MARK).append('\n');

        // Write timestamp as second line
        String timestampStr = DateUtils.formatUtcDate(lastUpdate);
        sb.append(timestampStr).append('\n');

        // Serialize list
        String mapStr = StringUtils.serializeMap(word2WikiFilePathMap);
        sb.append(mapStr);

        // Write cache file
        AnyFile searchIndexCacheFile = new AnyFile(SEARCH_INDEX_FILEPATH);
        try {
            repositoryService.writeTextFile(searchIndexCacheFile, sb.toString());
        } catch (ServiceException e) {
            // in case of error only log error
            logger.write("Error writing cache file " + searchIndexCacheFile.getFilePath(), e);
        }
    }
}
