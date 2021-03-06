/*
 * MoasdaWiki Server
 *
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3 as
 * published by the Free Software Foundation (AGPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.moasdawiki.service.wiki;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import org.mockito.invocation.InvocationOnMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

import static net.moasdawiki.AssertHelper.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings("SameReturnValue")
public class WikiServiceImplTest {

    private static final String[] REPOSITORY_FILE_PATHS = {"/page.txt", "/page-with-parent.txt", "/c.dat"};

    private RepositoryService repositoryServiceMock;
    private WikiService wikiService;

    @BeforeMethod
    public void setUp() throws Exception {
        repositoryServiceMock = mock(RepositoryService.class);
        when(repositoryServiceMock.getFile(anyString())).thenAnswer(this::getFileMock);
        when(repositoryServiceMock.getFiles()).thenAnswer(this::getFilesMock);
        when(repositoryServiceMock.readTextFile(any())).thenAnswer(this::readTextFileMockWithCacheFile);
        when(repositoryServiceMock.writeTextFile(any(), anyString())).thenAnswer(this::writeTextFileMock);
        wikiService = new WikiService(new Logger(null), repositoryServiceMock, true);
    }

    private AnyFile getFileMock(InvocationOnMock invocationOnMock) {
        String filePath = invocationOnMock.getArgument(0);
        if (new HashSet<>(Arrays.asList(REPOSITORY_FILE_PATHS)).contains(filePath)) {
            return new AnyFile(filePath);
        } else {
            return null;
        }
    }

    private Set<AnyFile> getFilesMock(InvocationOnMock invocationOnMock) {
        return Arrays.stream(REPOSITORY_FILE_PATHS).map(AnyFile::new).collect(Collectors.toSet());
    }

    private String readTextFileMockWithCacheFile(InvocationOnMock invocationOnMock) throws Exception {
        AnyFile anyFile = invocationOnMock.getArgument(0, AnyFile.class);
        if ("/parentrelations.cache".equals(anyFile.getFilePath())) {
            return "\n"
                    + "/page\n"
                    + "/page-with-parent\t/parent-page\n";
        } else {
            return readTextFileMockNoCacheFile(invocationOnMock);
        }
    }

    private String readTextFileMockNoCacheFile(InvocationOnMock invocationOnMock) throws Exception {
        AnyFile anyFile = invocationOnMock.getArgument(0, AnyFile.class);
        if ("/page.txt".equals(anyFile.getFilePath())) {
            return "testcontent";
        }
        throw new ServiceException("File not found");
    }

    private AnyFile writeTextFileMock(InvocationOnMock invocationOnMock) {
        return invocationOnMock.getArgument(0, AnyFile.class);
    }

    @Test
    public void testResetWithCacheFile() {
        // prepare cache
        wikiService.viewHistory.add("/a");
        // test method
        wikiService.reset();
        assertEquals(wikiService.childParentMap.size(), 2);
        assertIsEmpty(wikiService.childParentMap.get("/page"));
        assertContains(wikiService.childParentMap.get("/page-with-parent"), "/parent-page");
        assertIsEmpty(wikiService.viewHistory);
    }

    @Test
    public void testResetNoCacheFile() throws Exception {
        reset(repositoryServiceMock);
        when(repositoryServiceMock.getFile(anyString())).thenAnswer(this::getFileMock);
        when(repositoryServiceMock.getFiles()).thenReturn(new HashSet<>(Arrays.asList(new AnyFile("/page.txt"), new AnyFile("c.dat"))));
        when(repositoryServiceMock.readTextFile(any())).thenAnswer(this::readTextFileMockNoCacheFile);
        when(repositoryServiceMock.writeTextFile(any(), anyString())).thenAnswer(this::writeTextFileMock);
        // test method
        wikiService.reset();
        assertEquals(wikiService.childParentMap.size(), 1);
        assertIsEmpty(wikiService.childParentMap.get("/page"));
    }

    @Test
    public void testGetWikiFilePaths() {
        Set<String> filePaths = wikiService.getWikiFilePaths();
        assertEquals(filePaths.size(), 2);
        assertContains(filePaths, "/page");
        assertContains(filePaths, "/page-with-parent");
    }

    @Test
    public void testExistsWikiFile() {
        assertTrue(wikiService.existsWikiFile("/page"));
        assertFalse(wikiService.existsWikiFile("/unknown-page"));
    }

    @Test
    public void testGetWikiFile() throws Exception {
        WikiFile wikiFile = wikiService.getWikiFile("/page");
        assertNotNull(wikiFile);
        assertEquals(wikiFile.getWikiFilePath(), "/page");
        assertEquals(wikiFile.getWikiText(), "testcontent");
        assertEquals(wikiFile.getWikiPage().getPagePath(), "/page");
        assertEquals(wikiFile.getRepositoryFile().getFilePath(), "/page.txt");
    }

    @Test
    public void testDeleteWikiFile() throws Exception {
        wikiService.viewHistory.add("/page-in-cache");
        wikiService.childParentMap.put("/page-in-cache", Collections.singleton("/parent-page"));
        // test method
        wikiService.deleteWikiFile("/page-in-cache");
        // check cache updates
        assertContainsNot(wikiService.viewHistory, "/page-in-cache");
        assertContainsKeyNot(wikiService.childParentMap, "/page-in-cache");
        verify(repositoryServiceMock, times(1)).deleteFile(any());
    }

    @Test
    public void testReadWikiText() throws Exception {
        WikiText wikiText = wikiService.readWikiText("/page", null, null);
        assertEquals(wikiText.getText(), "testcontent");
    }

    @Test
    public void testReadWikiTextWithPosition() throws Exception {
        WikiText wikiText = wikiService.readWikiText("/page", 2, 7);
        assertEquals(wikiText.getText(), "stcon");
    }

    @Test(expectedExceptions = ServiceException.class)
    public void testReadWikiTextWithPositionInvalid() throws Exception {
        wikiService.readWikiText("/page", 2, 100);
    }

    @Test
    public void testWriteWikiText() throws Exception {
        WikiFile wikiFile = wikiService.writeWikiText("/new-page", new WikiText("testcontent"));
        assertEquals(wikiFile.getWikiFilePath(), "/new-page");
        assertEquals(wikiFile.getWikiText(), "testcontent");
        assertNotNull(wikiFile.getWikiPage());
        assertEquals(wikiFile.getRepositoryFile().getFilePath(), "/new-page.txt");
        verify(repositoryServiceMock, times(1)).writeTextFile(any(), eq("testcontent"));
    }

    @Test
    public void testWriteWikiTextWithPosition() throws Exception {
        // prepare internal cache
        WikiFile wikiFile = mock(WikiFile.class);
        when(wikiFile.getWikiText()).thenReturn("testcontent");
        // test method
        WikiFile newWikiFile = wikiService.writeWikiText("/page", new WikiText("-subcon-", 4, 7));
        assertEquals(newWikiFile.getWikiText(), "test-subcon-tent");
    }

    @Test
    public void testGetModifiedAfterEmpty() {
        when(repositoryServiceMock.getModifiedAfter(any())).thenReturn(Collections.emptySet());
        assertIsEmpty(wikiService.getModifiedAfter(null));
    }

    @Test
    public void testGetModifiedAfterNullParam() {
        Set<AnyFile> files = new HashSet<>(Arrays.asList(new AnyFile("/a"),
                new AnyFile("/b.txt"), new AnyFile("/c.bin"), new AnyFile("/path/d.txt")));
        when(repositoryServiceMock.getModifiedAfter(any())).thenReturn(files);
        Set<String> paths = wikiService.getModifiedAfter(null);
        assertEquals(paths.size(), 2);
        assertContains(paths, "/b");
        assertContains(paths, "/path/d");
    }

    @Test
    public void testGetModifiedAfterDateParam() {
        Date date = new Date();
        wikiService.getModifiedAfter(date);
        // check if date param is forwarded to RepositoryService
        verify(repositoryServiceMock, times(1)).getModifiedAfter(eq(date));
    }

    @Test
    public void testGetLastModifiedAll() {
        List<AnyFile> files = Arrays.asList(new AnyFile("/a.txt"), new AnyFile("/path/b.txt"));
        when(repositoryServiceMock.getLastModifiedFiles(anyInt(), any())).thenReturn(files);
        List<String> paths = wikiService.getLastModified(-1);
        assertEquals(paths.size(), 2);
        assertContains(paths, "/a");
        assertContains(paths, "/path/b");
    }

    @Test
    public void testGetLastModifiedOne() {
        List<AnyFile> files = Arrays.asList(new AnyFile("/a.txt"), new AnyFile("/path/b.txt"));
        when(repositoryServiceMock.getLastModifiedFiles(anyInt(), any())).thenReturn(files);
        List<String> paths = wikiService.getLastModified(1);
        assertEquals(paths.size(), 1);
        assertContains(paths, "/a");
    }

    @Test
    public void testGetLastModifiedMany() {
        List<AnyFile> files = Arrays.asList(new AnyFile("/a.txt"), new AnyFile("/path/b.txt"));
        when(repositoryServiceMock.getLastModifiedFiles(anyInt(), any())).thenReturn(files);
        // query more files than available
        List<String> paths = wikiService.getLastModified(100);
        assertEquals(paths.size(), 2);
    }

    @Test
    public void testGetLastViewedWikiFilesAll() {
        // prepare cache
        wikiService.viewHistory.add("/a");
        wikiService.viewHistory.add("/b");
        // test method
        List<String> paths = wikiService.getLastViewedWikiFiles(-1);
        assertEquals(paths.size(), 2);
    }

    @Test
    public void testGetLastViewedWikiFilesOne() {
        // prepare cache
        wikiService.viewHistory.add("/a");
        wikiService.viewHistory.add("/b");
        // test method
        List<String> paths = wikiService.getLastViewedWikiFiles(1);
        assertEquals(paths.size(), 1);
        assertContains(paths, "/b");
    }

    @Test
    public void testGetLastViewedWikiFilesMany() {
        // prepare cache
        wikiService.viewHistory.add("/a");
        wikiService.viewHistory.add("/b");
        // test method
        List<String> paths = wikiService.getLastViewedWikiFiles(100);
        assertEquals(paths.size(), 2);
    }

    @Test
    public void testAddLastViewedWikiFileNew() {
        // prepare cache
        wikiService.viewHistory.add("/a");
        wikiService.viewHistory.add("/b");
        // test method
        wikiService.addLastViewedWikiFile("/c");
        assertEquals(wikiService.viewHistory.size(), 3);
        assertEquals(wikiService.viewHistory.getLast(), "/c");
    }

    @Test
    public void testAddLastViewedWikiFileReplace() {
        // prepare cache
        wikiService.viewHistory.add("/a");
        wikiService.viewHistory.add("/b");
        // test method
        wikiService.addLastViewedWikiFile("/a");
        assertEquals(wikiService.viewHistory.size(), 2);
        assertEquals(wikiService.viewHistory.get(0), "/b");
        assertEquals(wikiService.viewHistory.get(1), "/a");
    }

    @Test
    public void testParseWikiText() throws Exception {
        // just test basic functionality, the parser itself is tested separately
        assertNotNull(wikiService.parseWikiText("testcontent"));
    }

    @Test
    public void testIsWikiFilePath() {
        assertFalse(WikiService.isWikiFilePath("/a"));
        assertTrue(WikiService.isWikiFilePath("/a.txt"));
    }

    @Test
    public void testRepositoryPath2WikiFilePath() {
        assertEquals(WikiService.repositoryPath2WikiFilePath("/a.txt"), "/a");
        assertEquals(WikiService.repositoryPath2WikiFilePath("/a.dat"), "/a.dat");
    }

    @Test
    public void testWikiFilePath2RepositoryPath() {
        assertEquals(WikiService.wikiFilePath2RepositoryPath("/a"), "/a.txt");
    }
}
