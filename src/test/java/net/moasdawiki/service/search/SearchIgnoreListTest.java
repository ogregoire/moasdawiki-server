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
import net.moasdawiki.service.repository.RepositoryService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
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
        verify(repositoryService, times(1)).readTextFile(any());
        // reset list
        searchIgnoreList.reset();
        searchIgnoreList.isValidWord("abc");
        verify(repositoryService, times(2)).readTextFile(any()); // second call
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
