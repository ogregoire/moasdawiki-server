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

package net.moasdawiki.util;

import net.moasdawiki.base.ServiceException;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

import static net.moasdawiki.AssertHelper.*;
import static org.testng.Assert.*;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
public class StringUtilsTest {

    @Test
    public void testIsNumeric() {
        assertTrue(StringUtils.isNumeric("12345"));
        assertFalse(StringUtils.isNumeric("-12345"));
        assertFalse(StringUtils.isNumeric("123k45"));
        assertFalse(StringUtils.isNumeric(" 123 "));
        assertFalse(StringUtils.isNumeric(""));
        assertFalse(StringUtils.isNumeric(null));
    }

    @Test
    public void testParseInteger() throws Exception {
        assertEquals(StringUtils.parseInteger("12345"), Integer.valueOf(12345));
        assertEquals(StringUtils.parseInteger("-12345"), Integer.valueOf(-12345));
        assertNull(StringUtils.parseInteger(""));
        assertNull(StringUtils.parseInteger(null));
    }

    @Test(expectedExceptions = ServiceException.class)
    public void testParseIntegerError() throws Exception {
        StringUtils.parseInteger("abc");
    }

    @Test
    public void testConcatList() {
        assertEquals(StringUtils.concat(Arrays.asList("a", "b", "c"), "<>"), "a<>b<>c");
        assertEquals(StringUtils.concat(Arrays.asList("a", "b", "c"), ""), "abc");
        assertEquals(StringUtils.concat(Arrays.asList("a", "b", "c"), null), "abc");
        assertEquals(StringUtils.concat(Collections.singletonList("a"), "<>"), "a");
        assertEquals(StringUtils.concat(Collections.emptyList(), "<>"), "");
        assertEquals(StringUtils.concat((List<String>) null, "<>"), "");
    }

    @Test
    public void testConcatArray() {
        assertEquals(StringUtils.concat(new String[]{ "a", "b", "c" }, "<>"), "a<>b<>c");
        assertEquals(StringUtils.concat(new String[]{ "a", "b", "c" }, ""), "abc");
        assertEquals(StringUtils.concat(new String[]{ "a", "b", "c" }, null), "abc");
        assertEquals(StringUtils.concat(new String[]{ "a" }, "<>"), "a");
        assertEquals(StringUtils.concat(new String[0], "<>"), "");
        assertEquals(StringUtils.concat((String[]) null, "<>"), "");
    }

    @Test
    public void testSplitByWhitespace() {
        assertEquals(StringUtils.splitByWhitespace("a b c"), new String[]{ "a", "b", "c" });
        assertEquals(StringUtils.splitByWhitespace(" a b c"), new String[]{ "a", "b", "c" });
        assertEquals(StringUtils.splitByWhitespace("a b c "), new String[]{ "a", "b", "c" });
        assertEquals(StringUtils.splitByWhitespace("a  b  c"), new String[]{ "a", "b", "c" });
        assertEquals(StringUtils.splitByWhitespace("a \t\r\nb"), new String[]{ "a", "b" });
        assertEquals(StringUtils.splitByWhitespace("a"), new String[]{ "a" });
        assertEquals(StringUtils.splitByWhitespace(""), new String[0]);
        assertEquals(StringUtils.splitByWhitespace(null), new String[0]);
    }

    @Test
    public void testNullToEmpty() {
        assertEquals(StringUtils.nullToEmpty("abc"), "abc");
        assertEquals(StringUtils.nullToEmpty(""), "");
        assertEquals(StringUtils.nullToEmpty(null), "");
    }

    @Test
    public void testEmptyToNull() {
        assertEquals(StringUtils.emptyToNull("abc"), "abc");
        assertNull(StringUtils.emptyToNull(""));
        assertNull(StringUtils.emptyToNull(null));
    }

    @Test
    public void testUnicodeNormalize() {
        assertEquals(StringUtils.unicodeNormalize("content"), "content");
        assertEquals(StringUtils.unicodeNormalize("Résumé-Säure"), "Resume-Saure");
    }

    @Test
    public void testSerializeMap() {
        {
            // emtpy map
            assertEquals(StringUtils.serializeMap(Collections.emptyMap()), "");
        }
        {
            // empty values set
            Map<String, Set<String>> map = Collections.singletonMap("a", Collections.emptySet());
            assertEquals(StringUtils.serializeMap(map), "a\n");
        }
        {
            // with values
            Map<String, Set<String>> map = new HashMap<>();
            map.put("b", Collections.singleton("v4"));
            map.put("a", new HashSet<>(Arrays.asList("v3", "v1", "v2")));
            assertEquals(StringUtils.serializeMap(map), "a\tv1\tv2\tv3\nb\tv4\n");
        }
    }

    @Test
    public void testParseMap() throws Exception {
        {
            // empty map
            assertIsEmpty(StringUtils.parseMap(new BufferedReader(new StringReader(""))));
        }
        {
            // empty values set
            Map<String, Set<String>> map = StringUtils.parseMap(new BufferedReader(new StringReader("a\n")));
            assertContainsKey(map, "a");
            assertIsEmpty(map.get("a"));
        }
        {
            // with values
            Map<String, Set<String>> map = StringUtils.parseMap(new BufferedReader(new StringReader("a\tv1\tv2\tv3\nb\tv4\n")));
            assertEquals(map.size(), 2);
            assertEquals(map.get("a").size(), 3);

            assertContains(map.get("a"), "v1");
            assertContains(map.get("a"), "v2");
            assertContains(map.get("a"), "v3");

            assertEquals(map.get("b").size(), 1);
            assertContains(map.get("b"), "v4");
        }
    }
}
