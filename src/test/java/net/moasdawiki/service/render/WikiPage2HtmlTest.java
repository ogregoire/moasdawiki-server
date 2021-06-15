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
            String html = convertPageElement(contentPage);
            assertEquals(html, "<h1></h1>");
        }
        {
            PageElement contentPage = new Heading(2, new TextOnly("heading"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<h2 id=\"heading\">heading</h2>");
        }
        {
            PageElement contentPage = new Heading(3, new TextOnly("heading"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<h3 id=\"heading\">heading</h3>");
        }
        {
            PageElement contentPage = new Heading(4, new TextOnly("heading"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<p id=\"heading\">heading</p>");
        }
    }

    @Test
    public void testGenerateSeparator() {
        PageElement contentPage = new Separator(null, null);
        String html = convertPageElement(contentPage);
        assertEquals(html, "<hr>");
    }

    @Test
    public void testGenerateVerticalSpace() {
        PageElement contentPage = new VerticalSpace(null, null);
        String html = convertPageElement(contentPage);
        assertEquals(html, "<div class=\"verticalspace\"></div>");
    }

    @Test
    public void testGenerateTask() {
        {
            PageElement contentPage = new Task(null, null, null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"task open\"></div>");
        }
        {
            PageElement contentPage = new Task(Task.State.OPEN, null, "task description", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"task open\">task description</div>");
        }
        {
            PageElement contentPage = new Task(Task.State.OPEN_IMPORTANT, "schedule", "task description", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"task important\"><span class=\"schedule\">schedule</span>task description</div>");
        }
        {
            PageElement contentPage = new Task(Task.State.CLOSED, null, "task description", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"task closed\">task description</div>");
        }
    }

    @Test
    public void testGenerateListItem() {
        {
            PageElement contentPage = new ListItem(1, false, null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<ul><li class=\"level1\"></li></ul>");
        }
        {
            PageElement contentPage = new ListItem(1, true, null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<ol start=\"1\"><li class=\"level1\"></li></ol>");
        }
        {
            PageElement contentPage = new ListItem(2, false, new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<ul><li class=\"level2\">content</li></ul>");
        }
        {
            PageElement contentPage = new ListItem(2, true, new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<ol start=\"1\"><li class=\"level2\">content</li></ol>");
        }
        {
            PageElementList pel = new PageElementList();
            pel.add(new ListItem(1, false, new TextOnly("line 1"), null, null));
            pel.add(new ListItem(1, false, new TextOnly("line 2"), null, null));
            pel.add(new ListItem(2, false, new TextOnly("line 3"), null, null));
            pel.add(new ListItem(1, false, new TextOnly("line 4"), null, null));
            String html = convertPageElement(pel);
            assertEquals(html, "<ul><li class=\"level1\">line 1</li></ul>\n" +
                    "<ul><li class=\"level1\">line 2</li></ul>\n" +
                    "<ul><li class=\"level2\">line 3</li></ul>\n" +
                    "<ul><li class=\"level1\">line 4</li></ul>");
        }
        {
            PageElementList pel = new PageElementList();
            pel.add(new ListItem(1, true, new TextOnly("line 1"), null, null));
            pel.add(new ListItem(1, true, new TextOnly("line 2"), null, null));
            pel.add(new ListItem(2, true, new TextOnly("line 3"), null, null));
            pel.add(new ListItem(3, true, new TextOnly("line 4"), null, null));
            pel.add(new ListItem(2, true, new TextOnly("line 5"), null, null));
            pel.add(new ListItem(1, true, new TextOnly("line 6"), null, null));
            String html = convertPageElement(pel);
            assertEquals(html, "<ol start=\"1\"><li class=\"level1\">line 1</li></ol>\n" +
                    "<ol start=\"2\"><li class=\"level1\">line 2</li></ol>\n" +
                    "<ol start=\"1\"><li class=\"level2\">line 3</li></ol>\n" +
                    "<ol start=\"1\"><li class=\"level3\">line 4</li></ol>\n" +
                    "<ol start=\"2\"><li class=\"level2\">line 5</li></ol>\n" +
                    "<ol start=\"3\"><li class=\"level1\">line 6</li></ol>");
        }
    }

    @Test
    public void testGetNextListItemSequence() {
        // invalid parameter values
        assertEquals(1, WikiPage2Html.getNextListItemSequence(0, null));
        assertEquals(1, WikiPage2Html.getNextListItemSequence(-1, null));
        assertEquals(1, WikiPage2Html.getNextListItemSequence(1, null));
        assertEquals(1, WikiPage2Html.getNextListItemSequence(0, new int[3]));
        assertEquals(1, WikiPage2Html.getNextListItemSequence(-1, new int[3]));
        assertEquals(1, WikiPage2Html.getNextListItemSequence(10, new int[3]));

        {
            // same level
            int[] listItemSequence = {0, 0, 0};
            assertEquals(1, WikiPage2Html.getNextListItemSequence(1, listItemSequence));
            assertEquals(2, WikiPage2Html.getNextListItemSequence(1, listItemSequence));
            assertEquals(3, WikiPage2Html.getNextListItemSequence(1, listItemSequence));
            assertEquals(4, WikiPage2Html.getNextListItemSequence(1, listItemSequence));
            assertEquals(1, WikiPage2Html.getNextListItemSequence(2, listItemSequence));
            assertEquals(2, WikiPage2Html.getNextListItemSequence(2, listItemSequence));
            assertEquals(3, WikiPage2Html.getNextListItemSequence(2, listItemSequence));
        }
        {
            // reset higher levels
            int[] listItemSequence = {0, 0, 0};
            assertEquals(1, WikiPage2Html.getNextListItemSequence(1, listItemSequence));
            assertEquals(1, WikiPage2Html.getNextListItemSequence(2, listItemSequence));
            assertEquals(2, WikiPage2Html.getNextListItemSequence(2, listItemSequence));
            assertEquals(2, WikiPage2Html.getNextListItemSequence(1, listItemSequence));
            assertEquals(1, WikiPage2Html.getNextListItemSequence(2, listItemSequence));
            assertEquals(2, WikiPage2Html.getNextListItemSequence(2, listItemSequence));
            assertEquals(3, WikiPage2Html.getNextListItemSequence(1, listItemSequence));
        }
    }

    @Test
    public void testGenerateTable() {
        {
            Table table = new Table(null, null, null);
            String html = convertPageElement(table);
            assertEquals(html, "<div class=\"table\"><table>\n" +
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
            String html = convertPageElement(table);
            assertEquals(html, "<div class=\"table\"><table class=\"tableparams\">\n" +
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
            Paragraph contentPage = new Paragraph(false, 0, false, null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"paragraph0\"></div>");
        }
        {
            Paragraph contentPage = new Paragraph(true, 0, false, new TextOnly("centered"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"paragraph0 center\">centered</div>");
        }
        {
            PageElementList pel = new PageElementList();
            pel.add(new Paragraph(false, 0, false, null, null, null));
            pel.add(new Paragraph(false, 1, true, null, null, null));
            String html = convertPageElement(pel);
            assertEquals(html, "<div class=\"paragraph0\"></div>\n" +
                    "<div class=\"verticalspace\"></div>\n" +
                    "<div class=\"paragraph1\"></div>");
        }
    }

    @Test
    public void testGenerateCode() {
        {
            PageElement contentPage = new Code(Code.ContentType.NONE, "final", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"code\">final</div>");
        }
        {
            PageElement contentPage = new Code(Code.ContentType.NONE, "text&with<special\nchars\tetc", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"code\">text&amp;with&lt;special<br>\n" +
                    "chars&nbsp;etc</div>");
        }
        {
            PageElement contentPage = new Code(Code.ContentType.HTML, "<tag>", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"code\"><span class=\"code-xml-tag\">&lt;tag&gt;</span></div>");
        }
        {
            PageElement contentPage = new Code(Code.ContentType.INI, "a=b", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"code\"><span class=\"code-ini-key\">a</span><span class=\"code-ini-delimiter\">=</span><span class=\"code-ini-value\">b</span></div>");
        }
        {
            PageElement contentPage = new Code(Code.ContentType.JAVA, "final", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"code\"><span class=\"code-java-keyword\">final</span></div>");
        }
        {
            PageElement contentPage = new Code(Code.ContentType.PROPERTIES, "a=b", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"code\"><span class=\"code-properties-key\">a</span><span class=\"code-properties-delimiter\">=</span><span class=\"code-properties-value\">b</span></div>");
        }
        {
            PageElement contentPage = new Code(Code.ContentType.XML, "<tag>", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"code\"><span class=\"code-xml-tag\">&lt;tag&gt;</span></div>");
        }
        {
            PageElement contentPage = new Code(Code.ContentType.YAML, "name", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<div class=\"code\"><span class=\"code-yaml-key\">name</span></div>");
        }
    }

    @Test
    public void testGenerateBold() {
        {
            PageElement contentPage = new Bold(null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<b></b>");
        }
        {
            PageElement contentPage = new Bold(new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<b>content</b>");
        }
    }

    @Test
    public void testGenerateItalic() {
        {
            PageElement contentPage = new Italic(null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<i></i>");
        }
        {
            PageElement contentPage = new Italic(new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<i>content</i>");
        }
    }

    @Test
    public void testGenerateUnderlined() {
        {
            PageElement contentPage = new Underlined(null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<u></u>");
        }
        {
            PageElement contentPage = new Underlined(new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<u>content</u>");
        }
    }

    @Test
    public void testGenerateStrikethrough() {
        {
            PageElement contentPage = new Strikethrough(null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<s></s>");
        }
        {
            PageElement contentPage = new Strikethrough(new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<s>content</s>");
        }
    }

    @Test
    public void testGenerateMonospace() {
        {
            PageElement contentPage = new Monospace(null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<code></code>");
        }
        {
            PageElement contentPage = new Monospace(new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<code>content</code>");
        }
    }

    @Test
    public void testGenerateSmall() {
        {
            PageElement contentPage = new Small(null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<span class=\"small\"></span>");
        }
        {
            PageElement contentPage = new Small(new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<span class=\"small\">content</span>");
        }
    }

    @Test
    public void testGenerateColor() {
        {
            PageElement contentPage = new Color("red", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<span style=\"color: red\"></span>");
        }
        {
            PageElement contentPage = new Color("green", new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<span style=\"color: green\">content</span>");
        }
    }

    @Test
    public void testGenerateStyle() {
        {
            PageElement contentPage = new Style(new String[]{}, null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<span class=\"\"></span>");
        }
        {
            PageElement contentPage = new Style(new String[]{"style1", "style2"}, new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<span class=\"style1 style2\">content</span>");
        }
    }

    @Test
    public void testGenerateNowiki() {
        {
            PageElement contentPage = new Nowiki(null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "");
        }
        {
            PageElement contentPage = new Nowiki("content", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "content");
        }
        {
            PageElement contentPage = new Nowiki("content<b>with@@tags", null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "content&lt;b&gt;with@@tags");
        }
    }

    @Test
    public void testGenerateHtml() {
        PageElement contentPage = new Html("<b>bold</b>text", null, null);
        String html = convertPageElement(contentPage);
        assertEquals(html, "<b>bold</b>text");
    }

    @Test
    public void testGenerateHtmlTag() {
        PageElement contentPage = new HtmlTag("tagname", "attributes", new TextOnly("content"));
        String html = convertPageElement(contentPage);
        assertEquals(html, "<tagname attributes>content</tagname>");
    }

    @Test
    public void testGenerateLinkPage() {
        {
            PageElement contentPage = new LinkPage(null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"\"></a>");
        }
        {
            // link without context page -> empty url
            PageElement contentPage = new LinkPage("/pagePath", null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"\">pagePath</a>");
        }
        {
            // link without context page -> empty url
            PageElement contentPage = new LinkPage("/pagePath", new TextOnly("alternative text"));
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"\">alternative text</a>");
        }
        {
            // absolute link with context page
            LinkPage linkPage = new LinkPage("/linkPath", null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkPage, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a class=\"linknewpage\" href=\"/edit/linkPath\">linkPath</a>");
        }
        {
            // relative link with context page
            LinkPage linkPage = new LinkPage("linkPath", new TextOnly("alternative text"));
            PageElement contentPage = new WikiPage("/path/contextPage", linkPage, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a class=\"linknewpage\" href=\"/edit/path/linkPath\">alternative text</a>");
        }
        {
            PageElement contentPage = new LinkPage("/pagePath", "anchor", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"#anchor\">pagePath#anchor</a>");
        }
        {
            LinkPage linkPage = new LinkPage("/path/", null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkPage, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"/view/path/\">path</a>");
        }
    }

    @Test
    public void testGenerateLinkWiki() {
        {
            PageElement contentPage = new LinkWiki("startpage", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"/\">ViewPageHandler.wiki.startpage</a>");
        }
        {
            // editpage without context page -> no link
            PageElement contentPage = new LinkWiki("editpage", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "ViewPageHandler.wiki.editpage");
        }
        {
            // editpage with context page
            LinkWiki linkWiki = new LinkWiki("editpage", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkWiki, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"/edit/path/contextPage\">ViewPageHandler.wiki.editpage</a>");
        }
        {
            // editpage with context page
            LinkWiki linkWiki = new LinkWiki("editpage", new TextOnly("alternative text"), null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkWiki, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"/edit/path/contextPage\">alternative text</a>");
        }
        {
            // newpage without context page
            PageElement contentPage = new LinkWiki("newpage", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"/edit/\">ViewPageHandler.wiki.newpage</a>");
        }
        {
            // newpage with context page
            LinkWiki linkWiki = new LinkWiki("newpage", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkWiki, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"/edit/path/\">ViewPageHandler.wiki.newpage</a>");
        }
        {
            PageElement contentPage = new LinkWiki("shutdown", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"/shutdown\">ViewPageHandler.wiki.shutdown</a>");
        }
        {
            PageElement contentPage = new LinkWiki("status", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a href=\"/status\">ViewPageHandler.wiki.status</a>");
        }
        {
            PageElement contentPage = new LinkWiki("unknown", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "wiki:unknown?");
        }
    }

    @Test
    public void testGenerateLinkLocalFile() {
        {
            // without context page -> no link
            PageElement contentPage = new LinkLocalFile("/filePath", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "");
        }
        {
            // absolute link with context page
            LinkLocalFile linkLocalFile = new LinkLocalFile("/filePath", new TextOnly("alternative text"), null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkLocalFile, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a class=\"linkfile\" href=\"/file/filePath\">alternative text</a>");
        }
        {
            // relative link with context page
            LinkLocalFile linkLocalFile = new LinkLocalFile("filePath", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", linkLocalFile, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a class=\"linkfile\" href=\"/file/path/filePath\">filePath</a>");
        }
    }

    @Test
    public void testGenerateLinkExternal() {
        {
            PageElement contentPage = new LinkExternal("https://moasdawiki.net/", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a class=\"linkexternal\" href=\"https://moasdawiki.net/\">https://moasdawiki.net/</a>");
        }
        {
            PageElement contentPage = new LinkExternal("https://moasdawiki.net/", new TextOnly("alternative text"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a class=\"linkexternal\" href=\"https://moasdawiki.net/\">alternative text</a>");
        }
        {
            PageElement contentPage = new LinkExternal("mailto:user@domain.org", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<a class=\"linkemail\" href=\"mailto:user@domain.org\">user@domain.org</a>");
        }
    }

    @Test
    public void testGenerateXmlTag() {
        {
            PageElement contentPage = new XmlTag(null, "tag", null, null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "");
        }
        {
            PageElement contentPage = new XmlTag("prefix", "tag", null, new TextOnly("content"), null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "content");
        }
    }

    @Test
    public void testGenerateTextOnly() {
        PageElement contentPage = new TextOnly("content");
        String html = convertPageElement(contentPage);
        assertEquals(html, "content");
    }

    @Test
    public void testGenerateLineBreak() {
        PageElement contentPage = new LineBreak();
        String html = convertPageElement(contentPage);
        assertEquals(html, "<br>");
    }

    @Test
    public void testGenerateAnchor() {
        PageElement contentPage = new Anchor("anchorname");
        String html = convertPageElement(contentPage);
        assertEquals(html, "<span id=\"anchorname\"></span>");
    }

    @Test
    public void testGenerateImage() {
        {
            // without context page -> no url
            PageElement contentPage = new Image("/image.png", null, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "");
        }
        {
            // absolute url with context page
            Image image = new Image("/image.png", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", image, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<img src=\"/img/image.png\" alt=\"\">");
        }
        {
            // relative url with context page
            Image image = new Image("image.png", null, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", image, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<img src=\"/img/path/image.png\" alt=\"\">");
        }
        {
            Map<String, String> options = new HashMap<>();
            options.put("key1", "value1");
            options.put("key2", "value2");
            Image image = new Image("image.png", options, null, null);
            PageElement contentPage = new WikiPage("/path/contextPage", image, null, null);
            String html = convertPageElement(contentPage);
            assertEquals(html, "<img src=\"/img/path/image.png\" key1=\"value1\" key2=\"value2\" alt=\"\">");
        }
    }

    @Test
    public void testGenerateSearchInput() {
        PageElement contentPage = new SearchInput(null, null);
        String html = convertPageElement(contentPage);
        assertEquals(html, "<form method=\"get\" action=\"/search/\" enctype=\"application/x-www-form-urlencoded\" name=\"searchForm\"><input type=\"text\" name=\"text\" placeholder=\"ViewPageHandler.html.search\"></form>");
    }

    private String convertPageElement(PageElement pageElement) {
        HtmlWriter htmlWriter = new WikiPage2Html(settings, messages, wikiService, false).generate(pageElement);
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
