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

import static net.moasdawiki.AssertHelper.assertIsEmpty;
import static org.testng.Assert.*;

public class HtmlWriterTest {

    @Test
    public void testGetTitle() {
        HtmlWriter writer = new HtmlWriter();
        assertNull(writer.getTitle());
        writer.setTitle("aTitle");
        assertEquals(writer.getTitle(), "aTitle");
    }

    @Test
    public void testGetBodyParams() {
        HtmlWriter writer = new HtmlWriter();
        assertNull(writer.getBodyParams());
        writer.setBodyParams("bodyParams");
        assertEquals(writer.getBodyParams(), "bodyParams");
    }

    @Test
    public void testHtmlText() {
        HtmlWriter writer = new HtmlWriter();
        writer.htmlText("html text ");
        writer.htmlText("more html");
        assertEquals(writer.getBodyLines().size(), 1);
        assertEquals(writer.getBodyLines().get(0), "html text more html");
    }

    @Test
    public void testSetContinueInNewLine() {
        HtmlWriter writer = new HtmlWriter();
        writer.htmlText("html text");
        writer.setContinueInNewLine();
        writer.htmlText("more html");
        writer.openTag("tagname");
        writer.setContinueInNewLine();
        writer.htmlText("even more html");
        assertEquals(writer.getBodyLines().size(), 3);
        assertEquals(writer.getBodyLines().get(0), "html text");
        assertEquals(writer.getBodyLines().get(1), "more html<tagname>");
        assertEquals(writer.getBodyLines().get(2), "  even more html");
    }

    @Test
    public void testHtmlNewLine() {
        HtmlWriter writer = new HtmlWriter();
        writer.htmlText("html text");
        writer.htmlNewLine();
        writer.htmlText("more html");
        assertEquals(writer.getBodyLines().size(), 2);
        assertEquals(writer.getBodyLines().get(0), "html text<br>"); // additional "<br>"
        assertEquals(writer.getBodyLines().get(1), "more html");
    }

    @Test
    public void testOpenTag() {
        HtmlWriter writer = new HtmlWriter();
        assertEquals(writer.openTag("tagname1"), 0);
        assertEquals(writer.getBodyLines().get(0), "<tagname1>");
        assertEquals(writer.openTag("tagname2", "param2=value2"), 1);
        assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2 param2=value2>");
    }

    @Test
    public void testOpenDivTag() {
        HtmlWriter writer = new HtmlWriter();
        assertEquals(writer.openDivTag("css-class1"), 0);
        assertEquals(writer.getBodyLines().get(0), "<div class=\"css-class1\">");
        assertEquals(writer.openDivTag("css-class2", "param2=value2"), 1);
        assertEquals(writer.getBodyLines().get(0), "<div class=\"css-class1\"><div class=\"css-class2\" param2=value2>");
    }

    @Test
    public void testOpenSpanTag() {
        HtmlWriter writer = new HtmlWriter();
        assertEquals(writer.openSpanTag("css-class"), 0);
        assertEquals(writer.getBodyLines().get(0), "<span class=\"css-class\">");
    }

    @Test
    public void testOpenFormTag() {
        {
            HtmlWriter writer = new HtmlWriter();
            assertEquals(writer.openFormTag("form-name1"), 0);
            assertEquals(writer.getBodyLines().get(0), "<form method=\"post\" enctype=\"application/x-www-form-urlencoded\" name=\"form-name1\">");
        }
        {
            HtmlWriter writer = new HtmlWriter();
            assertEquals(writer.openFormTag("form-name2", null, HtmlWriter.Method.GET), 0);
            assertEquals(writer.getBodyLines().get(0), "<form method=\"get\" enctype=\"application/x-www-form-urlencoded\" name=\"form-name2\">");
        }
        {
            HtmlWriter writer = new HtmlWriter();
            assertEquals(writer.openFormTag(null, "url", HtmlWriter.Method.POST), 0);
            assertEquals(writer.getBodyLines().get(0), "<form method=\"post\" action=\"url\" enctype=\"application/x-www-form-urlencoded\">");
        }
    }

    @Test
    public void testCloseTag() {
        {
            HtmlWriter writer = new HtmlWriter();
            // close without open tag -> nothing happens
            writer.closeTag();
            assertIsEmpty(writer.getBodyLines());
        }
        {
            HtmlWriter writer = new HtmlWriter();
            writer.openTag("tagname");
            assertEquals(writer.getBodyLines().get(0), "<tagname>");
            writer.closeTag();
            assertEquals(writer.getBodyLines().get(0), "<tagname></tagname>");
        }
        {
            HtmlWriter writer = new HtmlWriter();
            writer.openTag("tagname1");
            writer.openTag("tagname2");
            assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2>");
            writer.closeTag();
            assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2></tagname2>");
            writer.closeTag();
            assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2></tagname2></tagname1>");
        }
    }

    @Test
    public void testCloseTags() {
        HtmlWriter writer = new HtmlWriter();
        writer.openTag("tagname1");
        writer.openTag("tagname2");
        writer.openTag("tagname3");
        assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2><tagname3>");
        writer.closeTags(3);
        assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2><tagname3>");
        writer.closeTags(1);
        assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2><tagname3></tagname3></tagname2>");
    }

    @Test
    public void testCloseAllTags() {
        HtmlWriter writer = new HtmlWriter();
        writer.openTag("tagname1");
        writer.openTag("tagname2");
        assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2>");
        writer.closeAllTags();
        assertEquals(writer.getBodyLines().get(0), "<tagname1><tagname2></tagname2></tagname1>");
    }

    @Test
    public void testGetCurrentTag() {
        HtmlWriter writer = new HtmlWriter();
        assertNull(writer.getCurrentTag());
        writer.openTag("tagname1");
        writer.openTag("tagname2");
        assertEquals(writer.getCurrentTag(), "tagname2");
        assertEquals(writer.getCurrentTag(0), "tagname2");
        assertEquals(writer.getCurrentTag(1), "tagname1");
        assertNull(writer.getCurrentTag(2));
    }

    @Test
    public void testAddHtmlWriter() {
        HtmlWriter writer1 = new HtmlWriter();
        writer1.htmlText("line1");
        writer1.htmlNewLine();
        writer1.htmlText("line2");
        writer1.htmlNewLine();
        writer1.openTag("tag1");

        HtmlWriter writer2 = new HtmlWriter();
        writer2.addHtmlWriter(writer1);
        assertEquals(writer2.getBodyLines().get(0), "line1<br>");
        assertEquals(writer2.getBodyLines().get(1), "line2<br>");
        assertEquals(writer2.getBodyLines().get(2), "<tag1></tag1>");
    }
}
