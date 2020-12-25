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

import java.util.Map;

import net.moasdawiki.base.Messages;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.render.HtmlWriter.Method;
import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.Anchor;
import net.moasdawiki.service.wiki.structure.Bold;
import net.moasdawiki.service.wiki.structure.Code;
import net.moasdawiki.service.wiki.structure.Color;
import net.moasdawiki.service.wiki.structure.Heading;
import net.moasdawiki.service.wiki.structure.Html;
import net.moasdawiki.service.wiki.structure.Image;
import net.moasdawiki.service.wiki.structure.Italic;
import net.moasdawiki.service.wiki.structure.LineBreak;
import net.moasdawiki.service.wiki.structure.LinkExternal;
import net.moasdawiki.service.wiki.structure.LinkLocalFile;
import net.moasdawiki.service.wiki.structure.LinkPage;
import net.moasdawiki.service.wiki.structure.LinkWiki;
import net.moasdawiki.service.wiki.structure.Monospace;
import net.moasdawiki.service.wiki.structure.Nowiki;
import net.moasdawiki.service.wiki.structure.OrderedListItem;
import net.moasdawiki.service.wiki.structure.PageElement;
import net.moasdawiki.service.wiki.structure.PageElementList;
import net.moasdawiki.service.wiki.structure.Paragraph;
import net.moasdawiki.service.wiki.structure.SearchInput;
import net.moasdawiki.service.wiki.structure.Separator;
import net.moasdawiki.service.wiki.structure.Small;
import net.moasdawiki.service.wiki.structure.Strikethrough;
import net.moasdawiki.service.wiki.structure.Style;
import net.moasdawiki.service.wiki.structure.Table;
import net.moasdawiki.service.wiki.structure.TableCell;
import net.moasdawiki.service.wiki.structure.TableRow;
import net.moasdawiki.service.wiki.structure.Task;
import net.moasdawiki.service.wiki.structure.TextOnly;
import net.moasdawiki.service.wiki.structure.Underlined;
import net.moasdawiki.service.wiki.structure.UnorderedListItem;
import net.moasdawiki.service.wiki.structure.VerticalSpace;
import net.moasdawiki.service.wiki.structure.WikiPage;
import net.moasdawiki.service.wiki.structure.XmlTag;
import net.moasdawiki.util.EscapeUtils;
import net.moasdawiki.util.PathUtils;
import net.moasdawiki.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts a wiki page to a HTML text.
 *
 * Not thread-safe!
 */
public class WikiPage2Html {

	private static final String HTML_EDIT_CODE_KEY = "ViewPageHandler.html.editCode";
	private static final String HTML_EDIT_SECTION_KEY = "ViewPageHandler.html.editSection";
	private static final String HTML_EDIT_TABLE_KEY = "ViewPageHandler.html.editTable";
	private static final String HTML_EDIT_TABLE_CELL_KEY = "ViewPageHandler.html.editTableCell";
	private static final String HTML_SEARCH_KEY = "ViewPageHandler.html.search";
	private static final String WIKI_EDIT_PAGE_KEY = "ViewPageHandler.wiki.editpage";
	private static final String WIKI_NEW_PAGE_KEY = "ViewPageHandler.wiki.newpage";
	private static final String WIKI_SHUTDOWN_KEY = "ViewPageHandler.wiki.shutdown";
	private static final String WIKI_STARTPAGE_KEY = "ViewPageHandler.wiki.startpage";
	private static final String WIKI_STATUS_KEY = "ViewPageHandler.wiki.status";

	private final Messages messages;
	private final WikiService wikiService;
	private final boolean generateEditLinks;
	private HtmlWriter writer;
	private PageElement previousElement;

	/**
	 * Constructor.
	 */
	public WikiPage2Html(@SuppressWarnings("unused") @NotNull Settings settings, @NotNull Messages messages, @NotNull WikiService wikiService, boolean generateEditLinks) {
		super();
		this.messages = messages;
		this.wikiService = wikiService;
		this.generateEditLinks = generateEditLinks;
	}

