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
 * Wandelt eine Wiki-Seite in den entsprechenden HTML-Text um.<br>
 * <br>
 * Ist nicht Thread-safe.
 * 
 * @author Herbert Reiter
 */
public class WikiPage2Html {

	private final Messages messages;

	private final WikiService wikiService;

	private final boolean generateEditLinks;

	private HtmlWriter writer;

	private PageElement previousElement;

	public WikiPage2Html(@SuppressWarnings("unused") @NotNull Settings settings, @NotNull Messages messages, @NotNull WikiService wikiService, boolean generateEditLinks) {
		super();
		this.messages = messages;
		this.wikiService = wikiService;
		this.generateEditLinks = generateEditLinks;
	}

	/**
	 * Konvertiert eine Wiki-Seite in die entsprechende HTML-Darstellung.
	 * 
	 * @param contentPage Wiki-Seite, deren Inhalt jetzt verarbeitet werden
	 *        soll.
	 */
	@NotNull
	public HtmlWriter generate(@NotNull PageElement contentPage) {
		writer = new HtmlWriter();
		convertGeneric(contentPage);
		return writer;
	}

	/**
	 * Übersetzt ein einzelnes Seitenelement.<br>
	 * <br>
	 * Wiki-interne Elemente werden hier ignoriert. Diese müssen vorher durch
	 * Plugins in normale Elemente umgewandelt worden sein.
	 * 
	 * @param element Das zu übersetzende Seitenelement.
	 */
	private void convertGeneric(@Nullable PageElement element) {
		if (element == null) {
			return;
		}

		// falls eine Aufzählung oder Nummerierung offen ist,
		// diese ggf. schließen
		if (!(element instanceof UnorderedListItem) && !(element instanceof OrderedListItem)) {
			setEnumerationLevel(0, false);
		}

		// Bei einem Block-Element eine neue HTML-Zeile beginnen,
		// bei einem inline-Element in der selben Zeile weiterschreiben
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

		// Nach einem Block-Element eine neue HTML-Zeile beginnen,
		// nach einem inline-Element darf in der selben Zeile weitergeschrieben
		// werden
		if (!element.isInline()) {
			writer.setContinueInNewLine();
		}

		// letztes Element merken, aber Anker ignorieren, weil die unsichtbar
		// sind
		if (!(element instanceof Anchor)) {
			previousElement = element;
		}
	}

	private void convertPageElement(@NotNull PageElementList pageElementList) {
		for (PageElement pe : pageElementList) {
			convertGeneric(pe);
		}
	}

	/**
	 * Gibt den Inhalt der Unterseite aus, ohne den Namen der Unterseite
	 * anzuzeigen.
	 */
	private void convertPageElement(@NotNull WikiPage wikiPage) {
		convertGeneric(wikiPage.getChild());
	}

	/**
	 * Erzeugt eine Überschrift.<br>
	 * <br>
	 * Eine Überschrift wird nicht automatisch verlinkt. Ein Anker muss ggf.
	 * vorher generiert werden.
	 */
	private void convertPageElement(@NotNull Heading heading) {
		// Tag-Typ bestimmen
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
			// ab Level 4 keine sonstige Hervorhebung mehr
			tagName = "p";
		}

		// Überschrift-Tag öffnen
		String contentString = WikiHelper.getStringContent(heading);
		int depth = writer.openTag(tagName, "id=\"" + WikiHelper.getIdString(contentString) + "\"");

		// Editier-Symbol für Abschnitt generieren
		WikiPage wikiPage = WikiHelper.getContextWikiPage(heading, false);
		Integer toPos = getSectionToPos(heading);
		if (wikiPage != null && wikiPage.getPagePath() != null && heading.getFromPos() != null && toPos != null && generateEditLinks) {
			String url = PathUtils.concatWebPaths("/edit/", wikiPage.getPagePath());
			url = EscapeUtils.pagePath2Url(url) + "?fromPos=" + heading.getFromPos() + "&toPos=" + toPos;
			String msg = messages.getMessage("ViewPagePlugin.html.editSection");
			writer.htmlText("<a class=\"editsection\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url)) + "\"><img src=\"/edit2.png\" title=\""
					+ EscapeUtils.escapeHtml(msg) + "\" alt=\"\"></a>");
		}

