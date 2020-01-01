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

package net.moasdawiki.service.wiki;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.plugin.ServiceLocator;
import net.moasdawiki.service.wiki.structure.Html;
import net.moasdawiki.service.wiki.structure.PageElement;
import net.moasdawiki.service.wiki.structure.PageElementList;
import net.moasdawiki.service.wiki.structure.PageElementWithChild;
import net.moasdawiki.service.wiki.structure.Table;
import net.moasdawiki.service.wiki.structure.TableCell;
import net.moasdawiki.service.wiki.structure.TableRow;
import net.moasdawiki.service.wiki.structure.TextOnly;
import net.moasdawiki.service.wiki.structure.WikiPage;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stellt Hilfsmethoden zur Traversierung und Transformation von Wikiseiten zur
 * Verfügung.
 * 
 * @author Herbert Reiter
 */
public abstract class WikiHelper {

	/**
	 * Ergänzt eine Wikiseite um die Navigation, Head und Footer.
	 * 
	 * @param wikiPage Die zu ergänzende Wikiseite. Nicht null.
	 * @param addNavigation Soll die Navigation eingefügt werden? Diese kommt
	 *        aus einer Wikiseite, die über "page.navigation" in config.txt
	 *        festgelegt wird.
	 * @param addHeader Soll der Header eingefügt werden? Dieser kommt aus einer
	 *        Wikiseite, die über "page.header" in config.txt festgelegt wird.
	 * @param addFooter Soll der Footer eingefügt werden? Dieser kommt aus einer
	 *        Wikiseite, die über "page.footer" in config.txt festgelegt wird.
	 * @return Die erweiterte Wikiseite. Nicht <code>null</code>.
	 */
	@NotNull
	public static WikiPage extendWikiPage(@NotNull WikiPage wikiPage, boolean addNavigation, boolean addHeader, boolean addFooter, @NotNull ServiceLocator serviceLocator) {
		// Wikiseiten holen
		Logger logger = serviceLocator.getLogger();
		Settings settings = serviceLocator.getSettings();
		WikiFile navigationWikiFile = null;
		try {
			if (settings.getNavigationPagePath() != null) {
				navigationWikiFile = serviceLocator.getWikiService().getWikiFile(settings.getNavigationPagePath());
			}
		}
		catch (ServiceException e) {
			logger.write("Error reading navigation page to assemble an integrated page, ignoring it", e);
		}
		WikiFile headerWikiFile = null;
		try {
			if (settings.getHeaderPagePath() != null) {
				headerWikiFile = serviceLocator.getWikiService().getWikiFile(settings.getHeaderPagePath());
			}
		}
		catch (ServiceException e) {
			logger.write("Error reading header page to assemble an integrated page, ignoring it", e);
		}
		WikiFile footerWikiFile = null;
		try {
			if (settings.getFooterPagePath() != null) {
				footerWikiFile = serviceLocator.getWikiService().getWikiFile(settings.getFooterPagePath());
			}
		}
		catch (ServiceException e) {
			logger.write("Error reading footer page to assemble an integrated page, ignoring it", e);
		}

		// Wiki-Seiten zusammensetzen
		PageElementList content = new PageElementList();
		if (addNavigation && navigationWikiFile != null) {
			content.add(new Html("<div class=\"menu\">"));
			content.add(navigationWikiFile.getWikiPage());
			content.add(new Html("</div>"));
		}

		content.add(new Html("<div class=\"wikipage\">"));
		if (addHeader && headerWikiFile != null) {
			content.add(headerWikiFile.getWikiPage());
		}
		content.add(wikiPage);
		if (addFooter && footerWikiFile != null) {
			content.add(footerWikiFile.getWikiPage());
		}
		content.add(new Html("</div>"));

		return new WikiPage(wikiPage.getPagePath(), content, null, null);
	}

