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

package net.moasdawiki.service.wiki.parser;

import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.structure.*;
import org.testng.annotations.Test;

import java.io.StringReader;

import static org.testng.Assert.*;

@SuppressWarnings("ConstantConditions")
public class WikiParserTestInlineElements {

    @Test
    public void testParseCenter() throws Exception {
        String text = "{{center}}abc";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertTrue(pel.get(0) instanceof Paragraph);
        Paragraph paragraph = (Paragraph) pel.get(0);
        assertTrue(paragraph.isCentered());
        assertEquals(WikiHelper.getStringContent(paragraph), "abc");
    }

    @Test
    public void testParseTextOnly() throws Exception {
        String text = "abc";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 1);
        assertTrue(pel.get(0) instanceof Paragraph);
        Paragraph paragraph = (Paragraph) pel.get(0);
        assertFalse(paragraph.isCentered());
        assertTrue(paragraph.getChild() instanceof PageElementList);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertEquals(paragraphChild.size(), 1);
        assertTrue(paragraphChild.get(0) instanceof TextOnly);
        assertEquals(((TextOnly) paragraphChild.get(0)).getText(), "abc");
    }

    @Test
    public void testParseBold() throws Exception {
        String text = "''abc''";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Bold);
        Bold bold = (Bold) paragraphChild.get(0);
        assertEquals(WikiHelper.getStringContent(bold.getChild()), "abc");
    }

    @Test
    public void testParseItalic() throws Exception {
        String text = "##abc##";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Italic);
        Italic italic = (Italic) paragraphChild.get(0);
        assertEquals(WikiHelper.getStringContent(italic.getChild()), "abc");
    }

    @Test
    public void testParseUnderlined() throws Exception {
        String text = "__abc__";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Underlined);
        Underlined underlined = (Underlined) paragraphChild.get(0);
        assertEquals(WikiHelper.getStringContent(underlined.getChild()), "abc");
    }

    @Test
    public void testParseStrikethrough() throws Exception {
        String text = "~~abc~~";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Strikethrough);
        Strikethrough strikethrough = (Strikethrough) paragraphChild.get(0);
        assertEquals(WikiHelper.getStringContent(strikethrough.getChild()), "abc");
    }

    @Test
    public void testParseMonospace() throws Exception {
        String text = "@@abc@@";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Monospace);
        Monospace monospace = (Monospace) paragraphChild.get(0);
        assertEquals(WikiHelper.getStringContent(monospace.getChild()), "abc");
    }

    @Test
    public void testParseSmall() throws Exception {
        String text = "°°abc°°";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Small);
        Small small = (Small) paragraphChild.get(0);
        assertEquals(WikiHelper.getStringContent(small.getChild()), "abc");
    }

    @Test
    public void testParseNowiki() throws Exception {
        String text = "%%abc%%";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Nowiki);
        Nowiki nowiki = (Nowiki) paragraphChild.get(0);
        assertEquals(nowiki.getText(), "abc");
    }

    @Test
    public void testParseLinkPage() throws Exception {
        {
            String text = "[[pagename]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkPage);
            LinkPage linkPage = (LinkPage) paragraphChild.get(0);
            assertEquals(linkPage.getPagePath(), "pagename");
            assertNull(linkPage.getAnchor());
            assertNull(linkPage.getAlternativeText());
        }
        {
            String text = "[[pagename#anchor]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkPage);
            LinkPage linkPage = (LinkPage) paragraphChild.get(0);
            assertEquals(linkPage.getPagePath(), "pagename");
            assertEquals(linkPage.getAnchor(), "anchor");
            assertNull(linkPage.getAlternativeText());
        }
        {
            String text = "[[pagename#anchor | alternative text]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkPage);
            LinkPage linkPage = (LinkPage) paragraphChild.get(0);
            assertEquals(linkPage.getPagePath(), "pagename");
            assertEquals(linkPage.getAnchor(), "anchor");
            assertEquals(WikiHelper.getStringContent(linkPage.getAlternativeText()), "alternative text");
        }
    }

    @Test
    public void testParseLinkExternal() throws Exception {
        {
            String text = "[[https://moasdawiki.net/]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkExternal);
            LinkExternal linkExternal = (LinkExternal) paragraphChild.get(0);
            assertEquals(linkExternal.getUrl(), "https://moasdawiki.net/");
            assertNull(linkExternal.getAlternativeText());
        }
        {
            String text = "[[user@domain.org]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkExternal);
            LinkExternal linkExternal = (LinkExternal) paragraphChild.get(0);
            assertEquals(linkExternal.getUrl(), "mailto:user@domain.org");
            assertNull(linkExternal.getAlternativeText());
        }
        {
            String text = "[[https://moasdawiki.net/ | alternative text]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkExternal);
            LinkExternal linkExternal = (LinkExternal) paragraphChild.get(0);
            assertEquals(linkExternal.getUrl(), "https://moasdawiki.net/");
            assertEquals(WikiHelper.getStringContent(linkExternal.getAlternativeText()), "alternative text");
        }
    }

    @Test
    public void testParseLinkLocalFile() throws Exception {
        {
            String text = "[[file:/document.pdf]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkLocalFile);
            LinkLocalFile linkLocalFile = (LinkLocalFile) paragraphChild.get(0);
            assertEquals(linkLocalFile.getFilePath(), "/document.pdf");
            assertNull(linkLocalFile.getAlternativeText());
        }
        {
            String text = "[[file:/document.pdf | alternative text]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkLocalFile);
            LinkLocalFile linkLocalFile = (LinkLocalFile) paragraphChild.get(0);
            assertEquals(linkLocalFile.getFilePath(), "/document.pdf");
            assertEquals(WikiHelper.getStringContent(linkLocalFile.getAlternativeText()), "alternative text");
        }
    }

    @Test
    public void testParseLinkWiki() throws Exception {
        {
            String text = "[[wiki:newpage]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkWiki);
            LinkWiki linkWiki = (LinkWiki) paragraphChild.get(0);
            assertEquals(linkWiki.getCommand(), "newpage");
            assertNull(linkWiki.getAlternativeText());
        }
        {
            String text = "[[wiki:newpage | alternative text]]";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof LinkWiki);
            LinkWiki linkWiki = (LinkWiki) paragraphChild.get(0);
            assertEquals(linkWiki.getCommand(), "newpage");
            assertEquals(WikiHelper.getStringContent(linkWiki.getAlternativeText()), "alternative text");
        }
    }

    @Test
    public void testParseLineBreak() throws Exception {
        String text = "{{br}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof LineBreak);
    }

    @Test
    public void testParseWikiTagPercentage() throws Exception {
        String text = "{{%%}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof TextOnly);
        assertEquals(((TextOnly) paragraphChild.get(0)).getText(), "%%");
    }

    @Test
    public void testParseImage() throws Exception {
        {
            String text = "{{image:cow.png}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof Image);
            assertEquals(((Image) paragraphChild.get(0)).getUrl(), "cow.png");
            assertEquals(((Image) paragraphChild.get(0)).getOptions().size(), 0);
        }
        {
            String text = "{{image:cow.png | width=50px}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof Image);
            assertEquals(((Image) paragraphChild.get(0)).getUrl(), "cow.png");
            assertEquals(((Image) paragraphChild.get(0)).getOptions().size(), 1);
            assertEquals(((Image) paragraphChild.get(0)).getOptions().get("width"), "50px");
        }
    }

    @Test
    public void testParseHtml() throws Exception {
        String text = "{{html}}html code{{/html}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Html);
        assertEquals(((Html) paragraphChild.get(0)).getText(), "html code");
    }

    @Test
    public void testParseColor() throws Exception {
        String text = "{{color:red}}testcontent{{/color}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Color);
        Color color = (Color) paragraphChild.get(0);
        assertEquals(color.getColorName(), "red");
        assertEquals(WikiHelper.getStringContent(color.getChild()), "testcontent");
    }

    @Test
    public void testParseStyle() throws Exception {
        String text = "{{style:emphasized}}testcontent{{/style}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof Style);
        Style style = (Style) paragraphChild.get(0);
        assertEquals(style.getCssClasses().length, 1);
        assertEquals(style.getCssClasses()[0], "emphasized");
        assertEquals(WikiHelper.getStringContent(style.getChild()), "testcontent");
    }

    @Test
    public void testParseWikiVersion() throws Exception {
        String text = "{{version}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof WikiVersion);
    }

    @Test
    public void testParseDateTime() throws Exception {
        {
            String text = "{{datetime}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof DateTime);
            DateTime dateTime = (DateTime) paragraphChild.get(0);
            assertEquals(dateTime.getFormat(), DateTime.Format.SHOW_DATETIME);
        }
        {
            String text = "{{datetime | date}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof DateTime);
            DateTime dateTime = (DateTime) paragraphChild.get(0);
            assertEquals(dateTime.getFormat(), DateTime.Format.SHOW_DATE);
        }
        {
            String text = "{{datetime | time}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof DateTime);
            DateTime dateTime = (DateTime) paragraphChild.get(0);
            assertEquals(dateTime.getFormat(), DateTime.Format.SHOW_TIME);
        }
    }

    @Test
    public void testParsePageName() throws Exception {
        {
            String text = "{{pagename}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof PageName);
            PageName pageName = (PageName) paragraphChild.get(0);
            assertEquals(pageName.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(pageName.isLinked());
            assertFalse(pageName.isGlobalContext());
        }
        {
            String text = "{{pagename | showPath}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof PageName);
            PageName pageName = (PageName) paragraphChild.get(0);
            assertEquals(pageName.getPageNameFormat(), Listable.PageNameFormat.PAGE_PATH);
            assertFalse(pageName.isLinked());
            assertFalse(pageName.isGlobalContext());
        }
        {
            String text = "{{pagename | showFolder}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof PageName);
            PageName pageName = (PageName) paragraphChild.get(0);
            assertEquals(pageName.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(pageName.isLinked());
            assertFalse(pageName.isGlobalContext());
        }
        {
            String text = "{{pagename | link}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof PageName);
            PageName pageName = (PageName) paragraphChild.get(0);
            assertEquals(pageName.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertTrue(pageName.isLinked());
            assertFalse(pageName.isGlobalContext());
        }
        {
            String text = "{{pagename | globalContext}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof PageName);
            PageName pageName = (PageName) paragraphChild.get(0);
            assertEquals(pageName.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(pageName.isLinked());
            assertTrue(pageName.isGlobalContext());
        }
        {
            String text = "{{pagename | showFolder | globalContext}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof PageName);
            PageName pageName = (PageName) paragraphChild.get(0);
            assertEquals(pageName.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(pageName.isLinked());
            assertTrue(pageName.isGlobalContext());
        }
    }

    @Test
    public void testParsePageTimestamp() throws Exception {
        {
            String text = "{{pagetimestamp}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof PageTimestamp);
            PageTimestamp pageTimestamp = (PageTimestamp) paragraphChild.get(0);
            assertFalse(pageTimestamp.isGlobalContext());
        }
        {
            String text = "{{pagetimestamp | globalContext}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof PageTimestamp);
            PageTimestamp pageTimestamp = (PageTimestamp) paragraphChild.get(0);
            assertTrue(pageTimestamp.isGlobalContext());
        }
    }

    @Test
    public void testParseListViewHistory() throws Exception {
        {
            String text = "{{listviewhistory}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListViewHistory);
            ListViewHistory listViewHistory = (ListViewHistory) paragraphChild.get(0);
            assertEquals(listViewHistory.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listViewHistory.isShowInline());
            assertNull(listViewHistory.getInlineListSeparator());
            assertNull(listViewHistory.getOutputOnEmpty());
            assertEquals(listViewHistory.getMaxLength(), -1);
        }
        {
            String text = "{{listviewhistory | showPath | showinline | separator=\" * \"}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListViewHistory);
            ListViewHistory listViewHistory = (ListViewHistory) paragraphChild.get(0);
            assertEquals(listViewHistory.getPageNameFormat(), Listable.PageNameFormat.PAGE_PATH);
            assertTrue(listViewHistory.isShowInline());
            assertEquals(listViewHistory.getInlineListSeparator(), " * ");
            assertNull(listViewHistory.getOutputOnEmpty());
            assertEquals(listViewHistory.getMaxLength(), -1);
        }
        {
            String text = "{{listviewhistory | showFolder | outputOnEmpty=\"list is empty\" | length=3}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListViewHistory);
            ListViewHistory listViewHistory = (ListViewHistory) paragraphChild.get(0);
            assertEquals(listViewHistory.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(listViewHistory.isShowInline());
            assertNull(listViewHistory.getInlineListSeparator());
            assertEquals(listViewHistory.getOutputOnEmpty(), "list is empty");
            assertEquals(listViewHistory.getMaxLength(), 3);
        }
    }

    @Test
    public void testParseListEditHistory() throws Exception {
        {
            String text = "{{listedithistory}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListEditHistory);
            ListEditHistory listEditHistory = (ListEditHistory) paragraphChild.get(0);
            assertEquals(listEditHistory.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listEditHistory.isShowInline());
            assertNull(listEditHistory.getInlineListSeparator());
            assertNull(listEditHistory.getOutputOnEmpty());
            assertEquals(listEditHistory.getMaxLength(), -1);
        }
        {
            String text = "{{listedithistory | showPath | showinline | separator=\" * \"}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListEditHistory);
            ListEditHistory listEditHistory = (ListEditHistory) paragraphChild.get(0);
            assertEquals(listEditHistory.getPageNameFormat(), Listable.PageNameFormat.PAGE_PATH);
            assertTrue(listEditHistory.isShowInline());
            assertEquals(listEditHistory.getInlineListSeparator(), " * ");
            assertNull(listEditHistory.getOutputOnEmpty());
            assertEquals(listEditHistory.getMaxLength(), -1);
        }
        {
            String text = "{{listedithistory | showFolder | outputOnEmpty=\"list is empty\" | length=3}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListEditHistory);
            ListEditHistory listEditHistory = (ListEditHistory) paragraphChild.get(0);
            assertEquals(listEditHistory.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(listEditHistory.isShowInline());
            assertNull(listEditHistory.getInlineListSeparator());
            assertEquals(listEditHistory.getOutputOnEmpty(), "list is empty");
            assertEquals(listEditHistory.getMaxLength(), 3);
        }
    }

    @Test
    public void testParseListParents() throws Exception {
        {
            String text = "{{listparents}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListParents);
            ListParents listParents = (ListParents) paragraphChild.get(0);
            assertNull(listParents.getPagePath());
            assertEquals(listParents.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listParents.isShowInline());
            assertNull(listParents.getInlineListSeparator());
            assertNull(listParents.getOutputOnEmpty());
            assertFalse(listParents.isGlobalContext());
        }
        {
            String text = "{{listparents:/a}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListParents);
            ListParents listParents = (ListParents) paragraphChild.get(0);
            assertEquals(listParents.getPagePath(), "/a");
            assertEquals(listParents.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listParents.isShowInline());
            assertNull(listParents.getInlineListSeparator());
            assertNull(listParents.getOutputOnEmpty());
            assertFalse(listParents.isGlobalContext());
        }
        {
            String text = "{{listparents | showPath | showinline}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListParents);
            ListParents listParents = (ListParents) paragraphChild.get(0);
            assertNull(listParents.getPagePath());
            assertEquals(listParents.getPageNameFormat(), Listable.PageNameFormat.PAGE_PATH);
            assertTrue(listParents.isShowInline());
            assertNull(listParents.getInlineListSeparator());
            assertNull(listParents.getOutputOnEmpty());
            assertFalse(listParents.isGlobalContext());
        }
        {
            String text = "{{listparents | showFolder | separator=\" * \" | outputOnEmpty=\"empty list\" | globalContext}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListParents);
            ListParents listParents = (ListParents) paragraphChild.get(0);
            assertNull(listParents.getPagePath());
            assertEquals(listParents.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(listParents.isShowInline());
            assertEquals(listParents.getInlineListSeparator(), " * ");
            assertEquals(listParents.getOutputOnEmpty(), "empty list");
            assertTrue(listParents.isGlobalContext());
        }
    }

    @Test
    public void testParseListChildren() throws Exception {
        {
            String text = "{{listchildren}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListChildren);
            ListChildren listChildren = (ListChildren) paragraphChild.get(0);
            assertNull(listChildren.getPagePath());
            assertEquals(listChildren.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listChildren.isShowInline());
            assertNull(listChildren.getInlineListSeparator());
            assertNull(listChildren.getOutputOnEmpty());
            assertFalse(listChildren.isGlobalContext());
        }
        {
            String text = "{{listchildren:/a}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListChildren);
            ListChildren listChildren = (ListChildren) paragraphChild.get(0);
            assertEquals(listChildren.getPagePath(), "/a");
            assertEquals(listChildren.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listChildren.isShowInline());
            assertNull(listChildren.getInlineListSeparator());
            assertNull(listChildren.getOutputOnEmpty());
            assertFalse(listChildren.isGlobalContext());
        }
        {
            String text = "{{listchildren | showPath | showinline}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListChildren);
            ListChildren listChildren = (ListChildren) paragraphChild.get(0);
            assertNull(listChildren.getPagePath());
            assertEquals(listChildren.getPageNameFormat(), Listable.PageNameFormat.PAGE_PATH);
            assertTrue(listChildren.isShowInline());
            assertNull(listChildren.getInlineListSeparator());
            assertNull(listChildren.getOutputOnEmpty());
            assertFalse(listChildren.isGlobalContext());
        }
        {
            String text = "{{listchildren | showFolder | separator=\" * \" | outputOnEmpty=\"empty list\" | globalContext}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListChildren);
            ListChildren listChildren = (ListChildren) paragraphChild.get(0);
            assertNull(listChildren.getPagePath());
            assertEquals(listChildren.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(listChildren.isShowInline());
            assertEquals(listChildren.getInlineListSeparator(), " * ");
            assertEquals(listChildren.getOutputOnEmpty(), "empty list");
            assertTrue(listChildren.isGlobalContext());
        }
    }

    @Test
    public void testParseListPages() throws Exception {
        {
            String text = "{{listpages}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListPages);
            ListPages listPages = (ListPages) paragraphChild.get(0);
            assertNull(listPages.getFolder());
            assertEquals(listPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listPages.isShowInline());
            assertNull(listPages.getInlineListSeparator());
            assertNull(listPages.getOutputOnEmpty());
            assertFalse(listPages.isGlobalContext());
        }
        {
            String text = "{{listpages:/a}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListPages);
            ListPages listPages = (ListPages) paragraphChild.get(0);
            assertEquals(listPages.getFolder(), "/a");
            assertEquals(listPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listPages.isShowInline());
            assertNull(listPages.getInlineListSeparator());
            assertNull(listPages.getOutputOnEmpty());
            assertFalse(listPages.isGlobalContext());
        }
        {
            String text = "{{listpages | showPath | showinline}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListPages);
            ListPages listPages = (ListPages) paragraphChild.get(0);
            assertNull(listPages.getFolder());
            assertEquals(listPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_PATH);
            assertTrue(listPages.isShowInline());
            assertNull(listPages.getInlineListSeparator());
            assertNull(listPages.getOutputOnEmpty());
            assertFalse(listPages.isGlobalContext());
        }
        {
            String text = "{{listpages | showFolder | separator=\" * \" | outputOnEmpty=\"empty list\" | globalContext}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListPages);
            ListPages listPages = (ListPages) paragraphChild.get(0);
            assertNull(listPages.getFolder());
            assertEquals(listPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(listPages.isShowInline());
            assertEquals(listPages.getInlineListSeparator(), " * ");
            assertEquals(listPages.getOutputOnEmpty(), "empty list");
            assertTrue(listPages.isGlobalContext());
        }
    }

    @Test
    public void testParseListWantedPages() throws Exception {
        {
            String text = "{{listwantedpages}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListWantedPages);
            ListWantedPages listWantedPages = (ListWantedPages) paragraphChild.get(0);
            assertEquals(listWantedPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listWantedPages.isShowInline());
            assertNull(listWantedPages.getInlineListSeparator());
            assertNull(listWantedPages.getOutputOnEmpty());
        }
        {
            String text = "{{listwantedpages | showPath | showinline}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListWantedPages);
            ListWantedPages listWantedPages = (ListWantedPages) paragraphChild.get(0);
            assertEquals(listWantedPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_PATH);
            assertTrue(listWantedPages.isShowInline());
            assertNull(listWantedPages.getInlineListSeparator());
            assertNull(listWantedPages.getOutputOnEmpty());
        }
        {
            String text = "{{listwantedpages | showFolder | separator=\" * \" | outputOnEmpty=\"empty list\"}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListWantedPages);
            ListWantedPages listWantedPages = (ListWantedPages) paragraphChild.get(0);
            assertEquals(listWantedPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(listWantedPages.isShowInline());
            assertEquals(listWantedPages.getInlineListSeparator(), " * ");
            assertEquals(listWantedPages.getOutputOnEmpty(), "empty list");
        }
    }

    @Test
    public void testParseListUnlinkedPages() throws Exception {
        {
            String text = "{{listunlinkedpages}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListUnlinkedPages);
            ListUnlinkedPages listUnlinkedPages = (ListUnlinkedPages) paragraphChild.get(0);
            assertFalse(listUnlinkedPages.isHideParents());
            assertFalse(listUnlinkedPages.isHideChildren());
            assertEquals(listUnlinkedPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listUnlinkedPages.isShowInline());
            assertNull(listUnlinkedPages.getInlineListSeparator());
            assertNull(listUnlinkedPages.getOutputOnEmpty());
        }
        {
            String text = "{{listunlinkedpages | hideParents | hideChildren}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListUnlinkedPages);
            ListUnlinkedPages listUnlinkedPages = (ListUnlinkedPages) paragraphChild.get(0);
            assertTrue(listUnlinkedPages.isHideParents());
            assertTrue(listUnlinkedPages.isHideChildren());
            assertEquals(listUnlinkedPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_TITLE);
            assertFalse(listUnlinkedPages.isShowInline());
            assertNull(listUnlinkedPages.getInlineListSeparator());
            assertNull(listUnlinkedPages.getOutputOnEmpty());
        }
        {
            String text = "{{listunlinkedpages | showPath | showinline}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListUnlinkedPages);
            ListUnlinkedPages listUnlinkedPages = (ListUnlinkedPages) paragraphChild.get(0);
            assertFalse(listUnlinkedPages.isHideParents());
            assertFalse(listUnlinkedPages.isHideChildren());
            assertEquals(listUnlinkedPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_PATH);
            assertTrue(listUnlinkedPages.isShowInline());
            assertNull(listUnlinkedPages.getInlineListSeparator());
            assertNull(listUnlinkedPages.getOutputOnEmpty());
        }
        {
            String text = "{{listunlinkedpages | showFolder | separator=\" * \" | outputOnEmpty=\"empty list\"}}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof ListUnlinkedPages);
            ListUnlinkedPages listUnlinkedPages = (ListUnlinkedPages) paragraphChild.get(0);
            assertFalse(listUnlinkedPages.isHideParents());
            assertFalse(listUnlinkedPages.isHideChildren());
            assertEquals(listUnlinkedPages.getPageNameFormat(), Listable.PageNameFormat.PAGE_FOLDER);
            assertFalse(listUnlinkedPages.isShowInline());
            assertEquals(listUnlinkedPages.getInlineListSeparator(), " * ");
            assertEquals(listUnlinkedPages.getOutputOnEmpty(), "empty list");
        }
    }

    @Test
    public void testParseSearchInput() throws Exception {
        String text = "{{search}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof SearchInput);
    }

    @Test
    public void testParseWikiTag() throws Exception {
        String text = "{{tagname:tagvalue | option1=value1}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        Paragraph paragraph = (Paragraph) pel.get(0);
        PageElementList paragraphChild = (PageElementList) paragraph.getChild();
        assertTrue(paragraphChild.get(0) instanceof WikiTag);
        WikiTag wikiTag = (WikiTag) paragraphChild.get(0);
        assertEquals(wikiTag.getTagname(), "tagname");
        assertEquals(wikiTag.getValue(), "tagvalue");
        assertEquals(wikiTag.getOptions().size(), 1);
        assertEquals(wikiTag.getOptions().get("option1"), "value1");
    }

    @Test
    public void testParseXmlTag() throws Exception {
        {
            String text = "<tag>content</tag>";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof XmlTag);
            XmlTag xmlTag = (XmlTag) paragraphChild.get(0);
            assertEquals(xmlTag.getName(), "tag");
            assertNull(xmlTag.getPrefix());
            assertEquals(xmlTag.getOptions().size(), 0);
            assertEquals(WikiHelper.getStringContent(xmlTag.getChild()), "content");
        }
        {
            String text = "<prefix:tag attr1=value1>content</prefix:tag>";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof XmlTag);
            XmlTag xmlTag = (XmlTag) paragraphChild.get(0);
            assertEquals(xmlTag.getName(), "tag");
            assertEquals(xmlTag.getPrefix(), "prefix");
            assertEquals(xmlTag.getOptions().size(), 1);
            assertEquals(xmlTag.getOptions().get("attr1"), "value1");
        }
        {
            // not accepted XML syntax: no letter after "<"
            String text = "< tag>";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof TextOnly);
        }
        {
            // not accepted XML syntax: no letter after "<"
            String text = "<-tag>";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof TextOnly);
        }
        {
            // not accepted XML syntax: missing close tag
            String text = "<tag";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            Paragraph paragraph = (Paragraph) pel.get(0);
            PageElementList paragraphChild = (PageElementList) paragraph.getChild();
            assertTrue(paragraphChild.get(0) instanceof TextOnly);
        }
    }
}
