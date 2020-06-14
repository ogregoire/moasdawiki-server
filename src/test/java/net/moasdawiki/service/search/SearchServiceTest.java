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
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.wiki.WikiService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

@SuppressWarnings("FieldCanBeLocal")
public class SearchServiceTest {

    private WikiService wikiService;
    private SearchService searchService;

    @BeforeMethod
    public void setUp() {
        wikiService = mock(WikiService.class);
        searchService = new SearchService(mock(Logger.class), mock(RepositoryService.class), wikiService);
    }

    @Test
    public void testParseQueryString() {
        {
            SearchQuery searchQuery = searchService.parseQueryString("word");
            assertEquals(searchQuery.getQueryString(), "word");
            assertEquals(searchQuery.getIncluded().size(), 1);
            assertTrue(searchQuery.getIncluded().contains("word"));
            assertEquals(searchQuery.getExcluded().size(), 0);
        }
        {
            // ignore special characters
            SearchQuery searchQuery = searchService.parseQueryString(",.(\"word\") -=");
            assertEquals(searchQuery.getIncluded().size(), 1);
            assertTrue(searchQuery.getIncluded().contains("word"));
            assertEquals(searchQuery.getExcluded().size(), 0);
        }
        {
            SearchQuery searchQuery = searchService.parseQueryString("word1 word2");
            assertEquals(searchQuery.getIncluded().size(), 2);
            assertTrue(searchQuery.getIncluded().contains("word1"));
            assertTrue(searchQuery.getIncluded().contains("word2"));
            assertEquals(searchQuery.getExcluded().size(), 0);
        }
        {
            SearchQuery searchQuery = searchService.parseQueryString("word1 -word2 -word3");
            assertEquals(searchQuery.getIncluded().size(), 1);
            assertTrue(searchQuery.getIncluded().contains("word1"));
            assertEquals(searchQuery.getExcluded().size(), 2);
            assertTrue(searchQuery.getExcluded().contains("word2"));
            assertTrue(searchQuery.getExcluded().contains("word3"));
        }
    }

    @Test
    public void testExpandUmlaute() {
        assertEquals(SearchService.expandUmlaute("content"), "content");
        assertEquals(SearchService.expandUmlaute("textäöüßwithaeoeuessumlaute"), "text(ä|ae)(ö|oe)(ü|ue)(ß|ss)with(ä|ae)(ö|oe)(ü|ue)(ß|ss)umlaute");
    }
}
