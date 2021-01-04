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
import org.mockito.internal.util.collections.Sets;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

public class SearchIndexTest {

    private WikiService wikiService;
    private SearchIndex searchIndex;

    @BeforeMethod
    public void setUp() {
        wikiService = mock(WikiService.class);
        searchIndex = new SearchIndex(mock(Logger.class), mock(RepositoryService.class), wikiService);
    }

    @Test
    public void testNormalizeUmlaute() {
        assertEquals(SearchIndex.normalizeUmlaute(""), "");
        assertEquals(SearchIndex.normalizeUmlaute("abc"), "abc");
        assertEquals(SearchIndex.normalizeUmlaute("äöüÄÖÜ"), "aouaou");
    }

    @Test
    public void testCutWordPrefixAndNormalize() {
        // test length
        assertEquals(SearchIndex.cutWordPrefixAndNormalize(""), "");
        assertEquals(SearchIndex.cutWordPrefixAndNormalize("a"), "a");
        assertEquals(SearchIndex.cutWordPrefixAndNormalize("abcd"), "abc");
        assertEquals(SearchIndex.cutWordPrefixAndNormalize("漢語漢語"), "漢語漢");
        // test normalization
        assertEquals(SearchIndex.cutWordPrefixAndNormalize("ABC"), "abc");
        assertEquals(SearchIndex.cutWordPrefixAndNormalize("Résumé"), "res");
        assertEquals(SearchIndex.cutWordPrefixAndNormalize("äöü"), "aou");
    }

    @Test
    public void testSplitStringToWords_Simple() {
        List<String> words = SearchIndex.splitStringToWords("a b c");
        assertEquals(words.size(), 3);
        assertTrue(words.contains("a"));
        assertTrue(words.contains("b"));
        assertTrue(words.contains("c"));
    }

    @Test
    public void testSplitStringToWords_Empty() {
        List<String> words = SearchIndex.splitStringToWords("");
        assertTrue(words.isEmpty());
    }

    @Test
    public void testSplitStringToWords_NoSeparator() {
        List<String> words = SearchIndex.splitStringToWords("a");
        assertEquals(words.size(), 1);
        assertTrue(words.contains("a"));
    }

    @Test
    public void testSplitStringToWords_SingleSeparator() {
        List<String> words = SearchIndex.splitStringToWords("abc def ghi 123 jk45 6lmn");
        assertEquals(words.size(), 6);
        assertTrue(words.contains("abc"));
        assertTrue(words.contains("def"));
        assertTrue(words.contains("ghi"));
        assertTrue(words.contains("123"));
        assertTrue(words.contains("jk45"));
        assertTrue(words.contains("6lmn"));
    }

    @Test
    public void testSplitStringToWords_MultiSeparator() {
        List<String> words = SearchIndex.splitStringToWords("/*abc,-+def#'!\"§$%&/()=?ghi`'°^jkl<>| ,.-;:_123*");
        assertEquals(words.size(), 5);
        assertTrue(words.contains("abc"));
        assertTrue(words.contains("def"));
        assertTrue(words.contains("ghi"));
        assertTrue(words.contains("jkl"));
        assertTrue(words.contains("123"));
    }

    @Test
    public void testSplitStringToWords_Unicode() {
        List<String> words = SearchIndex.splitStringToWords("abc,漢語;äöüÄÖÜß123");
        assertEquals(words.size(), 3);
        assertTrue(words.contains("abc"));
        assertTrue(words.contains("漢語"));
        assertTrue(words.contains("äöüÄÖÜß123"));
    }

    @Test
    public void testIsWordRelevant() {
        assertFalse(SearchIndex.isWordRelevant(""));
        assertFalse(SearchIndex.isWordRelevant("a"));
        assertFalse(SearchIndex.isWordRelevant("abc"));
        assertFalse(SearchIndex.isWordRelevant("A"));
        assertFalse(SearchIndex.isWordRelevant("12"));
        assertTrue(SearchIndex.isWordRelevant("ABC"));
        assertTrue(SearchIndex.isWordRelevant("abcd"));
        assertTrue(SearchIndex.isWordRelevant("123"));
        assertTrue(SearchIndex.isWordRelevant("1234"));
        assertTrue(SearchIndex.isWordRelevant("abc1"));
    }