	/**
	 * Convert a wiki page to a HTML Text.
	 */
	@NotNull
	public HtmlWriter generate(@NotNull PageElement contentPage) {
		writer = new HtmlWriter();
		convertGeneric(contentPage);
		return writer;
	}

	/**
	 * Convert a single page element.
	 *
	 * Wiki-internal elements are ignored as they have to be transformed in advance.
	 */
	private void convertGeneric(@Nullable PageElement element) {
		if (element == null) {
			return;
		}

		// close open enumerations
		if (!(element instanceof UnorderedListItem) && !(element instanceof OrderedListItem)) {
			setEnumerationLevel(0, false);
		}

		// Begin a new HTML line for a block element.
		// Inline elements continue in the same line.
		if (!element.isInline()) {
			writer.setContinueInNewLine();
		}

		if (element instanceof PageElementList) {
			convertPageElement((PageElementList) element);
		} else if (element instanceof WikiPage) {
			convertPageElement((WikiPage) element);
		} else if (element instanceof Heading) {
			convertPageElement((Heading) element);
		} else if (element instanceof Separator) {
			convertPageElement((Separator) element);
		} else if (element instanceof VerticalSpace) {
			convertPageElementVerticalSpace();
		} else if (element instanceof Task) {
			convertPageElement((Task) element);
		} else if (element instanceof UnorderedListItem) {
			convertPageElement((UnorderedListItem) element);
		} else if (element instanceof OrderedListItem) {
			convertPageElement((OrderedListItem) element);
		} else if (element instanceof Table) {
			convertPageElement((Table) element);
		} else if (element instanceof Paragraph) {
			convertPageElement((Paragraph) element);
		} else if (element instanceof Code) {
			convertPageElement((Code) element);
		} else if (element instanceof Bold) {
			convertPageElement((Bold) element);
		} else if (element instanceof Italic) {
			convertPageElement((Italic) element);
		} else if (element instanceof Underlined) {
			convertPageElement((Underlined) element);
		} else if (element instanceof Strikethrough) {
			convertPageElement((Strikethrough) element);
		} else if (element instanceof Monospace) {
			convertPageElement((Monospace) element);
		} else if (element instanceof Small) {
			convertPageElement((Small) element);
		} else if (element instanceof Color) {
			convertPageElement((Color) element);
		} else if (element instanceof Style) {
			convertPageElement((Style) element);
		} else if (element instanceof Nowiki) {
			convertPageElement((Nowiki) element);
		} else if (element instanceof Html) {
			convertPageElement((Html) element);
		} else if (element instanceof LinkPage) {
			convertPageElement((LinkPage) element);
		} else if (element instanceof LinkWiki) {
			convertPageElement((LinkWiki) element);
		} else if (element instanceof LinkLocalFile) {
			convertPageElement((LinkLocalFile) element);
		} else if (element instanceof LinkExternal) {
			convertPageElement((LinkExternal) element);
		} else if (element instanceof XmlTag) {
			convertPageElement((XmlTag) element);
		} else if (element instanceof TextOnly) {
			convertPageElement((TextOnly) element);
		} else if (element instanceof LineBreak) {
			convertPageElement((LineBreak) element);
		} else if (element instanceof Anchor) {
			convertPageElement((Anchor) element);
		} else if (element instanceof Image) {
			convertPageElement((Image) element);
		} else if (element instanceof SearchInput) {
			convertPageElement((SearchInput) element);
		}

		// After a block element we have to start in a new line.
		if (!element.isInline()) {
			writer.setContinueInNewLine();
		}

		// remember the last element, except anchors as they are invisible
		if (!(element instanceof Anchor)) {
			previousElement = element;
		}
	}

	private void convertPageElement(@NotNull PageElementList pageElementList) {
		for (PageElement pe : pageElementList) {
			convertGeneric(pe);
		}
	}

	private void convertPageElement(@NotNull WikiPage wikiPage) {
		convertGeneric(wikiPage.getChild());
	}

