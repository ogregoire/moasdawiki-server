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
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;tag&gt;</span>");
        }
        {
            String xml = "</tag>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;/tag&gt;</span>");
        }
    }

    @Test
    public void testFormatAttribute() {
        {
            String xml = "<tag attr=value>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;tag</span>&nbsp;<span class=\"code-xml-attribute-name\">attr</span>=<span class=\"code-xml-attribute-value\">value</span><span class=\"code-xml-tag\">&gt;</span>");
        }
        {
            String xml = "<tag attr>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;tag</span>&nbsp;<span class=\"code-xml-attribute-name\">attr</span><span class=\"code-xml-tag\">&gt;</span>");
        }
        {
            String xml = "<tag attr1=\"value1\" attr2='value2'>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;tag</span>&nbsp;<span class=\"code-xml-attribute-name\">attr1</span>=<span class=\"code-xml-attribute-value\">&quot;value1&quot;</span>&nbsp;<span class=\"code-xml-attribute-name\">attr2</span>=<span class=\"code-xml-attribute-value\">&apos;value2&apos;</span><span class=\"code-xml-tag\">&gt;</span>");
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
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;!DOCTYPE&nbsp;xml&gt;</span>");
        }
        {
            String xml = "<!DOCTYPE\n"
                    + "xml>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;!DOCTYPE</span><br>\n"
                    + "<span class=\"code-xml-tag\">xml&gt;</span>");
        }
        {
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
            String result = new XmlFormatter(xml).format();
            assertEquals(result, "<span class=\"code-xml-tag\">&lt;?xml</span>&nbsp;<span class=\"code-xml-attribute-name\">version</span>=<span class=\"code-xml-attribute-value\">&quot;1.0&quot;</span>&nbsp;<span class=\"code-xml-attribute-name\">encoding</span>=<span class=\"code-xml-attribute-value\">&quot;UTF-8&quot;</span><span class=\"code-xml-tag\">?&gt;</span>");
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
