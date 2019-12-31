package net.moasdawiki.util;

import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class PathUtilsTest {

    @Test
    public void testConvertWebPath2FilePath() {
        String result = PathUtils.convertWebPath2FilePath("/a/path/");
        if (File.separatorChar == '/') {
            assertEquals(result, "/a/path/");
        } else {
            assertEquals(result, "\\a\\path\\");
        }
    }

    @Test
    public void testConvertWebPath2FilePathNull() {
        assertNull(PathUtils.convertWebPath2FilePath(null));
    }

    @Test
    public void testConvertFilePath2WebPath() {
        if (File.separatorChar == '/') {
            String result = PathUtils.convertFilePath2WebPath("/a/path/");
            assertEquals(result, "/a/path/");
        } else {
            String result = PathUtils.convertFilePath2WebPath("\\a\\path\\");
            assertEquals(result, "/a/path/");
        }
    }

    @Test
    public void testConvertFilePath2WebPathNull() {
        assertNull(PathUtils.convertFilePath2WebPath(null));
    }

    @Test
    public void testConvertPath() {
        String result = PathUtils.convertPath("a-bb-c-ddd-e", '-', '=');
        assertEquals(result, "a=bb=c=ddd=e");
    }

    @Test
    public void testConvertPathNull() {
        assertNull(PathUtils.convertPath(null, '-', '='));
    }

    @Test
    public void testConcatWebPaths() {
        assertEquals(PathUtils.concatWebPaths("a", "b"), "a/b");
        assertEquals(PathUtils.concatWebPaths("a/", "b"), "a/b");
        assertEquals(PathUtils.concatWebPaths("a", "/b"), "a/b");
        assertEquals(PathUtils.concatWebPaths("a/", "/b"), "a/b");
        assertEquals(PathUtils.concatWebPaths("", "b"), "b");
        assertEquals(PathUtils.concatWebPaths(null, "b"), "b");
        assertEquals(PathUtils.concatWebPaths("a", ""), "a/");
        assertEquals(PathUtils.concatWebPaths("a", null), "a/");
        assertEquals(PathUtils.concatWebPaths("/", "/"), "/");
        assertEquals(PathUtils.concatWebPaths("", ""), "");
        assertEquals(PathUtils.concatWebPaths(null, null), "");
    }

    @Test
    public void testConcatFilePaths() {
        if (File.separatorChar == '/') {
            assertEquals(PathUtils.concatFilePaths("a", "b"), "a/b");
        } else {
            assertEquals(PathUtils.concatFilePaths("a", "b"), "a\\b");
        }
    }

    @Test
    public void testMakeWebPathAbsolute() {
        assertEquals(PathUtils.makeWebPathAbsolute("abc", "base"), "/base/abc");
        assertEquals(PathUtils.makeWebPathAbsolute("abc", "/base"), "/base/abc");
        assertEquals(PathUtils.makeWebPathAbsolute("/abc", "base"), "/abc");
        assertEquals(PathUtils.makeWebPathAbsolute("", "base"), "/base/");
        assertEquals(PathUtils.makeWebPathAbsolute(null, "base"), "/base/");
        assertEquals(PathUtils.makeWebPathAbsolute("abc", ""), "/abc");
        assertEquals(PathUtils.makeWebPathAbsolute("abc", null), "/abc");
        assertEquals(PathUtils.makeWebPathAbsolute("", ""), "/");
        assertEquals(PathUtils.makeWebPathAbsolute(null, null), "/");
    }

    @Test
    public void testResolveDots() {
        assertEquals(PathUtils.resolveDots("abc"), "abc");
        assertEquals(PathUtils.resolveDots("a/b/c"), "a/b/c");
        assertEquals(PathUtils.resolveDots("a/b/../c/d"), "a/c/d");
        assertEquals(PathUtils.resolveDots("a/b/../../c/d"), "c/d");
        assertEquals(PathUtils.resolveDots("a/b/../c/../d"), "a/d");
        assertEquals(PathUtils.resolveDots("../a/b"), "a/b");
        assertEquals(PathUtils.resolveDots("a/../../b/c"), "b/c");
    }

    @Test
    public void testExtractWebFolder() {
        assertEquals(PathUtils.extractWebFolder("a/b/c"), "a/b/");
        assertEquals(PathUtils.extractWebFolder("a"), "/");
        assertEquals(PathUtils.extractWebFolder("a/"), "a/");
        assertEquals(PathUtils.extractWebFolder("a/b/"), "a/b/");
        assertEquals(PathUtils.extractWebFolder("/a"), "/");
        assertEquals(PathUtils.extractWebFolder("/a/b"), "/a/");
        assertEquals(PathUtils.extractWebFolder(""), "/");
        assertNull(PathUtils.extractWebFolder(null));
    }

    @Test
    public void testExtractWebName() {
        assertEquals(PathUtils.extractWebName("a/b/c"), "c");
        assertEquals(PathUtils.extractWebName("a"), "a");
        assertEquals(PathUtils.extractWebName("a/"), "");
        assertEquals(PathUtils.extractWebName("a/b/"), "");
        assertEquals(PathUtils.extractWebName("/a"), "a");
        assertEquals(PathUtils.extractWebName("/a/b"), "b");
        assertEquals(PathUtils.extractWebName(""), "");
        assertNull(PathUtils.extractWebName(null));
    }
}