	private void convertPageElement(@NotNull Heading heading) {
		String tagName;
		switch (heading.getLevel()) {
		case 1:
			tagName = "h1";
			break;
		case 2:
			tagName = "h2";
			break;
		case 3:
			tagName = "h3";
			break;
		default:
			// levels >= 4 are not emphasized
			tagName = "p";
		}

		// open heading tag
		String contentString = WikiHelper.getStringContent(heading);
		String param = null;
		if (!contentString.isEmpty()) {
			param = "id=\"" + WikiHelper.getIdString(contentString) + "\"";
		}
		int depth = writer.openTag(tagName, param);

		// generate edit icon for the section
		WikiPage wikiPage = WikiHelper.getContextWikiPage(heading, false);
		Integer toPos = getSectionToPos(heading);
		if (wikiPage != null && wikiPage.getPagePath() != null && heading.getFromPos() != null && toPos != null && generateEditLinks) {
			String url = PathUtils.concatWebPaths("/edit/", wikiPage.getPagePath());
			url = EscapeUtils.pagePath2Url(url) + "?fromPos=" + heading.getFromPos() + "&toPos=" + toPos;
			String msg = messages.getMessage(HTML_EDIT_SECTION_KEY);
			writer.htmlText("<a class=\"editsection\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url)) + "\"><img src=\"/edit2.png\" title=\""
					+ EscapeUtils.escapeHtml(msg) + "\" alt=\"\"></a>");
		}

		// write heading text
		convertGeneric(heading.getChild());
		writer.closeTags(depth);
	}

	/**
	 * Determine the end position of the section starting with the given heading.
	 *
	 * The section ends with the following heading (of same or higher importance),
	 * the end of the table cell or the end of the wiki page.
	 *
	 * @param heading Heading the current section starts with.
	 * @return end position of the section; null -> unknown.
	 */
	@Nullable
	private Integer getSectionToPos(@NotNull Heading heading) {
		PageElement parent = heading.getParent();
		if (!(parent instanceof PageElementList)) {
			return null;
		}
		PageElementList pel = (PageElementList) parent;

		// find current heading
		int index = 0;
		while (index < pel.size() && pel.get(index) != heading) {
			index++;
		}

		// find next heading
		index++; // ignore current heading
		while (index < pel.size()) {
			PageElement element = pel.get(index);
			if (element instanceof Heading) {
				Heading h = (Heading) element;
				if (h.getLevel() <= heading.getLevel()) {
					// section ends here
					return h.getFromPos();
				}
			}
			index++;
		}

		// no corresponding following heading found, use last list element
		return pel.getToPos();
	}

	private void convertPageElement(@SuppressWarnings("unused") @NotNull Separator separator) {
		writer.htmlText("<hr>");
	}

	private void convertPageElementVerticalSpace() {
		writer.openDivTag("verticalspace");
		writer.closeTag();
		writer.setContinueInNewLine();
	}

	private void convertPageElement(@NotNull Task task) {
		String cssClass;
		if (task.getState() == Task.State.OPEN_IMPORTANT) {
			cssClass = "task important";
		} else if (task.getState() == Task.State.CLOSED) {
			cssClass = "task closed";
		} else {
			cssClass = "task open";
		}
		int depth = writer.openDivTag(cssClass);

		if (task.getSchedule() != null) {
			writer.openSpanTag("schedule");
			writer.htmlText(task.getSchedule());
			writer.closeTag();
		}

		if (task.getDescription() != null) {
			writer.htmlText(EscapeUtils.escapeHtml(task.getDescription()));
		}
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull UnorderedListItem unorderedListItem) {
		setEnumerationLevel(unorderedListItem.getLevel(), true);
		int depth = writer.openTag("li");
		convertGeneric(unorderedListItem.getChild());
		writer.closeTags(depth); // li
	}

	private void convertPageElement(@NotNull OrderedListItem orderedListItem) {
		setEnumerationLevel(orderedListItem.getLevel(), false);
		int depth = writer.openTag("li");
		convertGeneric(orderedListItem.getChild());
		writer.closeTags(depth); // li
	}

