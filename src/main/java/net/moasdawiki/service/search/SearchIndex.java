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
 * As the index only stores the first 3 characters of a word,
 * it can only find matching <em>candidates</em>.
 *
 * Limitations:
 * <ul>
 *     <li>Supports only searching for the beginning of a word (3 characters).</li>
 *     <li>No support for regular expressions.</li>
 *     <li>No support for non-letter or non-digit characters.</li>
 * </ul>
 */
public class SearchIndex {

    private static final String SEARCH_INDEX_FILEPATH = "/search-index.cache";

    @NotNull
    private final Logger logger;

    @NotNull
    private final RepositoryService repositoryService;

    @NotNull
    private final WikiService wikiService;

    /**
     * Map: Word prefix -> Set of wiki file paths.
     *
     * Word prefix: 1-3 characters
     */
    @NotNull
    private final Map<String, Set<String>> word2WikiFilePathMap = new HashMap<>();

    @Nullable
    private Date lastUpdate; // null -> unknown

    /**
     * Constructor.
     */
    public SearchIndex(@NotNull Logger logger, @NotNull RepositoryService repositoryService, @NotNull WikiService wikiService) {
        this.logger = logger;
        this.repositoryService = repositoryService;
        this.wikiService = wikiService;
    }

    /**
     * Reads the search index from a cache file.
     */
    public boolean readCacheFile() {
        try {
            // Read cache file content
            AnyFile searchIndexCacheFile = new AnyFile(SEARCH_INDEX_FILEPATH);
            String cacheContent = repositoryService.readTextFile(searchIndexCacheFile);

            try (BufferedReader reader = new BufferedReader(new StringReader(cacheContent))) {
                // Read timestamp in first line
                String timestampStr = reader.readLine();
                Date cacheFileTimestamp = DateUtils.parseUtcDate(timestampStr);

                // Parse search index from cache file
                Map<String, Set<String>> parsedMap = StringUtils.parseMap(reader);

                word2WikiFilePathMap.putAll(parsedMap);
                lastUpdate = cacheFileTimestamp;
                logger.write(parsedMap.size() + " keys read from search index cache file");
            }
            return true;
        } catch (ServiceException | IOException e) {
            logger.write("Error reading search index cache file");
            return false;
        }
    }

    /**
     * Writes the search index into a cache file.
     */
    private void writeCacheFile() {
        // Write timestamp in first line
        StringBuilder sb = new StringBuilder();
        String timestampStr = DateUtils.formatUtcDate(new Date());
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

    /**
     * Updates the search index.
     */
    public void updateIndex() {
        cleanMap();

        Set<String> wikiFilePaths = wikiService.getModifiedAfter(lastUpdate);
        logger.write("Scanning " + wikiFilePaths.size() + " files to rebuild search index");
        for (String wikiFilePath : wikiFilePaths) {
            try {
                WikiFile wikiFile = wikiService.getWikiFile(wikiFilePath);
                addWordMappings(wikiFile.getWikiText(), wikiFile.getWikiFilePath());
                if (lastUpdate == null || lastUpdate.before(wikiFile.getRepositoryFile().getContentTimestamp())) {
                    lastUpdate = wikiFile.getRepositoryFile().getContentTimestamp();
                }
            }
            catch (ServiceException e) {
                // ignore file not found error
            }
        }
        if (!wikiFilePaths.isEmpty()) {
            logger.write("Added " + wikiFilePaths.size() + " wiki pages to search index, contains now " + word2WikiFilePathMap.size() + " words");
            writeCacheFile();
        }
    }

    /**
     * Removes all dangling wiki file references.
     */
    void cleanMap() {
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
     * Returns wiki file paths identified by the search index.
     * These are only candidates for the final search result page.
     *
     * @param words Words that have to match all.
     * @return Wiki file paths that match the given words.
     */
    @Contract("_ -> new")
    @NotNull
    public Set<String> searchWikiFilePathCandidates(@NotNull Set<String> words) {
        Set<String> result = new HashSet<>();
        Iterator<String> it = words.iterator();
        boolean firstIteration = true;
        while (it.hasNext()) {
            Set<String> wikiFilePaths = getWordMapping(it.next());
            if (firstIteration) {
                result.addAll(wikiFilePaths);
                firstIteration = false;
            } else {
                result.retainAll(wikiFilePaths);
            }
        }
        return result;
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
     * Scans the wiki file and adds the word mappings to the search index.
     */
    void addWordMappings(@NotNull String text, @NotNull String wikiFilePath) {
        List<String> words = splitStringToWords(text);
        for (String word : words) {
            // ignore short words
            if (isWordRelevant(word)) {
                addWordMapping(word, wikiFilePath);
            }
        }
    }

    /**
     * Adds a single word mapping to the index.
     */
    void addWordMapping(@NotNull String word, @NotNull String wikiFilePath) {
        String wordPrefix = cutWordPrefixAndNormalize(word);
        Set<String> wikiFilePaths = word2WikiFilePathMap.computeIfAbsent(wordPrefix, k -> new HashSet<>());
        wikiFilePaths.add(wikiFilePath);
    }

    /**
     * Returns the mappings for a single word.
     */
    @Contract(pure = true)
    @NotNull
    Set<String> getWordMapping(@NotNull String word) {
        String wordPrefix = cutWordPrefixAndNormalize(word);
        Set<String> wikiFilePaths = word2WikiFilePathMap.get(wordPrefix);
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
     * Check if the given word is relevant to be added to the search index.
     */
    @Contract(pure = true)
    static boolean isWordRelevant(@NotNull String word) {
        return word.length() >= 4
                || (word.length() == 3 && !Character.isLowerCase(word.codePointAt(1)));
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
     * Returns the first 3 unicode characters and normalizes them
     * This method is used to keep the index smaller as there will be more word collisions.
     */
    @Contract(pure = true)
    @NotNull
    static String cutWordPrefixAndNormalize(@NotNull String str) {
        // cut first 3 characters
        int codePointNum = Math.min(3, str.codePointCount(0, str.length()));
        str = str.substring(0, str.offsetByCodePoints(0, codePointNum));
        // normalize string
        str = StringUtils.unicodeNormalize(str);
        str = str.toLowerCase();
        return normalizeUmlaute(str);
    }

    /**
     * Normalizes German Umlaute: ä -> a, ö -> o, ü -> u.
     */
    @Contract(pure = true)
    @NotNull
    static String normalizeUmlaute(@NotNull String str) {
        StringBuilder sb = new StringBuilder(str.length());
        IntStream codePointStream = str.codePoints();
        PrimitiveIterator.OfInt it = codePointStream.iterator();
        while (it.hasNext()) {
            int codePoint = it.nextInt();
            switch (codePoint) {
                case (int) 'ä':
                case (int) 'Ä':
                    sb.append('a');
                    break;
                case (int) 'ö':
                case (int) 'Ö':
                    sb.append('o');
                    break;
                case (int) 'ü':
                case (int) 'Ü':
                    sb.append('u');
                    break;
                default:
                    sb.appendCodePoint(codePoint);
            }
        }
        return sb.toString();
    }
}
