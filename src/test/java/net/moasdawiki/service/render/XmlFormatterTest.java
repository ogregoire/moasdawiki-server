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

public class XmlFormatterTest {

    @Test
    public void testFormatText() {
        {
            String xml = "any text";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "any&nbsp;text");
        }
    }

    @Test
    public void testFormatLineBreak() {
        {
            String xml = "line\n"
                    + "break";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "line<br>\nbreak");
        }
    }

    @Test
    public void testFormatComment() {
        {
            String xml = "<!-- single line comment -->";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-comment\">&lt;!--&nbsp;single&nbsp;line&nbsp;comment&nbsp;--&gt;</span>");
        }
        {
            String xml = "<!-- multi line\n"
                    + " comment -->";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-comment\">&lt;!--&nbsp;multi&nbsp;line</span><br>\n"
                    + "<span class=\"code-xml-comment\">&nbsp;comment&nbsp;--&gt;</span>");
        }
    }

    @Test
    public void testFormatTag() {
        {
            String xml = "<tag>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-special-character\">&lt;</span><span class=\"code-xml-tag\">tag</span><span class=\"code-xml-special-character\">&gt;</span>");
        }
        {
            String xml = "</tag>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-special-character\">&lt;</span><span class=\"code-xml-special-character\">/</span><span class=\"code-xml-tag\">tag</span><span class=\"code-xml-special-character\">&gt;</span>");
        }
    }

    @Test
    public void testFormatAttribute() {
        {
            String xml = "<tag attr=value>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-special-character\">&lt;</span><span class=\"code-xml-tag\">tag</span>&nbsp;<span class=\"code-xml-attribute-name\">attr</span><span class=\"code-xml-special-character\">=</span><span class=\"code-xml-attribute-value\">value</span><span class=\"code-xml-special-character\">&gt;</span>");
        }
        {
            String xml = "<tag attr>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-special-character\">&lt;</span><span class=\"code-xml-tag\">tag</span>&nbsp;<span class=\"code-xml-attribute-name\">attr</span><span class=\"code-xml-special-character\">&gt;</span>");
        }
        {
            String xml = "<tag attr1=\"value1\" attr2='value2'>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-special-character\">&lt;</span><span class=\"code-xml-tag\">tag</span>&nbsp;<span class=\"code-xml-attribute-name\">attr1</span><span class=\"code-xml-special-character\">=</span><span class=\"code-xml-attribute-value\">&quot;value1&quot;</span>&nbsp;<span class=\"code-xml-attribute-name\">attr2</span><span class=\"code-xml-special-character\">=</span><span class=\"code-xml-attribute-value\">&apos;value2&apos;</span><span class=\"code-xml-special-character\">&gt;</span>");
        }
    }

    @Test
    public void testFormatEscapedCharacter() {
        {
            String xml = "any&nbsp;text";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "any<span class=\"code-xml-escaped-character\">&amp;nbsp;</span>text");
        }
        {
            String xml = "incomplete& char";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "incomplete<span class=\"code-xml-escaped-character\">&amp;</span>&nbsp;char");
        }
    }

    @Test
    public void testFormatDoctype() {
        {
            String xml = "<!DOCTYPE xml>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-special-character\">&lt;</span><span class=\"code-xml-special-character\">!</span><span class=\"code-xml-tag\">DOCTYPE</span>&nbsp;<span class=\"code-xml-attribute-name\">xml</span><span class=\"code-xml-special-character\">&gt;</span>");
        }
        {
            String xml = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-special-character\">&lt;</span><span class=\"code-xml-special-character\">!</span><span class=\"code-xml-tag\">DOCTYPE</span>&nbsp;<span class=\"code-xml-attribute-name\">HTML</span>&nbsp;<span class=\"code-xml-attribute-name\">PUBLIC</span>&nbsp;<span class=\"code-xml-attribute-value\">&quot;-//W3C//DTD&nbsp;HTML&nbsp;4.01&nbsp;Transitional//EN&quot;</span><span class=\"code-xml-special-character\">&gt;</span>");
        }
        {
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-special-character\">&lt;</span><span class=\"code-xml-special-character\">?</span><span class=\"code-xml-tag\">xml</span>&nbsp;<span class=\"code-xml-attribute-name\">version</span><span class=\"code-xml-special-character\">=</span><span class=\"code-xml-attribute-value\">&quot;1.0&quot;</span>&nbsp;<span class=\"code-xml-attribute-name\">encoding</span><span class=\"code-xml-special-character\">=</span><span class=\"code-xml-attribute-value\">&quot;UTF-8&quot;</span><span class=\"code-xml-special-character\">?</span><span class=\"code-xml-special-character\">&gt;</span>");
        }
    }

    @Test
    public void testFormatCdata() {
        {
            String xml = "<![CDATA[any &nbsp; text]]>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;![CDATA[</span>any&nbsp;&amp;nbsp;&nbsp;text<span class=\"code-xml-tag\">]]&gt;</span>");
        }
        {
            String xml = "<![CDATA[any\n"
                    + "text]]>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;![CDATA[</span>any<br>\n"
                    + "text<span class=\"code-xml-tag\">]]&gt;</span>");
        }
    }
}