	/**
	 * Opens or closes enumeration tags to reach the given target stack depth.
	 */
	private void setEnumerationLevel(int level, boolean unordered) {
		// determine current stack depth
		int currentLevel = 0;
		while (writer.getCurrentTag(currentLevel) != null
				&& ("ul".equals(writer.getCurrentTag(currentLevel)) || "ol".equals(writer.getCurrentTag(currentLevel)))) {
			currentLevel++;
		}

		// reduce stack depth
		while (currentLevel > level) {
			writer.closeTag();
			writer.setContinueInNewLine();
			currentLevel--;
		}

		// adjust enumeration type
		if (currentLevel > 0 && currentLevel == level
				&& ((unordered && !"ul".equals(writer.getCurrentTag())) || (!unordered && !"ol".equals(writer.getCurrentTag())))) {
			writer.closeTag();
			writer.setContinueInNewLine();
			currentLevel--;
		}

		// increase stack depth
		while (currentLevel < level) {
			if (unordered) {
				writer.openTag("ul");
			} else {
				writer.openTag("ol");
			}
			writer.setContinueInNewLine();
			currentLevel++;
		}
	}

	private void convertPageElement(@NotNull Table table) {
		int depth = writer.openDivTag("table");
		if (generateEditLinks && table.getFromPos() != null && table.getToPos() != null) {
			WikiPage wikiPage = WikiHelper.getContextWikiPage(table, false);
			if (wikiPage != null && wikiPage.getPagePath() != null) {
				String url = PathUtils.concatWebPaths("/edit/", wikiPage.getPagePath());
				url = EscapeUtils.pagePath2Url(url) + "?fromPos=" + table.getFromPos() + "&toPos=" + table.getToPos();
				String msg = messages.getMessage(HTML_EDIT_TABLE_KEY);
				writer.htmlText("<a class=\"edittable\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url)) + "\"><img src=\"/edit.png\" title=\""
						+ EscapeUtils.escapeHtml(msg) + "\" alt=\"\"></a>");
			}
		}

		String tableParameters = null;
		if (table.getParams() != null) {
			tableParameters = "class=\"" + EscapeUtils.escapeHtml(table.getParams()) + '\"';
		}
		writer.openTag("table", tableParameters);

		for (TableRow row : table.getRows()) {
			writer.setContinueInNewLine();
			String rowParameters = null;
			if (row.getParams() != null) {
				rowParameters = "class=\"" + EscapeUtils.escapeHtml(row.getParams()) + '\"';
			}
			writer.openTag("tr", rowParameters);
			for (TableCell cell : row.getCells()) {
				writer.setContinueInNewLine();
				String cellParameters = null;
				if (cell.getParams() != null) {
					cellParameters = "class=\"" + EscapeUtils.escapeHtml(cell.getParams()) + '\"';
				}
				int cellTagLevel;
				if (cell.isHeader()) {
					cellTagLevel = writer.openTag("th", cellParameters);
				} else {
					cellTagLevel = writer.openTag("td", cellParameters);
				}

				PageElement cellContent = cell.getContent();
				if (generateEditLinks && cellContent != null && cellContent.getFromPos() != null && cellContent.getToPos() != null) {
					writer.openDivTag("tablecell");
					WikiPage wikiPage = WikiHelper.getContextWikiPage(table, false);
					if (wikiPage != null && wikiPage.getPagePath() != null) {
						String url = PathUtils.concatWebPaths("/edit/", wikiPage.getPagePath());
						url += "?fromPos=" + cellContent.getFromPos() + "&toPos=" + cellContent.getToPos();
						String msg = messages.getMessage(HTML_EDIT_TABLE_CELL_KEY);
						writer.htmlText("<a class=\"editcell\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url))
								+ "\"><img src=\"/edit.png\" title=\"" + EscapeUtils.escapeHtml(msg) + "\" alt=\"\"></a>");
					}
				}

				previousElement = null; // no space before paragraph
				convertGeneric(cell.getContent());
				writer.closeTags(cellTagLevel); // div + td
			}
			writer.setContinueInNewLine();
			writer.closeTag(); // tr
		}

		writer.setContinueInNewLine();
		writer.closeTags(depth); // div + table
	}

