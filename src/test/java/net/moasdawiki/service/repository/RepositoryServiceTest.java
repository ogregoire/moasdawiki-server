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

package net.moasdawiki.service.repository;

import net.moasdawiki.FileHelper;
import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static net.moasdawiki.AssertHelper.assertContains;
import static net.moasdawiki.AssertHelper.assertContainsNot;
import static net.moasdawiki.service.repository.RepositoryService.FILELIST_CACHE_FILEPATH;
import static org.testng.Assert.*;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
public class RepositoryServiceTest {

    private static final String CHACHED_REPOSITORY_BASE_PATH = "src/test/resources/repository-with-cache";
    private static final String NO_CHACHE_REPOSITORY_BASE_PATH = "src/test/resources/repository-without-cache";
    private static final String SHADOW_REPOSITORY_BASE_PATH = "src/test/resources/repository-shadow";

    private RepositoryService frs;

    @BeforeMethod
    public void setUp() {
        frs = new RepositoryService(new Logger(null), new File(CHACHED_REPOSITORY_BASE_PATH), null, true);
    }

    @Test
    public void testInitWithoutCacheFile() {
        // Delete cache file if it exists
        //noinspection ResultOfMethodCallIgnored
        new File("src/test/resources/repository-without-cache" + FILELIST_CACHE_FILEPATH).delete();
        // Start service, it will create the cache file automatically
        RepositoryService rsWithoutCache = new RepositoryService(new Logger(null), new File(NO_CHACHE_REPOSITORY_BASE_PATH), null, true);
        assertEquals(rsWithoutCache.getFiles().size(), 2);
        // Check if cache file was generated
        assertNotNull(rsWithoutCache.getFile(FILELIST_CACHE_FILEPATH));
        // Remove cache file
        new File("src/test/resources/repository-without-cache" + FILELIST_CACHE_FILEPATH).delete();
    }

    @Test
    public void testGetFile() {
        assertNotNull(frs.getFile("/file-2020-01-01.txt"));
        assertNull(frs.getFile("/not-existing.txt"));
    }

