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

package net.moasdawiki.service.render;

import net.moasdawiki.base.Messages;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class WikiPage2HtmlTest {

    private Settings settings;
    private Messages messages;
    private WikiService wikiService;

    @BeforeMethod
    public void setUp() {
        settings = mock(Settings.class);
        messages = mock(Messages.class);
        when(messages.getMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        wikiService = mock(WikiService.class);
    }

    @Test
    public void testGenerateHeading() {
        {
            PageElement contentPage = new Heading(1, null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<h1></h1>");
        }
        {
            PageElement contentPage = new Heading(2, new TextOnly("heading"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<h2 id=\"heading\">heading</h2>");
        }
        {
            PageElement contentPage = new Heading(3, new TextOnly("heading"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<h3 id=\"heading\">heading</h3>");
        }
        {
            PageElement contentPage = new Heading(4, new TextOnly("heading"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<p id=\"heading\">heading</p>");
        }
    }

    @Test
    public void testGenerateSeparator() {
        PageElement contentPage = new Separator(null, null);
        HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
        assertEquals(getHtml(htmlWriter), "<hr>");
    }

    @Test
    public void testGenerateVerticalSpace() {
        PageElement contentPage = new VerticalSpace(null, null);
        HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
        assertEquals(getHtml(htmlWriter), "<div class=\"verticalspace\"></div>");
    }

    @Test
    public void testGenerateTask() {
        {
            PageElement contentPage = new Task(null, null, null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<div class=\"task open\"></div>");
        }
        {
            PageElement contentPage = new Task(Task.State.OPEN, null, "task description", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<div class=\"task open\">task description</div>");
        }
        {
            PageElement contentPage = new Task(Task.State.OPEN_IMPORTANT, "schedule", "task description", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<div class=\"task important\"><span class=\"schedule\">schedule</span>task description</div>");
        }
        {
            PageElement contentPage = new Task(Task.State.CLOSED, null, "task description", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<div class=\"task closed\">task description</div>");
        }
    }

    @Test
    public void testGenerateUnorderedListItem() {
        {
            PageElement contentPage = new UnorderedListItem(1, null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<ul>\n" +
                    "  <li></li>\n" +
                    "</ul>");
        }
        {
            PageElement contentPage = new UnorderedListItem(2, new TextOnly("line"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<ul>\n" +
                    "  <ul>\n" +
                    "    <li>line</li>\n" +
                    "  </ul></ul>");
        }
        {
            PageElementList pel = new PageElementList();
            pel.add(new UnorderedListItem(1, new TextOnly("line 1.1"), null, null));
            pel.add(new UnorderedListItem(1, new TextOnly("line 1.2"), null, null));
            pel.add(new UnorderedListItem(2, new TextOnly("line 2.1"), null, null));
            pel.add(new UnorderedListItem(1, new TextOnly("line 3.1"), null, null));
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(pel);
            assertEquals(getHtml(htmlWriter), "<ul>\n" +
                    "  <li>line 1.1</li>\n" +
                    "  <li>line 1.2</li>\n" +
                    "  <ul>\n" +
                    "    <li>line 2.1</li>\n" +
                    "  </ul>\n" +
                    "  <li>line 3.1</li>\n" +
                    "</ul>");
        }
    }

    @Test
    public void testGenerateOrderedListItem() {
        {
            PageElement contentPage = new OrderedListItem(1, null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<ol>\n" +
                    "  <li></li>\n" +
                    "</ol>");
        }
        {
            PageElement contentPage = new OrderedListItem(2, new TextOnly("line"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<ol>\n" +
                    "  <ol>\n" +
                    "    <li>line</li>\n" +
                    "  </ol></ol>");
        }
        {
            PageElementList pel = new PageElementList();
            pel.add(new OrderedListItem(1, new TextOnly("line 1.1"), null, null));
            pel.add(new OrderedListItem(1, new TextOnly("line 1.2"), null, null));
            pel.add(new OrderedListItem(2, new TextOnly("line 2.1"), null, null));
            pel.add(new UnorderedListItem(1, new TextOnly("line 3.1"), null, null));
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(pel);
            assertEquals(getHtml(htmlWriter), "<ol>\n" +
                    "  <li>line 1.1</li>\n" +
                    "  <li>line 1.2</li>\n" +
                    "  <ol>\n" +
                    "    <li>line 2.1</li>\n" +
                    "  </ol>\n" +
                    "</ol>\n" +
                    "<ul>\n" +
                    "  <li>line 3.1</li>\n" +
                    "</ul>");
        }
    }

    @Test
    public void testGenerateTable() {
        {
            Table table = new Table(null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(table);
            assertEquals(getHtml(htmlWriter), "<div class=\"table\"><table>\n" +
                    "  </table></div>");
        }
        {
            TableCell cell1 = new TableCell(new TextOnly("header cell 1"), true, null);
            TableCell cell2 = new TableCell(new TextOnly("header cell 2"), true, "cellparam");
            TableCell cell3 = new TableCell(new TextOnly("cell 3"), false, null);
            TableCell cell4 = new TableCell(new TextOnly("cell 4"), false, null);
            TableRow row1 = new TableRow(null);
            row1.addCell(cell1);
            row1.addCell(cell2);
            TableRow row2 = new TableRow("rowparam");
            row2.addCell(cell3);
            row2.addCell(cell4);
            Table table = new Table("tableparams", null, null);
            table.addRow(row1);
            table.addRow(row2);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(table);
            assertEquals(getHtml(htmlWriter), "<div class=\"table\"><table class=\"tableparams\">\n" +
                    "    <tr>\n" +
                    "      <th>header cell 1</th>\n" +
                    "      <th class=\"cellparam\">header cell 2</th>\n" +
                    "    </tr>\n" +
                    "    <tr class=\"rowparam\">\n" +
                    "      <td>cell 3</td>\n" +
                    "      <td>cell 4</td>\n" +
                    "    </tr>\n" +
                    "  </table></div>");
        }
    }

    @Test
    public void testGenerateParagraph() {
        {
            Paragraph paragraph = new Paragraph(false, 0, false, null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(paragraph);
            assertEquals(getHtml(htmlWriter), "<div class=\"paragraph0\"></div>");
        }
        {
            Paragraph paragraph = new Paragraph(true, 0, false, new TextOnly("centered"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(paragraph);
            assertEquals(getHtml(htmlWriter), "<div class=\"paragraph0 center\">centered</div>");
        }
        {
            PageElementList pel = new PageElementList();
            pel.add(new Paragraph(false, 0, false, null, null, null));
            pel.add(new Paragraph(false, 1, true, null, null, null));
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(pel);
            assertEquals(getHtml(htmlWriter), "<div class=\"paragraph0\"></div>\n" +
                    "<div class=\"verticalspace\"></div>\n" +
                    "<div class=\"paragraph1\"></div>");
        }
    }

    @Test
    public void testGenerateCode() {
        {
            Code code = new Code(null, "final", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(code);
            assertEquals(getHtml(htmlWriter), "<div class=\"code\">final</div>");
        }
        {
            Code code = new Code(null, "text&with<special\nchars\tetc", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(code);
            assertEquals(getHtml(htmlWriter), "<div class=\"code\">text&amp;with&lt;special<br>\n" +
                    "chars&nbsp;etc</div>");
        }
        {
            Code code = new Code("java", "final", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(code);
            assertEquals(getHtml(htmlWriter), "<div class=\"code\"><span class=\"code-java-keyword\">final</span></div>");
        }
        {
            Code code = new Code("html", "<tag>", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(code);
            assertEquals(getHtml(htmlWriter), "<div class=\"code\"><span class=\"code-xml-tag\">&lt;tag&gt;</span></div>");
        }
        {
            Code code = new Code("xml", "<tag>", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(code);
            assertEquals(getHtml(htmlWriter), "<div class=\"code\"><span class=\"code-xml-tag\">&lt;tag&gt;</span></div>");
        }
        {
            Code code = new Code("properties", "a=b", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(code);
            assertEquals(getHtml(htmlWriter), "<div class=\"code\"><span class=\"code-properties-key\">a</span><span class=\"code-properties-delimiter\">=</span><span class=\"code-properties-value\">b</span></div>");
        }
    }

    @Test
    public void testGenerateBold() {
        {
            PageElement contentPage = new Bold(null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<b></b>");
        }
        {
            PageElement contentPage = new Bold(new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<b>content</b>");
        }
    }

    @Test
    public void testGenerateItalic() {
        {
            PageElement contentPage = new Italic(null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<i></i>");
        }
        {
            PageElement contentPage = new Italic(new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<i>content</i>");
        }
    }

    @Test
    public void testGenerateUnderlined() {
        {
            PageElement contentPage = new Underlined(null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<u></u>");
        }
        {
            PageElement contentPage = new Underlined(new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<u>content</u>");
        }
    }

    @Test
    public void testGenerateStrikethrough() {
        {
            PageElement contentPage = new Strikethrough(null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<strike></strike>");
        }
        {
            PageElement contentPage = new Strikethrough(new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<strike>content</strike>");
        }
    }

    @Test
    public void testGenerateMonospace() {
        {
            PageElement contentPage = new Monospace(null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<tt></tt>");
        }
        {
            PageElement contentPage = new Monospace(new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<tt>content</tt>");
        }
    }

    @Test
    public void testGenerateSmall() {
        {
            PageElement contentPage = new Small(null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<span class=\"small\"></span>");
        }
        {
            PageElement contentPage = new Small(new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<span class=\"small\">content</span>");
        }
    }

    @Test
    public void testGenerateColor() {
        {
            PageElement contentPage = new Color("red", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<font color=\"red\"></font>");
        }
        {
            PageElement contentPage = new Color("green", new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<font color=\"green\">content</font>");
        }
    }

    @Test
    public void testGenerateStyle() {
        {
            PageElement contentPage = new Style(new String[]{}, null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<span class=\"\"></span>");
        }
        {
            PageElement contentPage = new Style(new String[]{"style1", "style2"}, new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<span class=\"style1 style2\">content</span>");
        }
    }

    @Test
    public void testGenerateNowiki() {
        {
            PageElement contentPage = new Nowiki(null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "");
        }
        {
            PageElement contentPage = new Nowiki("content", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "content");
        }
        {
            PageElement contentPage = new Nowiki("content<b>with@@tags", null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "content&lt;b&gt;with@@tags");
        }
    }

    @Test
    public void testGenerateHtml() {
        PageElement contentPage = new Html("<b>bold</b>text", null, null);
        HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
        assertEquals(getHtml(htmlWriter), "<b>bold</b>text");
    }

    @Test
    public void testGenerateLinkPage() {
        {
            PageElement contentPage = new LinkPage(null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"\"></a>");
        }
        {
            // link without context page -> empty url
            PageElement contentPage = new LinkPage("/pagePath", null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"\">pagePath</a>");
        }
        {
            // link without context page -> empty url
            PageElement contentPage = new LinkPage("/pagePath", new TextOnly("alternative text"));
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"\">alternative text</a>");
        }
        {
            // absolute link with context page
            LinkPage linkPage = new LinkPage("/linkPath", null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkPage, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a class=\"linknewpage\" href=\"/edit/linkPath\">linkPath</a>");
        }
        {
            // relative link with context page
            LinkPage linkPage = new LinkPage("linkPath", new TextOnly("alternative text"));
            PageElement contentPage = new WikiPage("/path/contextPage", linkPage, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a class=\"linknewpage\" href=\"/edit/path/linkPath\">alternative text</a>");
        }
        {
            PageElement contentPage = new LinkPage("/pagePath", "anchor", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"#anchor\">pagePath#anchor</a>");
        }
        {
            LinkPage linkPage = new LinkPage("/path/", null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkPage, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"/view/path/\">path</a>");
        }
    }

    @Test
    public void testGenerateLinkWiki() {
        {
            PageElement contentPage = new LinkWiki("startpage", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"/\">ViewPageHandler.wiki.startpage</a>");
        }
        {
            // editpage without context page -> no link
            PageElement contentPage = new LinkWiki("editpage", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "ViewPageHandler.wiki.editpage");
        }
        {
            // editpage with context page
            LinkWiki linkWiki = new LinkWiki("editpage", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkWiki, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"/edit/path/contextPage\">ViewPageHandler.wiki.editpage</a>");
        }
        {
            // editpage with context page
            LinkWiki linkWiki = new LinkWiki("editpage", new TextOnly("alternative text"), null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkWiki, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"/edit/path/contextPage\">alternative text</a>");
        }
        {
            // newpage without context page
            PageElement contentPage = new LinkWiki("newpage", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"/edit/\">ViewPageHandler.wiki.newpage</a>");
        }
        {
            // newpage with context page
            LinkWiki linkWiki = new LinkWiki("newpage", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkWiki, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"/edit/path/\">ViewPageHandler.wiki.newpage</a>");
        }
        {
            PageElement contentPage = new LinkWiki("shutdown", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"/shutdown\">ViewPageHandler.wiki.shutdown</a>");
        }
        {
            PageElement contentPage = new LinkWiki("status", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a href=\"/status\">ViewPageHandler.wiki.status</a>");
        }
        {
            PageElement contentPage = new LinkWiki("unknown", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "wiki:unknown?");
        }
    }

    @Test
    public void testGenerateLinkLocalFile() {
        {
            // without context page -> no link
            PageElement contentPage = new LinkLocalFile("/filePath", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "");
        }
        {
            // absolute link with context page
            LinkLocalFile linkLocalFile = new LinkLocalFile("/filePath", new TextOnly("alternative text"), null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkLocalFile, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a class=\"linkfile\" href=\"/file/filePath\">alternative text</a>");
        }
        {
            // relative link with context page
            LinkLocalFile linkLocalFile = new LinkLocalFile("filePath", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkLocalFile, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a class=\"linkfile\" href=\"/file/path/filePath\">filePath</a>");
        }
    }

    @Test
    public void testGenerateLinkExternal() {
        {
            PageElement contentPage = new LinkExternal("https://moasdawiki.net/", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a class=\"linkexternal\" href=\"https://moasdawiki.net/\">https://moasdawiki.net/</a>");
        }
        {
            PageElement contentPage = new LinkExternal("https://moasdawiki.net/", new TextOnly("alternative text"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a class=\"linkexternal\" href=\"https://moasdawiki.net/\">alternative text</a>");
        }
        {
            PageElement contentPage = new LinkExternal("mailto:user@domain.org", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<a class=\"linkemail\" href=\"mailto:user@domain.org\">user@domain.org</a>");
        }
    }

    @Test
    public void testGenerateXmlTag() {
        {
            PageElement contentPage = new XmlTag(null, "tag", null, null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "");
        }
        {
            PageElement contentPage = new XmlTag("prefix", "tag", null, new TextOnly("content"), null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "content");
        }
    }

    @Test
    public void testGenerateTextOnly() {
        PageElement contentPage = new TextOnly("content");
        HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
        assertEquals(getHtml(htmlWriter), "content");
    }

    @Test
    public void testGenerateLineBreak() {
        PageElement contentPage = new LineBreak();
        HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
        assertEquals(getHtml(htmlWriter), "<br>");
    }

    @Test
    public void testGenerateAnchor() {
        PageElement contentPage = new Anchor("anchorname");
        HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
        assertEquals(getHtml(htmlWriter), "<a name=\"anchorname\"></a>");
    }

    @Test
    public void testGenerateImage() {
        {
            // without context page -> no url
            PageElement contentPage = new Image("/image.png", null, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "");
        }
        {
            // absolute url with context page
            Image image = new Image("/image.png", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", image, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<img src=\"/img/image.png\" alt=\"\">");
        }
        {
            // relative url with context page
            Image image = new Image("image.png", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", image, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<img src=\"/img/path/image.png\" alt=\"\">");
        }
        {
            Map<String, String> options = new HashMap<>();
            options.put("key1", "value1");
            options.put("key2", "value2");
            Image image = new Image("image.png", options, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", image, null, null);
            HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
            assertEquals(getHtml(htmlWriter), "<img src=\"/img/path/image.png\" key1=\"value1\" key2=\"value2\" alt=\"\">");
        }
    }

    @Test
    public void testGenerateSearchInput() {
        PageElement contentPage = new SearchInput(null, null);
        HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(contentPage);
        assertEquals(getHtml(htmlWriter), "<form method=\"get\" action=\"/search/\" enctype=\"application/x-www-form-urlencoded; charset=utf-8\" name=\"searchForm\"><input type=\"text\" name=\"text\" placeholder=\"ViewPageHandler.html.search\"></form>");
    }

    private static String getHtml(HtmlWriter htmlWriter) {
        htmlWriter.closeAllTags();
        StringBuilder sb = new StringBuilder();
        for (String line : htmlWriter.getBodyLines()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString();
    }
}