	/**
	 * Converts a paragraph.
	 *
	 * Add additional vertical space if the previous page element was a
	 * paragraph, an enumeration, or a table.
	 */
	private void convertPageElement(@NotNull Paragraph paragraph) {
		// add vertical space
		if (paragraph.hasVerticalSpacing()
				&& (previousElement instanceof Paragraph || previousElement instanceof UnorderedListItem || previousElement instanceof OrderedListItem || previousElement instanceof Table)) {
			convertPageElementVerticalSpace();
		}

		// style
		String classtype = "paragraph" + paragraph.getIndention();
		if (paragraph.isCentered()) {
			classtype += " center";
		}

		int depth = writer.openDivTag(classtype);
		convertGeneric(paragraph.getChild());
		writer.closeTags(depth);
	}

	/**
	 * Converts a code block (with syntax highlighting).
	 *
	 * Cannot use <tt>white-space: pre</tt> as line breaks are missing when copy & paste is used.
	 * Thus, the formatting has to be done by HTML tags.
	 */
	private void convertPageElement(@NotNull Code code) {
		int depth = writer.openDivTag("code");

		// edit icon for section editing
		if (generateEditLinks && code.getFromPos() != null && code.getToPos() != null) {
			WikiPage wikiPage = WikiHelper.getContextWikiPage(code, false);
			if (wikiPage != null && wikiPage.getPagePath() != null) {
				String url = PathUtils.concatWebPaths("/edit/", wikiPage.getPagePath());
				url = EscapeUtils.pagePath2Url(url) + "?fromPos=" + code.getFromPos() + "&toPos=" + code.getToPos();
				String msg = messages.getMessage(HTML_EDIT_CODE_KEY);
				writer.htmlText("<a class=\"editcode\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url)) + "\"><img src=\"/edit.png\" title=\""
						+ EscapeUtils.escapeHtml(msg) + "\" alt=\"\"></a>");
			}
		}

		String formattedCode;
		if ("Java".equalsIgnoreCase(code.getLanguage())) {
			JavaFormatter javaFormatter = new JavaFormatter(code.getText());
			formattedCode = javaFormatter.format();
		} else if ("HTML".equalsIgnoreCase(code.getLanguage()) || "XML".equalsIgnoreCase(code.getLanguage())) {
			XmlFormatter xmlFormatter = new XmlFormatter(code.getText());
			formattedCode = xmlFormatter.format();
		} else if ("properties".equalsIgnoreCase(code.getLanguage())) {
			PropertiesFormatter propertiesFormatter = new PropertiesFormatter(code.getText());
			formattedCode = propertiesFormatter.format();
		} else {
			formattedCode = formatCode(code.getText());
		}
		writer.htmlText(formattedCode);

		writer.closeTags(depth);
	}

	/**
	 * Format code with line breaks and spaces.
	 */
	@NotNull
	private static String formatCode(@NotNull String codeText) {
		String str = EscapeUtils.escapeHtml(codeText);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			switch (ch) {
			case '\n':
				sb.append("<br>\n");
				break;
			case '\r':
				// ignore character
				break;
			case ' ':
			case '\t':
				sb.append("&nbsp;");
				break;
			default:
				sb.append(ch);
			}
		}

