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
        wikiService = new WikiService(new Logger(null), repositoryServiceMock);
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
        wikiService.wikiFileMap.put("/a", mock(WikiFile.class));
        wikiService.viewHistory.add("/a");
        // test method
        wikiService.reset();
        assertEquals(wikiService.childParentMap.size(), 2);
        assertTrue(wikiService.childParentMap.get("/page").isEmpty());
        assertTrue(wikiService.childParentMap.get("/page-with-parent").contains("/parent-page"));
        assertTrue(wikiService.wikiFileMap.isEmpty());
        assertTrue(wikiService.viewHistory.isEmpty());
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
        assertTrue(wikiService.childParentMap.get("/page").isEmpty());
        assertEquals(wikiService.wikiFileMap.size(), 1);
        assertEquals(wikiService.wikiFileMap.get("/page").getWikiFilePath(), "/page");
    }

    @Test
    public void testGetWikiFilePaths() {
        Set<String> filePaths = wikiService.getWikiFilePaths();
        assertEquals(filePaths.size(), 2);
        assertTrue(filePaths.contains("/page"));
        assertTrue(filePaths.contains("/page-with-parent"));
    }

    @Test
    public void testExistsWikiFileInRepository() {
        wikiService.wikiFileMap.clear();
        assertTrue(wikiService.existsWikiFile("/page"));
        assertFalse(wikiService.existsWikiFile("/unknown-page"));
    }

    @Test
    public void testExistsWikiFileInCache() {
        // not in cache -> false
        assertFalse(wikiService.existsWikiFile("/page-in-cache"));
        // in cache -> true
        wikiService.wikiFileMap.put("/page-in-cache", mock(WikiFile.class));
        assertTrue(wikiService.existsWikiFile("/page-in-cache"));
    }

    @Test
    public void testGetWikiFileFromRepository() throws Exception {
        // ensure the page is not in cache
        assertFalse(wikiService.wikiFileMap.containsKey("/page"));
        // test method
        WikiFile wikiFile = wikiService.getWikiFile("/page");
        assertNotNull(wikiFile);
        assertEquals(wikiFile.getWikiFilePath(), "/page");
        assertEquals(wikiFile.getWikiText(), "testcontent");
        assertEquals(wikiFile.getWikiPage().getPagePath(), "/page");
        assertEquals(wikiFile.getRepositoryFile().getFilePath(), "/page.txt");
        // check cache update
        assertTrue(wikiService.wikiFileMap.containsKey("/page"));
    }

    @Test
    public void testGetWikiFileFromCache() throws Exception {
        // Prepare cache
        WikiFile wikiFileMock = mock(WikiFile.class);
        when(wikiFileMock.cloneTyped()).thenReturn(wikiFileMock);
        wikiService.wikiFileMap.put("/page-in-cache", wikiFileMock);
        reset(repositoryServiceMock);
        // test method
        assertNotNull(wikiService.getWikiFile("/page-in-cache"));
        verify(repositoryServiceMock, never()).readTextFile(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteWikiFile() throws Exception {
        // prepare internal caches
        wikiService.wikiFileMap.put("/page-in-cache", mock(WikiFile.class));
        WikiFile parentWikiFile = mock(WikiFile.class);
        Set<String> parentChilds = mock(Set.class);
        when(parentWikiFile.getChildren()).thenReturn(parentChilds);
        wikiService.wikiFileMap.put("/parent-page", parentWikiFile);
        wikiService.viewHistory.add("/page-in-cache");
        wikiService.childParentMap.put("/page-in-cache", Collections.singleton("/parent-page"));
        // test method
        wikiService.deleteWikiFile("/page-in-cache");
        // check cache updates
        assertFalse(wikiService.wikiFileMap.containsKey("/page-in-cache"));
        assertFalse(wikiService.viewHistory.contains("/page-in-cache"));
        assertFalse(wikiService.childParentMap.containsKey("/page-in-cache"));
        verify(repositoryServiceMock, times(1)).deleteFile(any());
        verify(parentChilds).remove("/page-in-cache");
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
    public void testWriteWikiTextNewInCache() throws Exception {
        assertFalse(wikiService.wikiFileMap.containsKey("/new-page"));
        // test method
        WikiFile wikiFile = wikiService.writeWikiText("/new-page", new WikiText("testcontent"));
        assertEquals(wikiFile.getWikiFilePath(), "/new-page");
        assertEquals(wikiFile.getWikiText(), "testcontent");
        assertNotNull(wikiFile.getWikiPage());
        assertEquals(wikiFile.getRepositoryFile().getFilePath(), "/new-page.txt");
        // check cache updates
        assertSame(wikiService.wikiFileMap.get("/new-page"), wikiFile);
        verify(repositoryServiceMock, times(1)).writeTextFile(any(), eq("testcontent"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWriteWikiTextWithCacheUpdate() throws Exception {
        // prepare internal caches
        Set<String> parent1Childs = mock(Set.class);
        {
            WikiFile parent1WikiFile = mock(WikiFile.class);
            when(parent1WikiFile.getChildren()).thenReturn(parent1Childs);
            wikiService.wikiFileMap.put("/parent-page1", parent1WikiFile);
        }
        Set<String> parent2Childs = mock(Set.class);
        {
            WikiFile parent2WikiFile = mock(WikiFile.class);
            when(parent2WikiFile.getChildren()).thenReturn(parent2Childs);
            wikiService.wikiFileMap.put("/parent-page2", parent2WikiFile);
        }
        wikiService.childParentMap.put("/page", Collections.singleton("/parent-page1"));
        // test method
        wikiService.writeWikiText("/page", new WikiText("{{parent:/parent-page2}}"));
        // check cache updates
        Set<String> parents = wikiService.wikiFileMap.get("/page").getParents();
        assertEquals(parents.size(), 1);
        assertTrue(parents.contains("/parent-page2"));
        verify(parent1Childs).remove("/page");
        verify(parent2Childs).add("/page");
    }

    @Test
    public void testWriteWikiTextWithPosition() throws Exception {
        // prepare internal cache
        WikiFile wikiFile = mock(WikiFile.class);
        when(wikiFile.getWikiText()).thenReturn("testcontent");
        wikiService.wikiFileMap.put("/page", wikiFile);
        // test method
        WikiFile newWikiFile = wikiService.writeWikiText("/page", new WikiText("-subcon-", 4, 7));
        assertEquals(newWikiFile.getWikiText(), "test-subcon-tent");
    }

    @Test
    public void testGetModifiedAfterEmpty() {
        when(repositoryServiceMock.getModifiedAfter(any())).thenReturn(Collections.emptySet());
        assertTrue(wikiService.getModifiedAfter(null).isEmpty());
    }

    @Test
    public void testGetModifiedAfterNullParam() {
        Set<AnyFile> files = new HashSet<>(Arrays.asList(new AnyFile("/a"),
                new AnyFile("/b.txt"), new AnyFile("/c.bin"), new AnyFile("/path/d.txt")));
        when(repositoryServiceMock.getModifiedAfter(any())).thenReturn(files);
        Set<String> paths = wikiService.getModifiedAfter(null);
        assertEquals(paths.size(), 2);
        assertTrue(paths.contains("/b"));
        assertTrue(paths.contains("/path/d"));
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
        assertTrue(paths.contains("/a"));
        assertTrue(paths.contains("/path/b"));
    }

    @Test
    public void testGetLastModifiedOne() {
        List<AnyFile> files = Arrays.asList(new AnyFile("/a.txt"), new AnyFile("/path/b.txt"));
        when(repositoryServiceMock.getLastModifiedFiles(anyInt(), any())).thenReturn(files);
        List<String> paths = wikiService.getLastModified(1);
        assertEquals(paths.size(), 1);
        assertTrue(paths.contains("/a"));
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
        assertTrue(paths.contains("/b"));
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
