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

package net.moasdawiki.service.wiki.parser;

import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.structure.*;
import org.testng.annotations.Test;

import java.io.StringReader;

import static org.testng.Assert.*;

public class WikiParserTestBlockElements {

    @Test
    public void testParseEmpty() throws Exception {
        String text = "";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertNotNull(pel);
        assertEquals(pel.size(), 0);
    }

    @Test
    public void testParseCommentLine() throws Exception {
        String text = "// line comment";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 0);
    }

    @Test
    public void testParseCommentBlock() throws Exception {
        String text = "/* block\n"
                + "comment */";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 0);
    }

    @Test
    public void testParseHeading() throws Exception {
        {
            String text = "= heading 1";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Heading);
            Heading heading = (Heading) pel.get(0);
            assertEquals(heading.getLevel(), 1);
            assertEquals(WikiHelper.getStringContent(heading), "heading 1");
        }
        {
            String text = "= heading 1 =";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertTrue(pel.get(0) instanceof Heading);
            assertEquals(WikiHelper.getStringContent(pel), "heading 1 ");
        }
        {
            String text = "== heading 2";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertTrue(pel.get(0) instanceof Heading);
            Heading heading = (Heading) pel.get(0);
            assertEquals(heading.getLevel(), 2);
            assertEquals(WikiHelper.getStringContent(heading), "heading 2");
        }
        {
            String text = "=== heading 3";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertTrue(pel.get(0) instanceof Heading);
            Heading heading = (Heading) pel.get(0);
            assertEquals(heading.getLevel(), 3);
            assertEquals(WikiHelper.getStringContent(heading), "heading 3");
        }
    }

    @Test
    public void testParseSeparator() throws Exception {
        String text = "----";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 1);
        assertTrue(pel.get(0) instanceof Separator);
    }

    @Test
    public void testParseTask() throws Exception {
        {
            String text = "[ ] open task";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Task);
            Task task = (Task) pel.get(0);
            assertEquals(task.getState(), Task.State.OPEN);
            assertNull(task.getSchedule());
            assertEquals(task.getDescription(), "open task");
        }
        {
            String text = "[!] important task";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertTrue(pel.get(0) instanceof Task);
            Task task = (Task) pel.get(0);
            assertEquals(task.getState(), Task.State.OPEN_IMPORTANT);
        }
        {
            String text = "[x] closed task (lower case x)";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertTrue(pel.get(0) instanceof Task);
            Task task = (Task) pel.get(0);
            assertEquals(task.getState(), Task.State.CLOSED);
        }
        {
            String text = "[X] closed task (upper case X)";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertTrue(pel.get(0) instanceof Task);
            Task task = (Task) pel.get(0);
            assertEquals(task.getState(), Task.State.CLOSED);
        }
        {
            String text = "[ ] schedule | open task";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Task);
            Task task = (Task) pel.get(0);
            assertEquals(task.getState(), Task.State.OPEN);
            assertEquals(task.getSchedule(), "schedule");
            assertEquals(task.getDescription(), "open task");
        }
    }

    @Test
    public void testParseUnorderedList() throws Exception {
        {
            String text = "* item level 1";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof ListItem);
            ListItem item = (ListItem) pel.get(0);
            assertEquals(item.getLevel(), 1);
            assertFalse(item.isOrdered());
            assertEquals(WikiHelper.getStringContent(item), "item level 1");
        }
        {
            String text = "** item level 2";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof ListItem);
            ListItem item = (ListItem) pel.get(0);
            assertEquals(item.getLevel(), 2);
            assertFalse(item.isOrdered());
            assertEquals(WikiHelper.getStringContent(item), "item level 2");
        }
    }

    @Test
    public void testParseOrderedList() throws Exception {
        {
            String text = "# item level 1";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof ListItem);
            ListItem item = (ListItem) pel.get(0);
            assertEquals(item.getLevel(), 1);
            assertTrue(item.isOrdered());
            assertEquals(WikiHelper.getStringContent(item), "item level 1");
        }
        {
            String text = "## item level 2";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof ListItem);
            ListItem item = (ListItem) pel.get(0);
            assertEquals(item.getLevel(), 2);
            assertTrue(item.isOrdered());
            assertEquals(WikiHelper.getStringContent(item), "item level 2");
        }
    }

    @Test
    public void testParseCode() throws Exception {
        {
            // single code line
            String text = "@@\n"
                    + "code line\n"
                    + "@@";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Code);
            Code code = (Code) pel.get(0);
            assertNull(code.getLanguage());
            assertEquals(code.getText(), "code line");
        }
        {
            // multiple code line
            String text = "@@\n"
                    + "code line 1\n"
                    + "code line 2\n"
                    + "@@";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Code);
            Code code = (Code) pel.get(0);
            assertNull(code.getLanguage());
            assertEquals(code.getText(), "code line 1\ncode line 2");
        }
        {
            // language
            String text = "@@|java\n"
                    + "code line\n"
                    + "@@";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Code);
            Code code = (Code) pel.get(0);
            assertEquals(code.getLanguage(), "java");
            assertEquals(code.getText(), "code line");
        }
    }

    @Test
    public void testParseTableBasics() throws Exception {
        {
            // empty table
            String text = "{|\n"
                    + "|}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Table);
            Table table = (Table) pel.get(0);
            assertTrue(table.getRows().isEmpty());
        }
        {
            // table CSS params
            String text = "{|params\n"
                    + "|}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Table);
            Table table = (Table) pel.get(0);
            assertEquals(table.getParams(), "params");
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testParseTableCells() throws Exception {
        {
            // cells; row in one line
            String text = "{|\n"
                    + "|| h1 || h2 |-\n"
                    + "| a | b |-\n"
                    + "| c | d |-\n"
                    + "|}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Table);
            Table table = (Table) pel.get(0);
            assertEquals(table.getRows().size(), 3);
            // header row
            assertEquals(table.getRows().get(0).getCells().size(), 2);
            assertTrue(table.getRows().get(0).getCells().get(0).isHeader());
            assertEquals(WikiHelper.getStringContent(table.getRows().get(0).getCells().get(0).getContent()).trim(), "h1");
            assertTrue(table.getRows().get(0).getCells().get(1).isHeader());
            assertEquals(WikiHelper.getStringContent(table.getRows().get(0).getCells().get(1).getContent()).trim(), "h2");
            // row 2
            assertEquals(table.getRows().get(1).getCells().size(), 2);
            assertFalse(table.getRows().get(1).getCells().get(0).isHeader());
            assertEquals(WikiHelper.getStringContent(table.getRows().get(1).getCells().get(0).getContent()).trim(), "a");
            assertFalse(table.getRows().get(1).getCells().get(1).isHeader());
            assertEquals(WikiHelper.getStringContent(table.getRows().get(1).getCells().get(1).getContent()).trim(), "b");
            // row 3
            assertEquals(table.getRows().get(2).getCells().size(), 2);
            assertFalse(table.getRows().get(2).getCells().get(0).isHeader());
            assertEquals(WikiHelper.getStringContent(table.getRows().get(2).getCells().get(0).getContent()).trim(), "c");
            assertFalse(table.getRows().get(2).getCells().get(1).isHeader());
            assertEquals(WikiHelper.getStringContent(table.getRows().get(2).getCells().get(1).getContent()).trim(), "d");
        }
        {
            // cells; row in multiple lines
            String text = "{|\n"
                    + "|| h1\n"
                    + "| b\n"
                    + "|-\n"
                    + "|}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Table);
            Table table = (Table) pel.get(0);
            assertEquals(table.getRows().size(), 1);
            assertEquals(table.getRows().get(0).getCells().size(), 2);
            assertTrue(table.getRows().get(0).getCells().get(0).isHeader());
            assertEquals(WikiHelper.getStringContent(table.getRows().get(0).getCells().get(0).getContent()).trim(), "h1");
            assertFalse(table.getRows().get(0).getCells().get(1).isHeader());
            assertEquals(WikiHelper.getStringContent(table.getRows().get(0).getCells().get(1).getContent()).trim(), "b");
        }
        {
            // cell with multi line content
            String text = "{|\n"
                    + "| this text \n"
                    + "has more \n"
                    + "lines\n"
                    + "|-\n"
                    + "|}";
            PageElementList pel = new WikiParser(new StringReader(text)).parse();
            assertEquals(pel.size(), 1);
            assertTrue(pel.get(0) instanceof Table);
            Table table = (Table) pel.get(0);
            assertEquals(table.getRows().size(), 1);
            assertEquals(table.getRows().get(0).getCells().size(), 1);
            assertEquals(WikiHelper.getStringContent(table.getRows().get(0).getCells().get(0).getContent()).trim(), "this text has more lines");
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testParseTableNested() throws Exception {
        String text = "{|\n"
                + "|{|inner-table\n"
                + "| inner-a | inner-b |-\n"
                + "|}\n"
                + "| outer-c |-\n"
                + "|}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 1);
        assertTrue(pel.get(0) instanceof Table);
        Table table = (Table) pel.get(0);
        assertEquals(table.getRows().size(), 1);
        assertEquals(table.getRows().get(0).getCells().size(), 2);
        // inner table
        assertTrue(table.getRows().get(0).getCells().get(0).getContent() instanceof PageElementList);
        PageElementList innerPel = (PageElementList) table.getRows().get(0).getCells().get(0).getContent();
        assertEquals(innerPel.size(), 1);
        assertTrue(innerPel.get(0) instanceof Table);
        Table innerTable = (Table) innerPel.get(0);
        assertEquals(innerTable.getParams(), "inner-table");
        assertEquals(innerTable.getRows().size(), 1);
        assertEquals(innerTable.getRows().get(0).getCells().size(), 2);
        assertEquals(WikiHelper.getStringContent(innerTable.getRows().get(0).getCells().get(0).getContent()).trim(), "inner-a");
        assertEquals(WikiHelper.getStringContent(innerTable.getRows().get(0).getCells().get(1).getContent()).trim(), "inner-b");
        // outer cell
        assertEquals(WikiHelper.getStringContent(table.getRows().get(0).getCells().get(1).getContent()).trim(), "outer-c");
    }

    @Test
    public void testParseAnchor() throws Exception {
        String text = "{{# anchorname}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 1);
        assertTrue(pel.get(0) instanceof Anchor);
        Anchor anchor = (Anchor) pel.get(0);
        assertEquals(anchor.getName(), "anchorname");
    }

    @Test
    public void testParseTableOfContents() throws Exception {
        String text = "{{toc}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 1);
        assertTrue(pel.get(0) instanceof TableOfContents);
    }

    @Test
    public void testParseParent() throws Exception {
        String text = "{{parent:parentname}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 1);
        assertTrue(pel.get(0) instanceof Parent);
        Parent parent = (Parent) pel.get(0);
        assertEquals(parent.getParentPagePath(), "parentname");
    }

    @Test
    public void testParseIncludePage() throws Exception {
        String text = "{{includepage:pagePath}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 1);
        assertTrue(pel.get(0) instanceof IncludePage);
        IncludePage includePage = (IncludePage) pel.get(0);
        assertEquals(includePage.getPagePath(), "pagePath");
    }

    @Test
    public void testParseVerticalSpace() throws Exception {
        String text = "{{vspace}}";
        PageElementList pel = new WikiParser(new StringReader(text)).parse();
        assertEquals(pel.size(), 1);
        assertTrue(pel.get(0) instanceof VerticalSpace);
    }
}