		return sb.toString();
	}

	private void convertPageElement(@NotNull Bold bold) {
		int depth = writer.openTag("b");
		convertGeneric(bold.getChild());
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull Italic italic) {
		int depth = writer.openTag("i");
		convertGeneric(italic.getChild());
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull Underlined underlined) {
		int depth = writer.openTag("u");
		convertGeneric(underlined.getChild());
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull Strikethrough strikethrough) {
		int depth = writer.openTag("strike");
		convertGeneric(strikethrough.getChild());
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull Monospace monospace) {
		int depth = writer.openTag("tt");
		convertGeneric(monospace.getChild());
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull Small small) {
		int depth = writer.openSpanTag("small");
		convertGeneric(small.getChild());
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull Color color) {
		int depth = writer.openTag("font", "color=\"" + EscapeUtils.escapeHtml(color.getColorName()) + "\"");
		convertGeneric(color.getChild());
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull Style style) {
		String stylesheetClass = StringUtils.concat(style.getCssClasses(), " ");
		int depth = writer.openSpanTag(stylesheetClass);
		convertGeneric(style.getChild());
		writer.closeTags(depth);
	}

	private void convertPageElement(@NotNull Nowiki nowiki) {
		String text = EscapeUtils.escapeHtml(nowiki.getText());
		if (text != null) {
			text = text.replaceAll("\n", "<br>\n");
		}
		writer.htmlText(text);
	}

	private void convertPageElement(@NotNull Html html) {
		writer.htmlText(html.getText());
	}

	private void convertPageElement(@NotNull LinkPage link) {
		// Kontext bestimmen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(link, false);

		// show edit icon only if the target page doesn't exist
		boolean showEditorPen = false;

		// generate URL
		String url;
		String linkPagePath = WikiHelper.getAbsolutePagePath(link.getPagePath(), wikiPage);
		if (link.getPagePath() != null && linkPagePath != null) {
			if (linkPagePath.endsWith("/") || wikiService.existsWikiFile(linkPagePath)) {
				url = "/view" + linkPagePath;
			} else {
				url = "/edit" + linkPagePath;
				showEditorPen = true;
			}
		} else {
			url = ""; // anchor in current page
		}
		url = EscapeUtils.pagePath2Url(url);
		if (link.getAnchor() != null && !showEditorPen) {
			url += '#' + EscapeUtils.encodeUrlParameter(link.getAnchor());
		}
		String cssClassParam = "";
		if (showEditorPen) {
			cssClassParam = "class=\"linknewpage\" ";
		}
		int depth = writer.openTag("a", cssClassParam + "href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url)) + "\"");

		// link text
		if (link.getAlternativeText() != null) {
			convertGeneric(link.getAlternativeText());
		} else {
			// show only page name
			String localLinkPagePath = link.getPagePath();
			if (localLinkPagePath != null) {
				if (!localLinkPagePath.endsWith("/")) {
					// page name without path
					String pageName = PathUtils.extractWebName(localLinkPagePath);
					writer.htmlText(EscapeUtils.escapeHtml(pageName));
				} else {
					// page name with path
					String folderPath = localLinkPagePath.substring(0, localLinkPagePath.length() - 1);
					String folderName = PathUtils.extractWebName(folderPath);
					if (folderName.isEmpty()) {
						folderName = "/";
					}
					writer.htmlText(EscapeUtils.escapeHtml(folderName));
				}
			}
			// show anchor as well
			if (link.getAnchor() != null && !showEditorPen) {
				writer.htmlText('#' + EscapeUtils.escapeHtml(link.getAnchor()));
			}
		}

		writer.closeTags(depth); // a
	}

	private void convertPageElement(@NotNull LinkWiki link) {
		String url;
		String text;
		switch (link.getCommand()) {
			case "startpage":
				url = "/";
				text = messages.getMessage(WIKI_STARTPAGE_KEY);
				break;
			case "editpage": {
				// determine global page context as the tag refers to the whole page
				WikiPage wikiPage = WikiHelper.getContextWikiPage(link, true);
				if (wikiPage != null && wikiPage.getPagePath() != null) {
					url = PathUtils.concatWebPaths("/edit/", wikiPage.getPagePath());
				} else {
					url = null; // no link
				}
				text = messages.getMessage(WIKI_EDIT_PAGE_KEY);
				break;
			}
			case "newpage": {
				// determine global page context to get the path to the whole page
				WikiPage wikiPage = WikiHelper.getContextWikiPage(link, true);
				if (wikiPage != null && wikiPage.getPagePath() != null) {
					url = PathUtils.concatWebPaths("/edit/", PathUtils.extractWebFolder(wikiPage.getPagePath()));
				} else {
					url = "/edit/";
				}
				text = messages.getMessage(WIKI_NEW_PAGE_KEY);
				break;
			}
			case "shutdown":
				url = "/shutdown";
				text = messages.getMessage(WIKI_SHUTDOWN_KEY);
				break;
			case "status":
				url = "/status";
				text = messages.getMessage(WIKI_STATUS_KEY);
				break;
			default:
				// ungültiger Wiki-Link
				url = null;
				text = "wiki:" + EscapeUtils.escapeHtml(link.getCommand()) + "?";
				break;
		}

		if (url != null) {
			writer.openTag("a", "href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(EscapeUtils.pagePath2Url(url))) + "\"");
		}
		if (link.getAlternativeText() != null) {
			convertGeneric(link.getAlternativeText());
		} else {
			writer.htmlText(text);
		}
		if (url != null) {
			writer.closeTag(); // a
		}
	}

	private void convertPageElement(@NotNull LinkLocalFile link) {
		WikiPage wikiPage = WikiHelper.getContextWikiPage(link, false);

		// URL
		String linkFilePath = link.getFilePath();
		String pagePath = WikiHelper.getAbsolutePagePath(linkFilePath, wikiPage);
		if (pagePath == null) {
			return;
		}
		String url = "/file" + pagePath;
		int depth = writer.openTag("a", "class=\"linkfile\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(EscapeUtils.pagePath2Url(url))) + "\"");

		if (link.getAlternativeText() != null) {
			convertGeneric(link.getAlternativeText());
		} else {
			writer.htmlText(EscapeUtils.escapeHtml(link.getFilePath()));
		}

		writer.closeTags(depth); // a
	}

	private void convertPageElement(@NotNull LinkExternal link) {
		String cssClass;
		if (link.getUrl().startsWith("mailto:")) {
			cssClass = "linkemail";
		} else {
			cssClass = "linkexternal";
		}

		int depth = writer.openTag("a", "class=\"" + cssClass + "\" href=\"" + EscapeUtils.escapeHtml(link.getUrl()) + "\"");

		if (link.getAlternativeText() != null) {
			convertGeneric(link.getAlternativeText());
		} else if (link.getUrl().startsWith("mailto:")) {
			// cut off "mailto:"
			writer.htmlText(EscapeUtils.escapeHtml(link.getUrl().substring(7)));
		} else {
			writer.htmlText(EscapeUtils.escapeHtml(link.getUrl()));
		}

		writer.closeTags(depth); // a
	}

	/**
	 * Convert only the content of the semantic tag but ignore the tag name itself.
	 * Obviously there was no Transformer to handle this tag.
	 */
	private void convertPageElement(@NotNull XmlTag xmlTag) {
		convertGeneric(xmlTag.getChild());
	}

	private void convertPageElement(@NotNull TextOnly textOnly) {
		writer.htmlText(EscapeUtils.escapeHtml(textOnly.getText()));
	}

	private void convertPageElement(@SuppressWarnings("unused") @NotNull LineBreak lineBreak) {
		writer.htmlNewLine();
	}

	private void convertPageElement(@NotNull Anchor anchor) {
		writer.openTag("a", "name=\"" + EscapeUtils.escapeHtml(anchor.getName()) + "\"");
		writer.closeTag();
	}

	private void convertPageElement(@NotNull Image image) {
		WikiPage wikiPage = WikiHelper.getContextWikiPage(image, false);

		String url = image.getUrl();
		if (!url.startsWith("http")) {
			url = WikiHelper.getAbsolutePagePath(url, wikiPage);
			if (url == null) {
				return;
			}

			url = EscapeUtils.pagePath2Url("/img" + url);
			url = EscapeUtils.encodeUrl(url);
		}

		writer.htmlText("<img src=\"" + url + '\"' + optionsToString(image.getOptions()) + " alt=\"\">");
	}

	private void convertPageElement(@SuppressWarnings("unused") @NotNull SearchInput searchInput) {
		int depth = writer.openFormTag("searchForm", "/search/", Method.GET);
		String hint = messages.getMessage(HTML_SEARCH_KEY);
		writer.htmlText("<input type=\"text\" name=\"text\" placeholder=\"" + EscapeUtils.escapeHtml(hint) + "\">");
		writer.closeTags(depth);
	}

	@NotNull
	private String optionsToString(@NotNull Map<String, String> options) {
		StringBuilder s = new StringBuilder();
		for (String name : options.keySet()) {
			s.append(' ');
			s.append(name);
			s.append("=\"");
			s.append(options.get(name));
			s.append('"');
		}
		return s.toString();
	}
}
