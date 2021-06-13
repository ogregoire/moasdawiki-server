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

package net.moasdawiki.service.render;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PropertiesFormatterTest {

    @Test
    public void testCommentHash() {
        String propertiesText = "# comment";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-comment\">#&nbsp;comment</span>");
    }

    @Test
    public void testCommentLinebreak() {
        String propertiesText = "# comment\n";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-comment\">#&nbsp;comment</span><br>\n");
    }

    @Test
    public void testCommentSemicolon() {
        String propertiesText = "; comment";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-comment\">;&nbsp;comment</span>");
    }

    @Test
    public void testCommentSlash() {
        String propertiesText = "// comment";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-comment\">//&nbsp;comment</span>");
    }

    @Test
    public void testKey() {
        String propertiesText = "key";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-key\">key</span>");
    }

    @Test
    public void testKeyValueEquals() {
        String propertiesText = "key=value";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-key\">key</span><span class=\"code-properties-delimiter\">=</span><span class=\"code-properties-value\">value</span>");
    }

    @Test
    public void testKeyValueColon() {
        String propertiesText = "key:value";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-key\">key</span><span class=\"code-properties-delimiter\">:</span><span class=\"code-properties-value\">value</span>");
    }

    @Test
    public void testKeyValueSpace() {
        String propertiesText = "key  :  value";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-key\">key</span>&nbsp;&nbsp;<span class=\"code-properties-delimiter\">:</span>&nbsp;&nbsp;<span class=\"code-properties-value\">value</span>");
    }

    @Test
    public void testKeyValueMulti() {
        String propertiesText = "key1 = value1\n"
                + "key2 = value2";
        String result = new PropertiesFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-properties-key\">key1</span>&nbsp;<span class=\"code-properties-delimiter\">=</span>&nbsp;<span class=\"code-properties-value\">value1</span><br>\n"
                + "<span class=\"code-properties-key\">key2</span>&nbsp;<span class=\"code-properties-delimiter\">=</span>&nbsp;<span class=\"code-properties-value\">value2</span>");
    }
}
