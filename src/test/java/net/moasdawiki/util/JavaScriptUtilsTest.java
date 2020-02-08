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

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@SuppressWarnings("ConstantConditions")
public class JavaScriptUtilsTest {

    @Test
    public void testToArray() {
        String result = JavaScriptUtils.toArray(Arrays.asList("a", "b", "c"));
        assertEquals(result, "[\"a\", \"b\", \"c\"]");
    }

    @Test
    public void testEscapeJavaScriptNormal() {
        String result = JavaScriptUtils.escapeJavaScript("abc\"'\\\b\f\n\r\tdef");
        assertEquals(result, "abc\\\"\\'\\\\\\b\\f\\n\\r\\tdef");
    }

    @Test
    public void testEscapeJavaScriptNull() {
        assertNull(JavaScriptUtils.escapeJavaScript(null));
    }

    @Test
    public void testGenerateJson() {
        assertEquals(JavaScriptUtils.generateJson("text"), "{ 'message': 'text' }");
        assertEquals(JavaScriptUtils.generateJson("text'with\"special}characters"), "{ 'message': 'text\\'with\\\"special}characters' }");

        assertEquals(JavaScriptUtils.generateJson(null, null), "{ }");
        assertEquals(JavaScriptUtils.generateJson(5, null), "{ 'code': 5 }");
        assertEquals(JavaScriptUtils.generateJson(null, "text"), "{ 'message': 'text' }");
        assertEquals(JavaScriptUtils.generateJson(3, "text"), "{ 'code': 3, 'message': 'text' }");
    }
}
