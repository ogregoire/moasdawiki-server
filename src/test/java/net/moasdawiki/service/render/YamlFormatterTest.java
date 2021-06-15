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

public class YamlFormatterTest {

    @Test
    public void testCommentHash() {
        String propertiesText = "# comment";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-comment\">#&nbsp;comment</span>");
    }

    @Test
    public void testCommentLinebreak() {
        String propertiesText = "# comment\n";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-comment\">#&nbsp;comment</span><br>\n");
    }

    @Test
    public void testDocumentSeparator() {
        String propertiesText = "---";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-document-separator\">---</span>");
    }

    @Test
    public void testKey() {
        String propertiesText = "abc";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-key\">abc</span>");
    }

    @Test
    public void testDashKey() {
        String propertiesText = "- abc";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-special-character\">-</span>&nbsp;<span class=\"code-yaml-key\">abc</span>");
    }

    @Test
    public void testKeyColon() {
        String propertiesText = "abc:";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-key\">abc</span><span class=\"code-yaml-special-character\">:</span>");
    }

    @Test
    public void testKeyValue() {
        String propertiesText = "abc: value";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-key\">abc</span><span class=\"code-yaml-special-character\">:</span>&nbsp;<span class=\"code-yaml-value\">value</span>");
    }

    @Test
    public void testKeyValueMulti() {
        String propertiesText = "key1: value1\n"
                + "key2: value2";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-key\">key1</span><span class=\"code-yaml-special-character\">:</span>&nbsp;<span class=\"code-yaml-value\">value1</span><br>\n"
                + "<span class=\"code-yaml-key\">key2</span><span class=\"code-yaml-special-character\">:</span>&nbsp;<span class=\"code-yaml-value\">value2</span>");
    }

    @Test
    public void testMultilineTextGt() {
        String propertiesText = "abc: >\n"
                + "  line 1\n"
                + "  line 2";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-key\">abc</span><span class=\"code-yaml-special-character\">:</span>&nbsp;"
                + "<span class=\"code-yaml-multiline-text\">&gt;</span><br>\n"
                + "<span class=\"code-yaml-multiline-text\">&nbsp;&nbsp;line&nbsp;1</span><br>\n"
                + "<span class=\"code-yaml-multiline-text\">&nbsp;&nbsp;line&nbsp;2</span>");
    }

    @Test
    public void testMultilineTextPipe() {
        String propertiesText = "abc: |\n"
                + "  line 1\n"
                + "  line 2";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "<span class=\"code-yaml-key\">abc</span><span class=\"code-yaml-special-character\">:</span>&nbsp;"
                + "<span class=\"code-yaml-multiline-text\">|</span><br>\n"
                + "<span class=\"code-yaml-multiline-text\">&nbsp;&nbsp;line&nbsp;1</span><br>\n"
                + "<span class=\"code-yaml-multiline-text\">&nbsp;&nbsp;line&nbsp;2</span>");
    }

    @Test
    public void testMultilineTextUntilKey() {
        String propertiesText = " key1: >\n"
                + "   - a: b\n"
                + "  --- line2\n"
                + " key2";
        String result = new YamlFormatter(propertiesText).format();
        assertEquals(result, "&nbsp;<span class=\"code-yaml-key\">key1</span><span class=\"code-yaml-special-character\">:</span>&nbsp;"
                + "<span class=\"code-yaml-multiline-text\">&gt;</span><br>\n"
                + "<span class=\"code-yaml-multiline-text\">&nbsp;&nbsp;&nbsp;-&nbsp;a:&nbsp;b</span><br>\n"
                + "<span class=\"code-yaml-multiline-text\">&nbsp;&nbsp;---&nbsp;line2</span><br>\n"
                + "&nbsp;<span class=\"code-yaml-key\">key2</span>");
    }
}