	/**
	 * Durchläuft den angegebenen Wikibaum und ruft für jeden Knoten, der den
	 * passenden Typ hat, die angegebene Callbackmethode auf. Diese Methode ist
	 * nützlich, wenn eine bestimmte Information in einer Wikiseite gesucht
	 * wird.
	 * 
	 * @param pageElement Wurzelknoten des Wikibaums. Nicht null.
	 * @param callback Objekt mit Callbackmethode, die für jeden Knoten des
	 *        passenden Typs aufgerufen wird. Nicht <code>null</code>.
	 * @param clazz Typ der gesuchten Seitenelemente. Ist erforderlich, weil
	 *        Java-Generics zur Laufzeit keine Typinformation haben.
	 * @param recurseIntoMatchingClasses <code>true</code> -> auch alle Klassen
	 *        des gesuchten Typs zerlegen.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends PageElement> void viewPageElements(@NotNull PageElement pageElement, @NotNull PageElementViewer<T> callback, @NotNull Class<T> clazz,
			boolean recurseIntoMatchingClasses) {
		if (clazz.isInstance(pageElement)) {
			// Callbackmethode aufrufen
			callback.viewPageElement((T) pageElement);

			if (!recurseIntoMatchingClasses) {
				return; // Rekursion abbrechen
			}
		}

		// rekursiv den Baum durchlaufen
		if (pageElement instanceof PageElementWithChild) {
			PageElement child = ((PageElementWithChild) pageElement).getChild();
			if (child != null) {
				viewPageElements(child, callback, clazz, recurseIntoMatchingClasses);
			}
		} else if (pageElement instanceof PageElementList) {
			for (PageElement pe : (PageElementList) pageElement) {
				viewPageElements(pe, callback, clazz, recurseIntoMatchingClasses);
			}
		} else if (pageElement instanceof Table) {
			for (TableRow row : ((Table) pageElement).getRows()) {
				for (TableCell cell : row.getCells()) {
					if (cell.getContent() != null) {
						viewPageElements(cell.getContent(), callback, clazz, recurseIntoMatchingClasses);
					}
				}
			}
		}
	}

	/**
	 * Durchläuft den kompletten Wikibaum einer Wikiseite. Für jedes
	 * Seitenelement wird die angegebene Callbackmethode
	 * {@link PageElementTransformer#transformPageElement(PageElement)}
	 * aufgerufen, die die Möglichkeit hat, das Seitenelement durch ein neues zu
	 * ersetzen.
	 * 
	 * @param wikiPage Wikiseite, die transformiert werden soll. Nicht
	 *        <code>null</code>.
	 * @param callback Objekt mit Callbackmethode, die für jeden Knoten
	 *        aufgerufen wird. Nicht <code>null</code>.
	 * @return Transformierte Wikiseite.
	 */
	public static WikiPage transformPageElements(WikiPage wikiPage, PageElementTransformer callback) {
		// Wiki-Baum traversieren
		PageElement pe = transformPageElement(wikiPage, callback);

		if (pe instanceof WikiPage) {
			return (WikiPage) pe;
		} else {
			return new WikiPage(wikiPage.getPagePath(), pe, wikiPage.getFromPos(), wikiPage.getToPos());
		}
	}

	/**
	 * Zerlegt das angegebene Seitenelement.
	 */
	private static PageElement transformPageElement(PageElement pageElement, PageElementTransformer callback) {
		// Seitenelement durch Callbackmethode transformieren
		if (!(pageElement instanceof PageElementList)) {
			pageElement = callback.transformPageElement(pageElement);
		}

		// transformiertes Seitenelement weiter zerlegen
		if (pageElement instanceof PageElementWithChild) {
			return transformPageElementWithChild(((PageElementWithChild) pageElement), callback);
		} else if (pageElement instanceof PageElementList) {
			return transformPageElementList((PageElementList) pageElement, callback);
		} else if (pageElement instanceof Table) {
			return transformTable((Table) pageElement, callback);
		} else {
			// null / ohne Kindelemente / unbekannter Typ
			return pageElement;
		}
	}

	/**
	 * Transformiert eine Liste von Seitenelementen.<br>
	 * <br>
	 * Veränderte Listeneinträge werden ausgetauscht. Wenn ein Listenelement zu
	 * <code>null</code> transformiert wird, wird es aus der Liste gelöscht.
	 */
	@Nullable
	private static PageElementList transformPageElementList(PageElementList pageElementList, PageElementTransformer callback) {
		if (pageElementList == null) {
			return null;
		}

		int i = 0;
		while (i < pageElementList.size()) {
			// Listeneintrag transformieren
			PageElement pe = pageElementList.get(i);
			pe = transformPageElement(pe, callback);

			// geänderten Eintrag in Liste austauschen bzw. löschen
			if (pe != null) {
				pageElementList.set(i, pe);
				i++;
			} else {
				pageElementList.remove(i);
			}
		}
		return pageElementList;
	}

	/**
	 * Transformiert das Kind-Element eines Seitenelements.<br>
	 * <br>
	 * Wenn das Kind-Element ersetzt wird, wird die Parent-Referenz automatisch
	 * aktualisiert.
	 */
	private static PageElement transformPageElementWithChild(PageElementWithChild pageElement, PageElementTransformer callback) {
		PageElement child = pageElement.getChild();
		child = transformPageElement(child, callback);
		pageElement.setChild(child);
		return pageElement;
	}