    @Test
    public void testCacheTimestamp() {
        // 2020-01-20T21:39:58.804Z
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2020, Calendar.JANUARY, 20, 21, 39, 58);
        cal.set(Calendar.MILLISECOND, 804);
        AnyFile file = frs.getFile("/file-2020-01-20.txt");
        assertNotNull(file);
        assertEquals(file.getContentTimestamp().getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testGetFiles() {
        // Number of entries in the cache file
        assertEquals(frs.getFiles().size(), 5);
    }

    @Test
    public void testGetModifiedAfterDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2020, Calendar.JANUARY, 1);
        Set<AnyFile> files = frs.getModifiedAfter(cal.getTime());
        Set<String> filePaths = files.stream().map(AnyFile::getFilePath).collect(Collectors.toSet());
        assertContainsNot(filePaths, "/file-2019-11-01.txt");
        assertContainsNot(filePaths, "/file-2020-01-01.txt");
        assertContains(filePaths, "/file-2020-01-20.txt");
        assertContains(filePaths, "/file-2020-02-01.txt");
    }

    @Test
    public void testGetModifiedAfterNull() {
        Set<AnyFile> files = frs.getModifiedAfter(null);
        assertEquals(files.size(), 5);
    }

    @Test
    public void testGetLastModifiedFilesWithFilter() {
        List<AnyFile> files = frs.getLastModifiedFiles(1, anyFile -> anyFile.getFilePath().contains("2019"));
        assertEquals(files.size(), 1);
        assertEquals(files.get(0).getFilePath(), "/file-2019-11-01.txt");
    }

    @Test
    public void testGetLastModifiedFilesNoFilter() {
        List<AnyFile> files = frs.getLastModifiedFiles(1, anyFile -> true);
        assertEquals(files.size(), 1);
        assertTrue(files.get(0).getFilePath().equals("/file-2020-02-01.txt") ||
                files.get(0).getFilePath().equals("/filelist.cache"));
    }

    @Test
    public void testDeleteFile() throws Exception {
        File file = new File("src/test/resources/repository-with-cache/tmp-file.txt");
        new FileOutputStream(file).close();
        frs.deleteFile(new AnyFile("/tmp-file.txt"));
    }

    @Test(expectedExceptions = ServiceException.class)
    public void testDeleteFileUnknown() throws Exception {
        frs.deleteFile(new AnyFile("/not-existing.txt"));
    }

    @Test
    public void testReadTextFile() throws Exception {
        String content = frs.readTextFile(new AnyFile("/file-2020-01-01.txt"));
        assertEquals(content, "content in repository-with-cache");
    }

    @Test(expectedExceptions = ServiceException.class)
    public void testReadTextFileNotExisting() throws Exception {
        frs.readTextFile(new AnyFile("/not-existing.txt"));
    }

    @Test
    public void testWriteTextFile() throws Exception {
        // Write file
        AnyFile anyFile = frs.writeTextFile(new AnyFile("/tmp-file.txt"), "testcontent");
        assertNotNull(anyFile);
        assertEquals(anyFile.getFilePath(), "/tmp-file.txt");

        // Check written content
        String contentRead = FileHelper.readTextFile("src/test/resources/repository-with-cache/tmp-file.txt");
        assertEquals(contentRead, "testcontent");

        // Restore repository
        frs.deleteFile(anyFile);
    }

    @Test
    public void testReadBinaryFile() throws Exception {
        byte[] contentBytes = frs.readBinaryFile(new AnyFile("/file-2020-01-01.txt"));
        assertEquals(contentBytes, "content in repository-with-cache".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testReadBinaryFileNotInCache() throws Exception {
        // Before readBinaryFile()
        assertNull(frs.getFile("/new-file.bin"));

        // Create and read new file
        new FileWriter("src/test/resources/repository-with-cache/new-file.bin").append("testcontent").close();
        byte[] contentBytes = frs.readBinaryFile(new AnyFile("/new-file.bin"));
        assertEquals(contentBytes, "testcontent".getBytes(StandardCharsets.UTF_8));

        // After readBinaryFile()
        assertNotNull(frs.getFile("/new-file.bin"));

        // Restore repository
        frs.deleteFile(new AnyFile("/new-file.bin"));
    }

    @Test(expectedExceptions = ServiceException.class)
    public void testReadBinaryFileNotExisting() throws Exception {
        frs.readBinaryFile(new AnyFile("/not-existing.txt"));
    }

    @Test
    public void testWriteBinaryFile() throws Exception {
        // Write file
        byte[] content = "testcontent".getBytes(StandardCharsets.UTF_8);
        AnyFile anyFile = frs.writeBinaryFile(new AnyFile("/tmp-file.bin"), content, null);
        assertNotNull(anyFile);
        assertEquals(anyFile.getFilePath(), "/tmp-file.bin");

        // Check written content
        String contentRead = FileHelper.readTextFile("src/test/resources/repository-with-cache/tmp-file.bin");
        assertEquals(contentRead, "testcontent");

        // Restore repository
        frs.deleteFile(anyFile);
    }

    @Test
    public void testCreateFolders() throws Exception {
        File file = new File("folder1/subfolder1/file.txt");
        frs.createFolders(file);
        assertTrue(new File("folder1/subfolder1").exists());
        // remove test folders
        new File("folder1/subfolder1").delete();
        new File("folder1").delete();
    }

    @Test
    public void testRepository2FilesystemPath() {
        assertNull(frs.repository2FilesystemPath(null, false));
        assertEquals(frs.repository2FilesystemPath("", false), new File(CHACHED_REPOSITORY_BASE_PATH).getAbsolutePath() + File.separator);
        assertEquals(frs.repository2FilesystemPath("/", false), new File(CHACHED_REPOSITORY_BASE_PATH).getAbsolutePath() + File.separator);
        assertEquals(frs.repository2FilesystemPath("/a", false), new File(CHACHED_REPOSITORY_BASE_PATH, "a").getAbsolutePath());
        assertEquals(frs.repository2FilesystemPath("/a/b", false), new File(CHACHED_REPOSITORY_BASE_PATH, "a/b").getAbsolutePath());
        assertEquals(frs.repository2FilesystemPath("/forbidden\"%*:<>?\\|characters", false), new File(CHACHED_REPOSITORY_BASE_PATH, "forbidden%0022%0025%002a%003a%003c%003e%003f%005c%007ccharacters").getAbsolutePath());
        assertEquals(frs.repository2FilesystemPath("/a/./b", false), new File(CHACHED_REPOSITORY_BASE_PATH, "a/%002e/b").getAbsolutePath());
        assertEquals(frs.repository2FilesystemPath("/a/../b", false), new File(CHACHED_REPOSITORY_BASE_PATH, "a/%002e%002e/b").getAbsolutePath());
    }

    @Test
    public void testFilesystem2RepositoryPath() {
        //noinspection ConstantConditions
        assertNull(frs.filesystem2RepositoryPath(null));
        assertNull(frs.filesystem2RepositoryPath("/outside-repository"));
        assertEquals(frs.filesystem2RepositoryPath(new File(CHACHED_REPOSITORY_BASE_PATH).getAbsolutePath()), "/");
        assertEquals(frs.filesystem2RepositoryPath(new File(CHACHED_REPOSITORY_BASE_PATH, "a").getAbsolutePath()), "/a");
        assertEquals(frs.filesystem2RepositoryPath(new File(CHACHED_REPOSITORY_BASE_PATH, "a/b").getAbsolutePath()), "/a/b");
        assertEquals(frs.filesystem2RepositoryPath(new File(CHACHED_REPOSITORY_BASE_PATH, "forbidden%0022%0025%002a%003a%003c%003e%003f%005c%007ccharacters").getAbsolutePath()), "/forbidden\"%*:<>?\\|characters");
        assertNull(frs.filesystem2RepositoryPath(new File(CHACHED_REPOSITORY_BASE_PATH, "invalid%klmncharacters").getAbsolutePath()));
    }

    @Test
    public void testShadowRepository() throws Exception {
        // Delete cache file if it exists
        new File("src/test/resources/repository-without-cache" + FILELIST_CACHE_FILEPATH).delete();
        // Start service, it will create the cache file automatically
        RepositoryService rsShadow = new RepositoryService(new Logger(null), new File(NO_CHACHE_REPOSITORY_BASE_PATH),
                new File(SHADOW_REPOSITORY_BASE_PATH), true);
        assertEquals(rsShadow.getFiles().size(), 3);
        assertNotNull(rsShadow.getFile("/file-2020-01-01.txt"));
        assertNotNull(rsShadow.getFile("/file-only-in-shadow.txt"));
        assertEquals(rsShadow.readTextFile(new AnyFile("/file-2020-01-01.txt")), "content in repository-without-cache");
        assertEquals(rsShadow.readTextFile(new AnyFile("/file-only-in-shadow.txt")), "file-only-in-shadow");
        assertEquals(rsShadow.repository2FilesystemPath("/a", false), new File(NO_CHACHE_REPOSITORY_BASE_PATH, "a").getAbsolutePath());
        assertEquals(rsShadow.repository2FilesystemPath("/a", true), new File(SHADOW_REPOSITORY_BASE_PATH, "a").getAbsolutePath());
        // Remove cache file
        new File("src/test/resources/repository-without-cache" + FILELIST_CACHE_FILEPATH).delete();
    }
}