		// Überschrift ausgeben
		convertGeneric(heading.getChild());
		writer.closeTags(depth);
	}

	/**
	 * Bestimmt die Endeposition des Abschnitts, der mit der angegebenen
	 * Überschrift beginnt. Ein Abschnitt geht bis zur nächsten Überschrift, die
	 * nicht untergeordnet ist (der Level darf nicht größer sein), ansonsten bis
	 * zum Ende der aktuellen Umgebung (z.B. Tabellenzelle oder Wikiseite).<br>
	 * <br>
	 * Hat die nächste Überschrift den Wert <code>fromPos</code> nicht gesetzt
	 * hat oder das Ende der Wikiseite nicht ermittelt werden konnte, wird
	 * <code>null</code> zurückgegeben.
	 * 
	 * @param heading Überschrift, mit der der Abschnitt beginnt. Nicht null.
	 * @return Endeposition des aktuellen Abschnitts. null -> unbekannt.
	 */
	@Nullable
	private Integer getSectionToPos(@NotNull Heading heading) {
		PageElement parent = heading.getParent();
		if (!(parent instanceof PageElementList)) {
			return null;
		}
		PageElementList pel = (PageElementList) parent;

		// aktuelle Überschrift suchen
		int index = 0;
		while (index < pel.size() && pel.get(index) != heading) {
			index++;
		}

		// nachfolgende Überschrift suchen
		index++; // aktuelle Überschrift ignorieren
		while (index < pel.size()) {
			PageElement element = pel.get(index);
			if (element instanceof Heading) {
				Heading h = (Heading) element;
				if (h.getLevel() <= heading.getLevel()) {
					// Abschnitt endet hier
					return h.getFromPos();
				}
			}
			index++;
		}

		// keine passende nachfolgende Überschrift gefunden,
		// verwende Ende der Liste
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
	 * Öffnet bzw. schließt Aufzählungsumgebungen, damit schließlich die durch
	 * <tt>level</tt> angegebene Schachtelungstiefe vorliegt.
	 */
	private void setEnumerationLevel(int level, boolean unordered) {
		// aktuelle Schachtelungstiefe bestimmen
		int currentLevel = 0;
		while (writer.getCurrentTag(currentLevel) != null
				&& ("ul".equals(writer.getCurrentTag(currentLevel)) || "ol".equals(writer.getCurrentTag(currentLevel)))) {
			currentLevel++;
		}

		// wenn die Schachtelungstiefe zu groß ist, einige Tags schließen
		while (currentLevel > level) {
			writer.closeTag();
			writer.setContinueInNewLine();
			currentLevel--;
		}

		// wenn der Typ des aktuellen Tags nicht stimmt, auch dieses Tag
		// schließen
		if (currentLevel > 0 && currentLevel == level
				&& ((unordered && !"ul".equals(writer.getCurrentTag())) || (!unordered && !"ol".equals(writer.getCurrentTag())))) {
			writer.closeTag();
			writer.setContinueInNewLine();
			currentLevel--;
		}

		// wenn die aktuelle Schachtelungstiefe zu klein ist, Tags öffnen
		while (currentLevel < level) {
			if (unordered) {
				writer.openTag("ul"); // ungeordnete Liste geöffnete
			} else {
				writer.openTag("ol"); // geordnete Liste geöffnet
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
				String msg = messages.getMessage("ViewPagePlugin.html.editTable");
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
				if (cell.getIsHeader()) {
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
						String msg = messages.getMessage("ViewPagePlugin.html.editTableCell");
						writer.htmlText("<a class=\"editcell\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url))
								+ "\"><img src=\"/edit.png\" title=\"" + EscapeUtils.escapeHtml(msg) + "\" alt=\"\"></a>");
					}
				}

				previousElement = null; // kein Abstand vor Absatz
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
	 * Gibt einen Absatz aus. Wenn das vorherige Seitenelement ein Absatz, eine
	 * Aufzählung oder eine Tabelle ist, wird zusätzlich ein Abstand davor
	 * eingefügt.
	 */
	private void convertPageElement(@NotNull Paragraph paragraph) {
		// ggf. vertikalen Abstand zwischen zwei Absätze einfügen
		if (paragraph.hasVerticalSpacing()
				&& (previousElement instanceof Paragraph || previousElement instanceof UnorderedListItem || previousElement instanceof OrderedListItem || previousElement instanceof Table)) {
			convertPageElementVerticalSpace();
		}

		// Formatierung bestimmen
		String classtype = "paragraph" + paragraph.getIndention();
		if (paragraph.isCentered()) {
			classtype += " center";
		}

		int depth = writer.openDivTag(classtype);
		convertGeneric(paragraph.getChild());
		writer.closeTags(depth);
	}

	/**
	 * Formatiert einen Codeblock, ggf. mit Syntaxhervorhebung.<br>
	 * <br>
	 * <tt>white-space: pre</tt> kann nicht verwendet werden, weil dann bei
	 * Copy&Paste die Zeilenumbrüche fehlen. Die Formatierung muss daher per
	 * HTML nachgebildet werden.
	 */
	private void convertPageElement(@NotNull Code code) {
		int depth = writer.openDivTag("code");

		// Stift zum Bearbeiten des Codeblocks anzeigen
		if (generateEditLinks && code.getFromPos() != null && code.getToPos() != null) {
			WikiPage wikiPage = WikiHelper.getContextWikiPage(code, false);
			if (wikiPage != null && wikiPage.getPagePath() != null) {
				String url = PathUtils.concatWebPaths("/edit/", wikiPage.getPagePath());
				url = EscapeUtils.pagePath2Url(url) + "?fromPos=" + code.getFromPos() + "&toPos=" + code.getToPos();
				String msg = messages.getMessage("ViewPagePlugin.html.editCode");
				writer.htmlText("<a class=\"editcode\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url)) + "\"><img src=\"/edit.png\" title=\""
						+ EscapeUtils.escapeHtml(msg) + "\" alt=\"\"></a>");
			}
		}

		// Codeformatierung per HTML nachbilden
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
	 * Formatiert den Code unter Beibehaltung der Zeilenumbrüche und
	 * Leerzeichen, jedoch ohne spezielle Syntaxhervorhebung.
	 */
	@Nullable
	private static String formatCode(@Nullable String codeText) {
		if (codeText == null || codeText.isEmpty()) {
			return null;
		}

		// zunächst alle HTML-Kommandos unschädlich machen
		String str = EscapeUtils.escapeHtml(codeText);

		// Formatierung konvertieren
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			switch (ch) {
			case '\n':
				sb.append("<br>\n");
				break;
			case '\r':
				// Zeichen ignorieren
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

	/**
	 * Zeilenumbrüche werden *nicht* durch &lt;br&gt; ersetzt, weil "nowiki"
	 * überhaupt keine Interpretation verursachen soll.
	 */
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

	/**
	 * Erzeugt einen Link auf eine Wiki-Seite. Ggf. wird ein Anker-Link
	 * innerhalb einer Wiki-Seite erzeugt. Anker-Links beginnen mit dem Symbol
	 * <code>#</code>.
	 */
	private void convertPageElement(@NotNull LinkPage link) {
		// Kontext bestimmen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(link, false);

		// zeige Stift nur, wenn eine Seite nicht existiert
		boolean showEditorPen = false;

		// URL erzeugen
		String linkPagePath = link.getPagePath();
		String url;
		if (link.getPagePath() != null) {
			linkPagePath = WikiHelper.getAbsolutePagePath(linkPagePath, wikiPage);

			if (linkPagePath.endsWith("/") || wikiService.existsWikiFile(linkPagePath)) {
				url = "/view" + linkPagePath;
			} else {
				url = "/edit" + linkPagePath;
				showEditorPen = true;
			}
		} else {
			url = ""; // aktuelle Seite, bei Anker
		}
		url = EscapeUtils.pagePath2Url(url);
		if (link.getAnchor() != null && !showEditorPen) {
			url += '#' + EscapeUtils.encodeUrlParameter(link.getAnchor());
		}
		String cssClassParam = "";
		if (showEditorPen) {
			// Wenn die Seite nicht existiert, noch einen Stift anzeigen
			cssClassParam = "class=\"linknewpage\" ";
		}
		int depth = writer.openTag("a", cssClassParam + "href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(url)) + "\"");

		// Linktext schreiben
		if (link.getAlternativeText() != null) {
			convertGeneric(link.getAlternativeText());
		} else {
			// sonst nur Seitenname angeben
			if (linkPagePath != null) {
				if (!linkPagePath.endsWith("/")) {
					// normaler Seitenname
					String pageName = PathUtils.extractWebName(linkPagePath);
					writer.htmlText(EscapeUtils.escapeHtml(pageName));
				} else {
					// Ordnername
					String folderPath = linkPagePath.substring(0, linkPagePath.length() - 1);
					String folderName = PathUtils.extractWebName(folderPath);
					if (folderName.isEmpty()) {
						folderName = "/";
					}
					writer.htmlText(EscapeUtils.escapeHtml(folderName));
				}
			}
			// ggf. Ankername mit ausgeben
			if (link.getAnchor() != null && !showEditorPen) {
				writer.htmlText('#' + EscapeUtils.escapeHtml(link.getAnchor()));
			}
		}

		writer.closeTags(depth); // a
	}

	/**
	 * Erzeugt einen speziellen Wiki-Link. Diese haben den Präfix "wiki".
	 */
	private void convertPageElement(@NotNull LinkWiki link) {
		String url;
		String text;
		switch (link.getCommand()) {
			case "startpage":
				url = "/";
				text = messages.getMessage("ViewPagePlugin.wiki.startpage");
				break;
			case "editpage": {
				// globalen Kontext bestimmen,
				// weil die gesamte Seite editiert werden soll
				WikiPage wikiPage = WikiHelper.getContextWikiPage(link, true);
				if (wikiPage != null && wikiPage.getPagePath() != null) {
					url = PathUtils.concatWebPaths("/edit/", wikiPage.getPagePath());
				} else {
					url = null; // keinen Link erzeugen
				}
				text = messages.getMessage("ViewPagePlugin.wiki.editpage");
				break;
			}
			case "newpage": {
				// globalen Kontext bestimmen,
				// um den Pfad der gesamten Seite zu erhalten
				WikiPage wikiPage = WikiHelper.getContextWikiPage(link, true);
				if (wikiPage != null && wikiPage.getPagePath() != null) {
					url = PathUtils.concatWebPaths("/edit/", PathUtils.extractWebFolder(wikiPage.getPagePath()));
				} else {
					url = "/edit/";
				}
				text = messages.getMessage("ViewPagePlugin.wiki.newpage");
				break;
			}
			case "shutdown":
				url = "/shutdown";
				text = messages.getMessage("ViewPagePlugin.wiki.shutdown");
				break;
			case "status":
				url = "/status";
				text = messages.getMessage("ViewPagePlugin.wiki.status");
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

	/**
	 * Erzeugt einen Download-Link auf eine Datei im lokalen Dateisystem.
	 */
	private void convertPageElement(@NotNull LinkLocalFile link) {
		// Kontext bestimmen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(link, false);

		// URL erzeugen
		String linkFilePath = link.getFilePath();
		String url = "/file" + WikiHelper.getAbsolutePagePath(linkFilePath, wikiPage);

		int depth = writer.openTag("a", "class=\"linkfile\" href=\"" + EscapeUtils.escapeHtml(EscapeUtils.encodeUrl(EscapeUtils.pagePath2Url(url))) + "\"");

		if (link.getAlternativeText() != null) {
			convertGeneric(link.getAlternativeText());
		} else {
			writer.htmlText(EscapeUtils.escapeHtml(link.getFilePath()));
		}

		writer.closeTags(depth); // a
	}

	/**
	 * Erzeugt einen externen Link, der auf eine Seite außerhalb des Wiki
	 * verweist. Hinter dem Link wird eine Weltkugel-Grafik angezeigt, damit der
	 * Link als externer Link erkennbar ist.
	 */
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
			// "mailto:" abschneiden
			writer.htmlText(EscapeUtils.escapeHtml(link.getUrl().substring(7)));
		} else {
			writer.htmlText(EscapeUtils.escapeHtml(link.getUrl()));
		}

		writer.closeTags(depth); // a
	}

	/**
	 * Gibt den Inhalt des SemanticTag aus, ohne den Namen des semantischen Tags
	 * selbst auszugeben. Offensichtlich war kein Plugin vorhanden, das dieses
	 * Tag behandelt hat.
	 */
	private void convertPageElement(@NotNull XmlTag xmlTag) {
		// XML-Tag selbst ist unsichtbar -> ignorieren
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
		// Kontext bestimmen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(image, false);

		String url = image.getUrl();
		if (!url.startsWith("http")) {
			url = WikiHelper.getAbsolutePagePath(url, wikiPage);

			// Bildname escapen, damit auch Sonderzeichen usw. funktionieren
			url = EscapeUtils.pagePath2Url("/img" + url);
			url = EscapeUtils.encodeUrl(url);
		}

		writer.htmlText("<img src=\"" + url + '\"' + optionsToString(image.getOptions()) + " alt=\"\">");
	}

	private void convertPageElement(@SuppressWarnings("unused") @NotNull SearchInput searchInput) {
		int depth = writer.openFormTag("searchForm", "/search/", Method.GET);
		String hint = messages.getMessage("ViewPagePlugin.html.search");
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
