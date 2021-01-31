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
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.TextOnly;
import net.moasdawiki.service.wiki.structure.WikiPage;
import net.moasdawiki.util.DateUtils;
import org.mockito.internal.util.collections.Sets;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static net.moasdawiki.AssertHelper.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class SearchIndexTest {

    private RepositoryService repositoryService;
    private WikiService wikiService;
    private SearchIgnoreList searchIgnoreList;
    private SearchIndex searchIndex;

    @BeforeMethod
    public void setUp() throws Exception {
        repositoryService = mock(RepositoryService.class);
        when(repositoryService.readTextFile(any())).thenReturn("");

        wikiService = mock(WikiService.class);

        searchIgnoreList = mock(SearchIgnoreList.class);
        when(searchIgnoreList.isValidWord(anyString())).thenReturn(true);
        when(searchIgnoreList.isValidWord("ignore1")).thenReturn(false);

        searchIndex = new SearchIndex(mock(Logger.class), repositoryService, wikiService, searchIgnoreList, false);
    }

    @Test
    public void testNormalizeWord() {
        assertEquals(SearchIndex.normalizeWord(""), "");
        assertEquals(SearchIndex.normalizeWord("abc"), "abc");
        assertEquals(SearchIndex.normalizeWord("ABC"), "abc");
        assertEquals(SearchIndex.normalizeWord("äöüÄÖÜ"), "aouaou");
        assertEquals(SearchIndex.normalizeWord("Résumé"), "resume");
    }

    @Test
    public void testSplitStringToWords_Simple() {
        List<String> words = SearchIndex.splitStringToWords("a b c");
        assertEquals(words.size(), 3);
        assertContains(words, "a");
        assertContains(words, "b");
        assertContains(words, "c");
    }

    @Test
    public void testSplitStringToWords_Empty() {
        List<String> words = SearchIndex.splitStringToWords("");
        assertIsEmpty(words);
    }

    @Test
    public void testSplitStringToWords_NoSeparator() {
        List<String> words = SearchIndex.splitStringToWords("a");
        assertEquals(words.size(), 1);
        assertContains(words, "a");
    }

    @Test
    public void testSplitStringToWords_SingleSeparator() {
        List<String> words = SearchIndex.splitStringToWords("abc def ghi 123 jk45 6lmn");
        assertEquals(words.size(), 6);
        assertContains(words, "abc");
        assertContains(words, "def");
        assertContains(words, "ghi");
        assertContains(words, "123");
        assertContains(words, "jk45");
        assertContains(words, "6lmn");
    }

    @Test
    public void testSplitStringToWords_MultiSeparator() {
        List<String> words = SearchIndex.splitStringToWords("/*abc,-+def#'!\"§$%&/()=?ghi`'°^jkl<>| ,.-;:_123*");
        assertEquals(words.size(), 5);
        assertContains(words, "abc");
        assertContains(words, "def");
        assertContains(words, "ghi");
        assertContains(words, "jkl");
        assertContains(words, "123");
    }

    @Test
    public void testSplitStringToWords_Unicode() {
        List<String> words = SearchIndex.splitStringToWords("abc,漢語;äöüÄÖÜß123");
        assertEquals(words.size(), 3);
        assertContains(words, "abc");
        assertContains(words, "漢語");
        assertContains(words, "äöüÄÖÜß123");
    }

    @Test
    public void testAddWordMapping_Single() {
        searchIndex.addWordMapping("a", "/file/path");
        assertEquals(searchIndex.getWord2WikiFilePathMap().keySet().size(), 1);
        Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("a");
        assertEquals(filePaths.size(), 1);
        assertContains(filePaths, "/file/path");
    }

    @Test
    public void testAddWordMapping_Multiple() {
        searchIndex.addWordMapping("a", "/file/path1");
        searchIndex.addWordMapping("a", "/file/path2");
        searchIndex.addWordMapping("b", "/file/path2");
        assertEquals(searchIndex.getWord2WikiFilePathMap().keySet().size(), 2);
        {
            Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("a");
            assertEquals(filePaths.size(), 2);
            assertContains(filePaths, "/file/path1");
            assertContains(filePaths, "/file/path2");
        }
        {
            Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("b");
            assertEquals(filePaths.size(), 1);
            assertContains(filePaths, "/file/path2");
        }
    }

    @Test
    public void testAddWordMapping_LongKey() {
        searchIndex.addWordMapping("abcd", "/file/path1");
        assertEquals(searchIndex.getWord2WikiFilePathMap().keySet().size(), 1);
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "abcd");
    }

    @Test
    public void testAddWordMappings() {
        searchIndex.addNormalizedWordMappings("a ä bc BC def Def ghij Résumé Säure ignore1 IgNore1 12 123 4567", "/file/path");
        assertEquals(searchIndex.getWord2WikiFilePathMap().keySet().size(), 9);
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "a");
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "bc");
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "def");
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "ghij");
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "resume");
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "saure");
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "12");
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "123");
        assertContainsKey(searchIndex.getWord2WikiFilePathMap(), "4567");
        assertContainsKeyNot(searchIndex.getWord2WikiFilePathMap(), "ignore1");
    }

    @Test
    public void testGetWordMapping() {
        searchIndex.addWordMapping("abe", "/file/path1");
        assertEquals(searchIndex.getWordMapping("abe").size(), 1);
        assertEquals(searchIndex.getWordMapping("ABE").size(), 1);
        assertEquals(searchIndex.getWordMapping("Äbé").size(), 1);
        assertIsEmpty(searchIndex.getWordMapping("ab"));
        assertIsEmpty(searchIndex.getWordMapping("abet"));
    }

    @Test
    public void testSearchWikiFilePaths() {
        searchIndex.addWordMapping("word1", "/file/path1");
        searchIndex.addWordMapping("word1", "/file/path2");
        searchIndex.addWordMapping("word2", "/file/path2");
        searchIndex.addWordMapping("word3", "/file/path3");
        {
            Set<String> words = Sets.newSet("word1");
            Set<String> filePaths = searchIndex.searchWikiFilePaths(words);
            assertEquals(filePaths.size(), 2);
            assertContains(filePaths, "/file/path1");
            assertContains(filePaths, "/file/path2");
        }
        {
            Set<String> words = Sets.newSet("word2");
            Set<String> filePaths = searchIndex.searchWikiFilePaths(words);
            assertEquals(filePaths.size(), 1);
            assertContains(filePaths, "/file/path2");
        }
        {
            Set<String> words = Sets.newSet("word3");
            Set<String> filePaths = searchIndex.searchWikiFilePaths(words);
            assertEquals(filePaths.size(), 1);
            assertContains(filePaths, "/file/path3");
        }
        {
            Set<String> words = Sets.newSet("word1", "word2");
            Set<String> filePaths = searchIndex.searchWikiFilePaths(words);
            assertEquals(filePaths.size(), 1);
            assertContains(filePaths, "/file/path2");
        }
        {
            Set<String> words = Sets.newSet("word2", "word3");
            Set<String> filePaths = searchIndex.searchWikiFilePaths(words);
            assertIsEmpty(filePaths);
        }
        {
            Set<String> words = Collections.emptySet();
            Set<String> filePaths = searchIndex.searchWikiFilePaths(words);
            assertIsEmpty(filePaths);
        }
    }

    @Test
    public void testSearchWikiFilePaths_IgnoreList() {
        searchIndex.addWordMapping("word1", "/file/path1");
        searchIndex.addWordMapping("ignore1", "/file/path2");
        {
            Set<String> words = Sets.newSet("ignore1");
            Set<String> filePaths = searchIndex.searchWikiFilePaths(words);
            assertIsEmpty(filePaths);
        }
        {
            Set<String> words = Sets.newSet("ignore1", "word1");
            Set<String> filePaths = searchIndex.searchWikiFilePaths(words);
            assertEquals(filePaths.size(), 1);
            assertContains(filePaths, "/file/path1");
        }
    }

    @Test
    public void testEnsureCacheUpdated_CacheEmpty() throws Exception {
        // call ensureCacheUpdated()
        searchIndex.searchWikiFilePaths(Collections.emptySet());
        // check if readCacheFile() is called
        verify(repositoryService, times(1)).readTextFile(any());
    }

    @Test
    public void testEnsureCacheUpdated_CacheFilled() throws Exception {
        // simulate filled cache
        searchIndex.setLastUpdate(new Date());
        // call ensureCacheUpdated()
        searchIndex.searchWikiFilePaths(Collections.emptySet());
        // check that readCacheFile() is NOT called
        verify(repositoryService, never()).readTextFile(any());
    }

    @Test
    public void testEnsureCacheUpdated_CleanOldEntries() {
        // set repositoryScanAllowed = true
        searchIndex = new SearchIndex(mock(Logger.class), repositoryService, wikiService, searchIgnoreList, true);
        // add "old" content
        searchIndex.addWordMapping("word1", "/file/path1");
        // call ensureCacheUpdated()
        searchIndex.searchWikiFilePaths(Collections.emptySet());
        // check that old content is removed
        assertIsEmpty(searchIndex.getWord2WikiFilePathMap());
    }

    @Test
    public void testEnsureCacheUpdated_UpdateIndex() {
        // set repositoryScanAllowed = true
        searchIndex = new SearchIndex(mock(Logger.class), repositoryService, wikiService, searchIgnoreList, true);
        // call ensureCacheUpdated()
        searchIndex.searchWikiFilePaths(Collections.emptySet());
        // check if updateIndex() is called
        verify(wikiService, times(1)).getModifiedAfter(any());
    }

    @Test
    public void testCleanOldEntries() {
        searchIndex.addWordMapping("a", "/file/path1");
        searchIndex.addWordMapping("a", "/file/path2");
        searchIndex.addWordMapping("b", "/file/path2");
        when(wikiService.existsWikiFile("/file/path1")).thenReturn(true);
        searchIndex.cleanOldEntries();
        {
            Set<String> filePaths = searchIndex.getWordMapping("a");
            assertEquals(filePaths.size(), 1);
            assertContains(filePaths, "/file/path1");
        }
        {
            Set<String> filePaths = searchIndex.getWordMapping("b");
            assertIsEmpty(filePaths);
        }
    }

    @Test
    public void testUpdateIndex() throws Exception {
        // prepare WikiService
        Set<String> modifiedFilePaths = Sets.newSet("/file/path1", "/file/path2");
        when(wikiService.getModifiedAfter(any())).thenReturn(modifiedFilePaths);
        WikiFile wikiFile1 = buildWikiFile("/file/path1", "abcd efgh", new Date(2000L));
        when(wikiService.getWikiFile("/file/path1")).thenReturn(wikiFile1);
        WikiFile wikiFile2 = buildWikiFile("/file/path2", "efgh ijkl", new Date(1000L));
        when(wikiService.getWikiFile("/file/path2")).thenReturn(wikiFile2);
        // run test method
        assertNull(searchIndex.getLastUpdate());
        searchIndex.updateIndex();
        assertEquals(searchIndex.getLastUpdate().getTime(), 2000L);
        {
            Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("abcd");
            assertEquals(filePaths.size(), 1);
            assertContains(filePaths, "/file/path1");
        }
        {
            Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("efgh");
            assertEquals(filePaths.size(), 2);
            assertContains(filePaths, "/file/path1");
            assertContains(filePaths, "/file/path2");
        }
    }

    private WikiFile buildWikiFile(String wikiFilePath, String content, Date contentTimestamp) {
        WikiPage wikiPage = new WikiPage(wikiFilePath, new TextOnly(""), 0, 0);
        AnyFile anyFile = new AnyFile(wikiFilePath + ".txt", contentTimestamp);
        return new WikiFile(wikiFilePath, content, wikiPage, anyFile);
    }

    @Test
    public void testReset() {
        searchIndex.addWordMapping("word1", "/file/path1");
        searchIndex.setLastUpdate(new Date());
        searchIndex.reset();
        assertIsEmpty(searchIndex.getWord2WikiFilePathMap());
        assertNull(searchIndex.getLastUpdate());
    }

    @Test
    public void testReadCacheFile() throws Exception {
        String cacheFileContent = "Version 3\n"
                + "2020-01-30T01:02:03.000Z\n"
                + "word1\t/file/path1\t/file/path2";
        when(repositoryService.readTextFile(any())).thenReturn(cacheFileContent);
        searchIndex.readCacheFile();
        assertEquals(DateUtils.formatUtcDate(searchIndex.getLastUpdate()), "2020-01-30T01:02:03.000Z");
        assertEquals(searchIndex.getWord2WikiFilePathMap().size(), 1);
        Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("word1");
        assertEquals(filePaths.size(), 2);
        assertContains(filePaths, "/file/path1");
        assertContains(filePaths, "/file/path2");
    }

    @Test
    public void testWriteCacheFile() throws Exception {
        searchIndex.addWordMapping("word1", "/file/path1");
        searchIndex.addWordMapping("word1", "/file/path2");
        searchIndex.setLastUpdate(DateUtils.parseUtcDate("2020-01-30T01:02:03.000Z"));
        searchIndex.writeCacheFile();
        String cacheFileContent = "Version 3\n"
                + "2020-01-30T01:02:03.000Z\n"
                + "word1\t/file/path1\t/file/path2\n";
        verify(repositoryService, times(1)).writeTextFile(
                argThat(anyFile -> anyFile.getFilePath().equals(SearchIndex.SEARCH_INDEX_FILEPATH)),
                eq(cacheFileContent));
    }

    /**
     * Convenience method to identify ignore words.
     */
    @Ignore
    @Test
    public void printLongLines() throws Exception {
        File file = new File(".../search-index.cache");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() > 4000) {
                int pos = line.indexOf('\t');
                System.out.println(line.substring(0, pos));
            }
        }
    }
}
