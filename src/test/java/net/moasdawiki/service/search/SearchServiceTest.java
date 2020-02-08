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
import net.moasdawiki.service.wiki.WikiService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

public class SearchServiceTest {

    private WikiService wikiService;
    private SearchService searchService;

    @BeforeMethod
    public void setUp() {
        Logger logger = mock(Logger.class);
        wikiService = mock(WikiService.class);
        searchService = new SearchService(logger, wikiService);
    }

    @Test
    public void testParseQueryString() {
        {
            SearchQuery searchQuery = searchService.parseQueryString("content");
            assertEquals(searchQuery.getQueryString(), "content");
            assertEquals(searchQuery.getIncluded().size(), 1);
            assertTrue(searchQuery.getIncluded().contains("content"));
            assertEquals(searchQuery.getExcluded().size(), 0);
        }
        {
            // white space at beginning and end
            SearchQuery searchQuery = searchService.parseQueryString(" content ");
            assertEquals(searchQuery.getIncluded().size(), 1);
            assertTrue(searchQuery.getIncluded().contains("content"));
            assertEquals(searchQuery.getExcluded().size(), 0);
        }
        {
            SearchQuery searchQuery = searchService.parseQueryString("text1 text2");
            assertEquals(searchQuery.getIncluded().size(), 2);
            assertTrue(searchQuery.getIncluded().contains("text1"));
            assertTrue(searchQuery.getIncluded().contains("text2"));
            assertEquals(searchQuery.getExcluded().size(), 0);
        }
        {
            SearchQuery searchQuery = searchService.parseQueryString("\"text 1 and 2\"");
            assertEquals(searchQuery.getIncluded().size(), 1);
            assertTrue(searchQuery.getIncluded().contains("text 1 and 2"));
            assertEquals(searchQuery.getExcluded().size(), 0);
        }
        {
            SearchQuery searchQuery = searchService.parseQueryString("text1 -text2 -\"text 3 and 4\" text5 -text6");
            assertEquals(searchQuery.getIncluded().size(), 2);
            assertTrue(searchQuery.getIncluded().contains("text1"));
            assertTrue(searchQuery.getIncluded().contains("text5"));
            assertEquals(searchQuery.getExcluded().size(), 3);
            assertTrue(searchQuery.getExcluded().contains("text2"));
            assertTrue(searchQuery.getExcluded().contains("text 3 and 4"));
            assertTrue(searchQuery.getExcluded().contains("text6"));
        }
    }

    @Test
    public void testEscapeRegEx() {
        assertEquals(SearchService.escapeRegEx("content"), "content");
        assertEquals(SearchService.escapeRegEx("special\\.-[](){}|&$*+?chars"), "special\\\\\\.\\-\\[\\]\\(\\)\\{\\}\\|\\&\\$\\*\\+\\?chars");
    }

    @Test
    public void testExpandUmlaute() {
        assertEquals(SearchService.expandUmlaute("content"), "content");
        assertEquals(SearchService.expandUmlaute("textäöüßwithaeoeuessumlaute"), "text(ä|ae)(ö|oe)(ü|ue)(ß|ss)with(ä|ae)(ö|oe)(ü|ue)(ß|ss)umlaute");
    }

    @Test
    public void testUnicodeNormalize() {
        assertEquals(SearchService.unicodeNormalize("content"), "content");
        assertEquals(SearchService.unicodeNormalize("Résumé-Säure"), "Resume-Saure");
    }
}