	/**
	 * Transformiert die Zelleninhalte einer Tabelle.
	 */
	private static Table transformTable(Table table, PageElementTransformer callback) {
		for (TableRow tableRow : table.getRows()) {
			for (TableCell tableCell : tableRow.getCells()) {
				PageElement pe = transformPageElement(tableCell.getContent(), callback);
				tableCell.setContent(pe);
			}
		}
		return table;
	}

	/**
	 * Sucht den lokalen bzw. globalen Kontext zu einem Knoten im Wikibaum. Der
	 * lokale Kontext ist die Wikiseite, die das Seitenelement enthält. Der
	 * globale Kontext ist die Wurzel des Baums. Der globale und lokale Kontext
	 * zu einem Knoten unterscheiden sich nur, wenn eine Wikiseite per
	 * <tt>includepage</tt> in eine andere eingebettet wird.
	 * 
	 * @param pageElement Der Knoten im Wikibaum, zu dem der globale/lokale
	 *        Kontext gesucht werden soll.
	 * @param globalContext <code>true</code> -> globalen Kontext suchen,
	 *        <code>false</code> -> lokalen Kontext suchen.
	 * @return Der Kontext zum Knoten. <code>null</code> -> keinen Kontext
	 *         gefunden. Sollte eigentlich nicht passieren, da jeder Wikibaum
	 *         ein WikiPage-Element als Wurzel haben sollte.
	 */
	@Nullable
	public static WikiPage getContextWikiPage(PageElement pageElement, boolean globalContext) {
		while (pageElement != null) {
			// Kontext gefunden?
			if (pageElement instanceof WikiPage && (!globalContext || pageElement.getParent() == null)) {
				return (WikiPage) pageElement;
			}

			pageElement = pageElement.getParent();
		}

		// Keinen gültigen Kontext gefunden.
		return null;
	}

	/**
	 * Wandelt den angegebenen Pfad einer Wikiseite in einen absoluten Pfad um.
	 * 
	 * @param pagePath Pfad im Repository. Relativ oder absolut.
	 *        <code>null</code> -> aktuelle Wikiseite verwenden.
	 * @param contextWikiPage Die Kontext-Wikiseite. Nicht <code>null</code>.
	 * @return Absoluter Pfad. Nicht <code>null</code>.
	 */
	public static String getAbsolutePagePath(String pagePath, WikiPage contextWikiPage) {
		if (pagePath != null) {
			return PathUtils.makeWebPathAbsolute(pagePath, PathUtils.extractWebFolder(contextWikiPage.getPagePath()));
		} else {
			return contextWikiPage.getPagePath();
		}
	}

	/**
	 * Gibt den reinen Textanteil im angegebenen Teilbaum zurück. Das sind alle
	 * TextOnly-Inhalte aneinander gekettet.
	 * 
	 * @param pageElement Teilbaum, aus dem der Text extrahiert werden soll.
	 * @return Enthaltener reiner Text. Nicht <code>null</code>.
	 */
	public static String getStringContent(PageElement pageElement) {
		StringContentCollector scc = new StringContentCollector();
		viewPageElements(pageElement, scc, TextOnly.class, true);
		return scc.getStringContent();
	}

	/**
	 * Entfernt aus einem beliebigen String alle Sonderzeichen, damit ein String
	 * entsteht, der als HTML-id verwendet werden kann.
	 * 
	 * @param s Beliebiger String. Nicht <code>null</code>.
	 * @return id String. Nicht <code>null</code>.
	 */
	public static String getIdString(String s) {
		StringBuilder sb = new StringBuilder();
		boolean letterPrefix = false;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_') {
				letterPrefix = true;
			}
			if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || letterPrefix
					&& ((ch >= '0' && ch <= '9') || ch == '-' || ch == '.' || ch == ':')) {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * Hilfsklasse zum Aufsammeln aller reinen Textanteile in einem Teilbaum.
	 */
	private static class StringContentCollector implements PageElementViewer<TextOnly> {
		@NotNull
		private final StringBuilder sb;

		public StringContentCollector() {
			sb = new StringBuilder();
		}

		public void viewPageElement(@NotNull TextOnly textOnly) {
			sb.append(textOnly.getText());
		}

		@NotNull
		public String getStringContent() {
			return sb.toString();
		}
	}
}
