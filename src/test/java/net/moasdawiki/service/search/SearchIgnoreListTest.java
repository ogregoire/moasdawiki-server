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

import net.moasdawiki.base.Logger;
import net.moasdawiki.service.repository.RepositoryService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SearchIgnoreListTest {

    private RepositoryService repositoryService;
    private SearchIgnoreList searchIgnoreList;

    @BeforeMethod
    public void setUp() throws Exception {
        String[] ignoreListWords = { "ab", "abc", "cd" };
        repositoryService = mock(RepositoryService.class);
        when(repositoryService.readTextFile(any())).thenReturn(String.join("\n", ignoreListWords));
        searchIgnoreList = new SearchIgnoreList(mock(Logger.class), repositoryService);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testReset() throws Exception {
        // load word list
        searchIgnoreList.isValidWord("abc");
        verify(repositoryService, times(1))
                .readTextFile(argThat(anyFile -> anyFile.getFilePath().equals(SearchIgnoreList.SEARCH_IGNORE_LIST_FILEPATH)));
        // reset list
        searchIgnoreList.reset();
        searchIgnoreList.isValidWord("abc");
        // verify second call
        verify(repositoryService, times(2))
                .readTextFile(argThat(anyFile -> anyFile.getFilePath().equals(SearchIgnoreList.SEARCH_IGNORE_LIST_FILEPATH)));
    }

    @Test
    public void testIsNotIgnoredWord_List() {
        assertTrue(searchIgnoreList.isValidWord("hello"));
        assertFalse(searchIgnoreList.isValidWord("ab"));
        assertFalse(searchIgnoreList.isValidWord("abc"));
        assertFalse(searchIgnoreList.isValidWord("cd"));
    }

    @Test
    public void testIsNotIgnoredWord_Length() {
        assertFalse(searchIgnoreList.isValidWord(""));
        assertFalse(searchIgnoreList.isValidWord("a"));
        assertFalse(searchIgnoreList.isValidWord("1"));
    }
}
