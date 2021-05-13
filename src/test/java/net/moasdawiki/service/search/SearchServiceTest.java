/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.service.search;

import org.testng.annotations.Test;

import java.util.Set;

import static net.moasdawiki.AssertHelper.assertContains;
import static org.testng.Assert.assertEquals;

public class SearchServiceTest {

    @Test
    public void testParseQueryString() {
        {
            Set<String> words = SearchService.parseQueryString("word");
            assertEquals(words.size(), 1);
            assertContains(words, "word");
        }
        {
            // ignore special characters
            Set<String> words = SearchService.parseQueryString(",.(\"word\") -=");
            assertEquals(words.size(), 1);
            assertContains(words, "word");
        }
        {
            Set<String> words = SearchService.parseQueryString("word1 word2");
            assertEquals(words.size(), 2);
            assertContains(words, "word1");
            assertContains(words, "word2");
        }
    }

    @Test
    public void testExpandUmlaute() {
        assertEquals(SearchService.expandUmlaute("content"), "content");
        assertEquals(SearchService.expandUmlaute("textäöüßwithaeoeuessumlaute"), "text(ä|ae)(ö|oe)(ü|ue)(ß|ss)with(ä|ae)(ö|oe)(ü|ue)(ß|ss)umlaute");
    }
}
