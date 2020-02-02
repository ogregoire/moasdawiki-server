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

package net.moasdawiki.util;

import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
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
        assertEquals(PathUtils.resolveDots("/../abc"), "/abc");
        assertEquals(PathUtils.resolveDots("a/../../b/c"), "b/c");
        assertEquals(PathUtils.resolveDots("a/.."), "");
        assertEquals(PathUtils.resolveDots(".."), "");
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
