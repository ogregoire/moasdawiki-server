/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.service.wiki;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.transform.TransformerHelper;
import net.moasdawiki.service.wiki.structure.*;
import org.mockito.invocation.InvocationOnMock;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class WikiHelperTest {

    @Test
    public void testExtendWikiPage() throws Exception {
        Logger logger = mock(Logger.class);
        Settings settings = mock(Settings.class);
        when(settings.getNavigationPagePath()).thenReturn("/navigation");
        when(settings.getHeaderPagePath()).thenReturn("/header");
        when(settings.getFooterPagePath()).thenReturn("/footer");
        WikiService wikiService = mock(WikiService.class);
        when(wikiService.getWikiFile(anyString())).thenAnswer(this::getWikiFileMock);

        {
            // body only
            WikiPage wikiPage = new WikiPage(null, new TextOnly("main content"), null, null);
            WikiPage newWikiPage = WikiHelper.extendWikiPage(wikiPage, false, false, false,
                    logger, settings, wikiService);
            assertTrue(containsText("main content", newWikiPage));
            assertFalse(containsText("Content of /navigation", newWikiPage));
            assertFalse(containsText("Content of /header", newWikiPage));
            assertFalse(containsText("Content of /footer", newWikiPage));
        }

        {
            // with navigation
            WikiPage wikiPage = new WikiPage(null, new TextOnly("main content"), null, null);
            WikiPage newWikiPage = WikiHelper.extendWikiPage(wikiPage, true, false, false,
                    logger, settings, wikiService);
            assertTrue(containsText("main content", newWikiPage));
            assertTrue(containsText("Content of /navigation", newWikiPage));
            assertFalse(containsText("Content of /header", newWikiPage));
            assertFalse(containsText("Content of /footer", newWikiPage));
        }

        {
            // with header
            WikiPage wikiPage = new WikiPage(null, new TextOnly("main content"), null, null);
            WikiPage newWikiPage = WikiHelper.extendWikiPage(wikiPage, false, true, false,
                    logger, settings, wikiService);
            assertTrue(containsText("main content", newWikiPage));
            assertFalse(containsText("Content of /navigation", newWikiPage));
            assertTrue(containsText("Content of /header", newWikiPage));
            assertFalse(containsText("Content of /footer", newWikiPage));
        }

        {
            // with footer
            WikiPage wikiPage = new WikiPage(null, new TextOnly("main content"), null, null);
            WikiPage newWikiPage = WikiHelper.extendWikiPage(wikiPage, false, false, true,
                    logger, settings, wikiService);
            assertTrue(containsText("main content", newWikiPage));
            assertFalse(containsText("Content of /navigation", newWikiPage));
            assertFalse(containsText("Content of /header", newWikiPage));
            assertTrue(containsText("Content of /footer", newWikiPage));
        }
    }

    private WikiFile getWikiFileMock(InvocationOnMock invocationOnMock) throws ServiceException {
        String wikiFilePath = invocationOnMock.getArgument(0, String.class);
        switch (wikiFilePath) {
            case "/navigation":
            case "/header":
            case "/footer":
                WikiPage wikiPage = new WikiPage(wikiFilePath, new TextOnly("Content of " + wikiFilePath), null, null);
                return new WikiFile(wikiFilePath, "testcontent", wikiPage, new AnyFile(wikiFilePath + ".txt"));
            default:
                throw new ServiceException("File not found");
        }
    }

    private boolean containsText(final String text, WikiPage wikiPage) {
        AtomicInteger findCount = new AtomicInteger(0);
        PageElementConsumer<TextOnly, AtomicInteger> consumer = (textOnly, context) -> {
            if (textOnly.getText().contains(text)) {
                context.incrementAndGet();
            }
        };
        WikiHelper.traversePageElements(wikiPage, consumer, TextOnly.class, findCount, false);
        return findCount.get() >= 1;
    }

    @Test
    public void testTraversePageElements() {
        PageElementList pel = new PageElementList();
        pel.add(new TextOnly("a"));
        pel.add(new Html("b"));
        pel.add(new Bold(new TextOnly("c"), null, null));
        Table table = new Table(null, null, null);
        TableRow row = new TableRow(null);
        row.addCell(new TableCell(new TextOnly("d"), false, null));
        table.addRow(row);
        pel.add(table);

        StringBuilder sb = new StringBuilder();
        WikiHelper.traversePageElements(pel, (textOnly, context) -> context.append(textOnly.getText()), TextOnly.class, sb, false);
        assertEquals(sb.toString(), "acd");
    }

    @Test
    public void testTransformPageElementsWikiPage() {
        {
            PageElementTransformer unchangedTrans = mock(PageElementTransformer.class);
            when(unchangedTrans.transformPageElement(any())).thenAnswer(invocation -> invocation.getArgument(0));
            WikiPage wikiPage = TransformerHelper.transformPageElements(new WikiPage("/a", null, null, null), unchangedTrans);
            assertEquals(wikiPage.getPagePath(), "/a");
            verify(unchangedTrans, times(1)).transformPageElement(any());
        }
        {
            PageElementTransformer nullTrans = mock(PageElementTransformer.class);
            when(nullTrans.transformPageElement(any())).thenReturn(null);
            WikiPage wikiPage = TransformerHelper.transformPageElements(new WikiPage("/a", null, null, null), nullTrans);
            assertEquals(wikiPage.getPagePath(), "/a");
            verify(nullTrans, times(1)).transformPageElement(any());
        }
    }

    @Test
    public void testTransformPageElementsPageElementList() {
        PageElementList pel = new PageElementList();
        pel.add(new TextOnly("a"));
        pel.add(new TextOnly("b"));
        pel.add(new TextOnly("c"));
        PageElementTransformer transformer = mock(PageElementTransformer.class);
        when(transformer.transformPageElement(any())).thenAnswer(invocation -> {
            PageElement pageElement = invocation.getArgument(0);
            if (pageElement instanceof TextOnly && ((TextOnly) pageElement).getText().equals("b")) {
                // remove list element "b"
                return null;
            } else {
                return pageElement;
            }
        });
        // test method
        WikiPage wikiPage = TransformerHelper.transformPageElements(new WikiPage(null, pel, null, null), transformer);
        assertSame(wikiPage.getChild(), pel);
        assertEquals(pel.size(), 2);
        assertEquals(((TextOnly) pel.get(0)).getText(), "a");
        assertEquals(((TextOnly) pel.get(1)).getText(), "c");
    }

    @Test
    public void testTransformPageElementsWithChild() {
        TextOnly textOnly = new TextOnly("content");
        Bold bold = new Bold(textOnly, null, null);
        PageElementTransformer transformer = mock(PageElementTransformer.class);
        when(transformer.transformPageElement(any())).thenAnswer(invocation -> {
            PageElement pageElement = invocation.getArgument(0);
            if (pageElement instanceof Bold) {
                // replace element Bold -> Italic
                return new Italic(((Bold) pageElement).getChild(), null, null);
            } else {
                return pageElement;
            }
        });
        // test method
        WikiPage wikiPage = TransformerHelper.transformPageElements(new WikiPage(null, bold, null, null), transformer);
        assertTrue(wikiPage.getChild() instanceof Italic);
        assertSame(((Italic) wikiPage.getChild()).getChild(), textOnly);
    }

    @Test
    public void testTransformPageElementsTable() {
        TextOnly textOnly = new TextOnly("content");
        TableCell tableCell = new TableCell(textOnly, false, null);
        TableRow tableRow = new TableRow(null);
        tableRow.addCell(tableCell);
        Table table = new Table(null, null, null);
        table.addRow(tableRow);
        PageElementTransformer transformer = mock(PageElementTransformer.class);
        when(transformer.transformPageElement(any())).thenAnswer(invocation -> {
            PageElement pageElement = invocation.getArgument(0);
            if (pageElement instanceof TextOnly) {
                // replace element
                return new TextOnly("newcontent");
            } else {
                return pageElement;
            }
        });
        // test method
        WikiPage wikiPage = TransformerHelper.transformPageElements(new WikiPage(null, table, null, null), transformer);
        assertTrue(wikiPage.getChild() instanceof Table);
        PageElement pe = ((Table) wikiPage.getChild()).getRows().get(0).getCells().get(0).getContent();
        assertTrue(pe instanceof TextOnly);
        assertEquals(((TextOnly) pe).getText(), "newcontent");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testGetContextWikiPage() {
        // no WikiPage in hierarchy
        assertNull(WikiHelper.getContextWikiPage(null, false));
        assertNull(WikiHelper.getContextWikiPage(new TextOnly("a"), false));
        {
            // simple WikiPage
            WikiPage wikiPage = WikiHelper.getContextWikiPage(new WikiPage("/a", null, null, null), false);
            assertEquals(wikiPage.getPagePath(), "/a");
        }
        {
            // single WikiPage in hierarchy
            TextOnly textOnly = new TextOnly("content");
            new WikiPage("/a", textOnly, null, null);
            WikiPage result = WikiHelper.getContextWikiPage(textOnly, false);
            assertEquals(result.getPagePath(), "/a");
        }
        {
            // multiple WikiPages in hierarchy; find local context
            TextOnly textOnly = new TextOnly("content");
            WikiPage wikiPageLocal = new WikiPage("/local", textOnly, null, null);
            new WikiPage("/global", wikiPageLocal, null, null);
            WikiPage result = WikiHelper.getContextWikiPage(textOnly, false);
            assertEquals(result.getPagePath(), "/local");
        }
        {
            // multiple WikiPages in hierarchy; find global context (= tree root)
            TextOnly textOnly = new TextOnly("content");
            WikiPage wikiPageLocal = new WikiPage("/local", textOnly, null, null);
            new WikiPage("/global", wikiPageLocal, null, null);
            WikiPage result = WikiHelper.getContextWikiPage(textOnly, true);
            assertEquals(result.getPagePath(), "/global");
        }
    }

    @Test
    public void testGetAbsolutePagePath() {
        // no context -> null
        assertNull(WikiHelper.getAbsolutePagePath(null, null));
        assertNull(WikiHelper.getAbsolutePagePath("/a", null));
        assertNull(WikiHelper.getAbsolutePagePath("a", null));
        {
            // no page path
            String absolutePagePath = WikiHelper.getAbsolutePagePath(null, new WikiPage("/path/wikipage", null, null, null));
            assertEquals(absolutePagePath, "/path/wikipage");
        }
        {
            // absolute page path
            String absolutePagePath = WikiHelper.getAbsolutePagePath("/a", new WikiPage("/path/wikipage", null, null, null));
            assertEquals(absolutePagePath, "/a");
        }
        {
            // relative page path
            String absolutePagePath = WikiHelper.getAbsolutePagePath("a/b", new WikiPage("/path/wikipage", null, null, null));
            assertEquals(absolutePagePath, "/path/a/b");
        }
    }

    @Test
    public void testGetStringContent() {
        PageElementList pel = new PageElementList();
        pel.add(new TextOnly("a"));
        pel.add(new Bold(new TextOnly("b"), null, null));
        assertEquals(WikiHelper.getStringContent(pel), "ab");
    }

    @Test
    public void testGetIdString() {
        assertEquals(WikiHelper.getIdString("abc"), "abc");
        assertEquals(WikiHelper.getIdString("_abc"), "_abc");
        assertEquals(WikiHelper.getIdString("123-.:abc"), "abc");
    }
}