    @Test
    public void testAddWordMapping_Single() {
        searchIndex.addWordMapping("a", "/file/path");
        assertEquals(searchIndex.getWord2WikiFilePathMap().keySet().size(), 1);
        Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("a");
        assertEquals(filePaths.size(), 1);
        assertTrue(filePaths.contains("/file/path"));
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
            assertTrue(filePaths.contains("/file/path1"));
            assertTrue(filePaths.contains("/file/path2"));
        }
        {
            Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("b");
            assertEquals(filePaths.size(), 1);
            assertTrue(filePaths.contains("/file/path2"));
        }
    }

    @Test
    public void testAddWordMapping_LongKey() {
        searchIndex.addWordMapping("abcd", "/file/path1");
        assertEquals(searchIndex.getWord2WikiFilePathMap().keySet().size(), 1);
        assertTrue(searchIndex.getWord2WikiFilePathMap().containsKey("abc"));
    }

    @Test
    public void testAddWordMapping_Normalization() {
        searchIndex.addWordMapping("Résumé", "/file/path1");
        searchIndex.addWordMapping("Säure", "/file/path2");
        assertEquals(searchIndex.getWord2WikiFilePathMap().keySet().size(), 2);
        assertTrue(searchIndex.getWord2WikiFilePathMap().containsKey("res"));
        assertTrue(searchIndex.getWord2WikiFilePathMap().containsKey("sau"));
    }

    @Test
    public void testAddWordMappings() {
        searchIndex.addWordMappings("a bc def ghij klmno 12 123 4567", "/file/path");
        assertEquals(searchIndex.getWord2WikiFilePathMap().keySet().size(), 4);
        assertFalse(searchIndex.getWord2WikiFilePathMap().containsKey("a"));
        assertFalse(searchIndex.getWord2WikiFilePathMap().containsKey("bc"));
        assertFalse(searchIndex.getWord2WikiFilePathMap().containsKey("def"));
        assertFalse(searchIndex.getWord2WikiFilePathMap().containsKey("12"));
        assertTrue(searchIndex.getWord2WikiFilePathMap().containsKey("ghi"));
        assertTrue(searchIndex.getWord2WikiFilePathMap().containsKey("klm"));
        assertTrue(searchIndex.getWord2WikiFilePathMap().containsKey("123"));
        assertTrue(searchIndex.getWord2WikiFilePathMap().containsKey("456"));
    }

    @Test
    public void testGetWordMapping() {
        searchIndex.addWordMapping("abe", "/file/path1");
        assertFalse(searchIndex.getWordMapping("abet").isEmpty());
        assertFalse(searchIndex.getWordMapping("abe").isEmpty());
        assertFalse(searchIndex.getWordMapping("ABE").isEmpty());
        assertFalse(searchIndex.getWordMapping("Äbé").isEmpty());
    }

    @Test
    public void testSearchFilePathCandidates() {
        searchIndex.addWordMapping("uncle", "/file/path1");
        searchIndex.addWordMapping("uncut", "/file/path2");
        searchIndex.addWordMapping("bob", "/file/path2");
        searchIndex.addWordMapping("bob", "/file/path3");
        searchIndex.addWordMapping("fred", "/file/path4");
        {
            Set<String> words = Sets.newSet("uncle");
            Set<String> filePaths = searchIndex.searchWikiFilePathCandidates(words);
            assertEquals(filePaths.size(), 2);
            assertTrue(filePaths.contains("/file/path1"));
            assertTrue(filePaths.contains("/file/path2"));
        }
        {
            Set<String> words = Sets.newSet("bob");
            Set<String> filePaths = searchIndex.searchWikiFilePathCandidates(words);
            assertEquals(filePaths.size(), 2);
            assertTrue(filePaths.contains("/file/path2"));
            assertTrue(filePaths.contains("/file/path3"));
        }
        {
            Set<String> words = Sets.newSet("uncle", "bob");
            Set<String> filePaths = searchIndex.searchWikiFilePathCandidates(words);
            assertEquals(filePaths.size(), 1);
            assertTrue(filePaths.contains("/file/path2"));
        }
        {
            Set<String> words = Sets.newSet("bob", "fred");
            Set<String> filePaths = searchIndex.searchWikiFilePathCandidates(words);
            assertTrue(filePaths.isEmpty());
        }
    }

    @Test
    public void testCleanMap() {
        searchIndex.addWordMapping("a", "/file/path1");
        searchIndex.addWordMapping("a", "/file/path2");
        searchIndex.addWordMapping("b", "/file/path2");
        when(wikiService.existsWikiFile("/file/path1")).thenReturn(true);
        searchIndex.cleanMap();
        {
            Set<String> filePaths = searchIndex.getWordMapping("a");
            assertEquals(filePaths.size(), 1);
            assertTrue(filePaths.contains("/file/path1"));
        }
        {
            Set<String> filePaths = searchIndex.getWordMapping("b");
            assertTrue(filePaths.isEmpty());
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
            Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("abc");
            assertEquals(filePaths.size(), 1);
            assertTrue(filePaths.contains("/file/path1"));
        }
        {
            Set<String> filePaths = searchIndex.getWord2WikiFilePathMap().get("efg");
            assertEquals(filePaths.size(), 2);
            assertTrue(filePaths.contains("/file/path1"));
            assertTrue(filePaths.contains("/file/path2"));
        }
    }

    private WikiFile buildWikiFile(String wikiFilePath, String content, Date contentTimestamp) {
        WikiPage wikiPage = new WikiPage(wikiFilePath, new TextOnly(""), 0, 0);
        AnyFile anyFile = new AnyFile(wikiFilePath + ".txt", contentTimestamp);
        return new WikiFile(wikiFilePath, content, wikiPage, anyFile);
    }
}
