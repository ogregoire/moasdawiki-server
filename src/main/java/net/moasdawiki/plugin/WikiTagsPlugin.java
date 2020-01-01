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

package net.moasdawiki.plugin;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.wiki.*;
import net.moasdawiki.service.wiki.structure.*;
import net.moasdawiki.util.DateUtils;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Collator;
import java.util.*;

/**
 * Ersetzt spezielle Wiki-Elemente durch normalen Text.<br>
 * <br>
 * Dieses Plugin sollte als letztes Transformations-Plugin aufgerufen werden,
 * damit alle anderen Plugins volle Unterstützung für Wiki-Tags erhalten.
 * 
 * @author Herbert Reiter
 */
public class WikiTagsPlugin implements Plugin, PageElementTransformer {

	private Logger logger;
	private Settings settings;
	private Messages messages;
	private WikiService wikiService;

	public void setServiceLocator(@NotNull ServiceLocator serviceLocator) {
		this.logger = serviceLocator.getLogger();
		this.settings = serviceLocator.getSettings();
		this.messages = serviceLocator.getMessages();
		this.wikiService = serviceLocator.getWikiService();
	}

	@NotNull
	@CallOrder(10)
	public WikiPage transformWikiPage(@NotNull WikiPage wikiPage) {
		return WikiHelper.transformPageElements(wikiPage, this);
	}

	public PageElement transformPageElement(@NotNull PageElement pageElement) {
		if (pageElement instanceof TableOfContents) {
			return transform((TableOfContents) pageElement);
		} else if (pageElement instanceof Parent) {
			return transform((Parent) pageElement);
		} else if (pageElement instanceof WikiVersion) {
			return transform((WikiVersion) pageElement);
		} else if (pageElement instanceof DateTime) {
			return transform((DateTime) pageElement);
		} else if (pageElement instanceof PageName) {
			return transform((PageName) pageElement);
		} else if (pageElement instanceof PageTimestamp) {
			return transform((PageTimestamp) pageElement);
		} else if (pageElement instanceof ListViewHistory) {
			return transform((ListViewHistory) pageElement);
		} else if (pageElement instanceof ListEditHistory) {
			return transform((ListEditHistory) pageElement);
		} else if (pageElement instanceof ListParents) {
			return transform((ListParents) pageElement);
		} else if (pageElement instanceof ListChildren) {
			return transform((ListChildren) pageElement);
		} else if (pageElement instanceof ListPages) {
			return transform((ListPages) pageElement);
		} else if (pageElement instanceof ListWantedPages) {
			return transform((ListWantedPages) pageElement);
		} else if (pageElement instanceof ListUnlinkedPages) {
			return transform((ListUnlinkedPages) pageElement);
		} else {
			return pageElement; // ansonsten unverändert lassen
		}
	}

	/**
	 * Erzeugt ein Inhaltsverzeichnis dieser Seite. Berücksichtigt werden nur
	 * Überschriften mit Level 1 bis 3 auf oberster Listenebene.
	 */
	@NotNull
	private PageElement transform(@NotNull TableOfContents tableOfContents) {
		// Kontext suchen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(tableOfContents, false);
		if (wikiPage == null) {
			return new PageElementList();
		}

		// Überschriften suchen
		HeadingCollector headingCollector = new HeadingCollector();
		WikiHelper.viewPageElements(wikiPage, headingCollector, Heading.class, false);

		// Inhaltsverzeichnis generieren
		PageElementList pageElementList = new PageElementList();
		int[] levelCounter = { 0, 0, 0 };
		for (Heading heading : headingCollector.getHeadingList()) {
			if (heading.getLevel() > 3) {
				continue;
			}

			// Nummerierung aktualisieren
			levelCounter[heading.getLevel() - 1]++; // Zähler für aktuelle Ebene
			// erhöhen
			for (int i = heading.getLevel() + 1; i <= 3; i++) {
				levelCounter[i - 1] = 0; // Zähler höherer Ebenen zurücksetzen
			}

			// Nummerierung zusammensetzen
			String nummerierung = "";
			for (int i = 1; i <= heading.getLevel(); i++) {
				//noinspection StringConcatenationInLoop
				nummerierung += levelCounter[i - 1] + ".";
			}

			// Rückgabe erstellen
			PageElementList text = new PageElementList();
			text.add(new TextOnly(nummerierung + ' '));
			if (heading.getChild() != null) {
				text.add(heading.getChild());
			}
			String contentString = WikiHelper.getStringContent(heading);
			PageElement tocLine = new LinkPage(null, WikiHelper.getIdString(contentString), text, null, null);
			pageElementList.add(new Paragraph(false, heading.getLevel(), false, tocLine, null, null));
		}

		return pageElementList;
	}

	@SuppressWarnings("SameReturnValue")
	@Nullable
	private PageElement transform(@SuppressWarnings("unused") @NotNull Parent parent) {
		// Element löschen, dient nur zur logischen Strukturierung
		return null;
	}

	@NotNull
	private PageElement transform(@SuppressWarnings("unused") @NotNull WikiVersion wikiVersion) {
		return new TextOnly(settings.getProgramNameVersion());
	}

	@NotNull
	private PageElement transform(@NotNull DateTime dateTime) {
		String dateTimeFormat;
		switch (dateTime.getFormat()) {
			case SHOW_TIME:
				dateTimeFormat = messages.getMessage("WikiTagsPlugin.dateformat.time");
				return new TextOnly(DateUtils.formatDate(settings.getActualTime(), dateTimeFormat));
			case SHOW_DATETIME:
				dateTimeFormat = messages.getMessage("WikiTagsPlugin.dateformat.datetime");
				return new TextOnly(DateUtils.formatDate(settings.getActualTime(), dateTimeFormat));
			case SHOW_DATE:
			default:
				dateTimeFormat = messages.getMessage("WikiTagsPlugin.dateformat.date");
				return new TextOnly(DateUtils.formatDate(settings.getActualTime(), dateTimeFormat));
		}
	}

	@Nullable
	private PageElement transform(@NotNull PageName pageName) {
		// Kontext suchen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(pageName, pageName.isGlobalContext());
		if (wikiPage == null) {
			return null;
		}
		String pagePath = wikiPage.getPagePath();
		if (pagePath == null) {
			return null;
		}

		if (pageName.getPageNameFormat() == Listable.PageNameFormat.PAGE_TITLE) {
			String pageTitle = PathUtils.extractWebName(pagePath);
			if (pageName.isLinked()) {
				return new LinkPage(pagePath, new TextOnly(pageTitle));
			} else {
				return new TextOnly(pageTitle);
			}
		} else if (pageName.getPageNameFormat() == Listable.PageNameFormat.PAGE_FOLDER) {
			String pageFolder = PathUtils.extractWebFolder(pagePath);
			if (pageName.isLinked()) {
				return generateStepwiseLinks(pageFolder);
			} else {
				return new TextOnly(pageFolder);
			}
		} else {
			if (pageName.isLinked()) {
				return generateStepwiseLinks(pagePath);
			} else {
				return new TextOnly(pagePath);
			}
		}
	}

	/**
	 * Verlinkt die einzelnen Ordnerbestandteile in einem Pfad.
	 */
	@NotNull
	private PageElement generateStepwiseLinks(@NotNull String path) {
		PageElementList result = new PageElementList();

		path = PathUtils.makeWebPathAbsolute(path, null);
		int leftPos = 0;
		while (leftPos < path.length()) {
			// nächsten Abschnitt ermitteln
			int rightPos = path.indexOf('/', leftPos);
			if (rightPos < 0) {
				rightPos = path.length() - 1; // letzter Abschnitt
			}

			// Name und Pfad des Abschnitts bestimmen,
			// inkl. abschließendem '/', falls vorhanden
			String partName = path.substring(leftPos, rightPos + 1);
			String partPath = path.substring(0, rightPos + 1);

			// Link erzeugen
			if (leftPos > 0) {
				result.add(new TextOnly(" "));
			}
			result.add(new LinkPage(partPath, new TextOnly(partName)));

			leftPos = rightPos + 1;
		}

		return result;
	}

	@Nullable
	private PageElement transform(@NotNull PageTimestamp pageTimestamp) {
		// Kontext suchen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(pageTimestamp, pageTimestamp.isGlobalContext());
		if (wikiPage == null || wikiPage.getPagePath() == null) {
			return null;
		}

		try {
			WikiFile wikiFile = wikiService.getWikiFile(wikiPage.getPagePath());
			if (wikiFile.getRepositoryFile().getContentTimestamp() != null) {
				String dateTimeFormat = messages.getMessage("WikiTagsPlugin.dateformat.datetime");
				return new TextOnly(DateUtils.formatDate(wikiFile.getRepositoryFile().getContentTimestamp(), dateTimeFormat));
			} else {
				// keine Ausgabe
				return null;
			}
		}
		catch (ServiceException e) {
			logger.write("Error reading wiki file to show page timestamp", e);
			return null;
		}
	}

	@Nullable
	private PageElement transform(@NotNull ListViewHistory listViewHistory) {
		List<String> history = wikiService.getLastViewedWikiFiles(listViewHistory.getMaxLength());
		return generateListOfPageLinks(history, listViewHistory);
	}

	@Nullable
	private PageElement transform(@NotNull ListEditHistory listEditHistory) {
		List<String> history = wikiService.getLastModifiedWikiFiles(listEditHistory.getMaxLength());
		return generateListOfPageLinks(history, listEditHistory);
	}

	@Nullable
	private PageElement transform(@NotNull ListParents listParents) {
		// Kontext suchen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(listParents, listParents.isGlobalContext());

		// absoluten Pfad bestimmen
		String pagePath = listParents.getPagePath();
		pagePath = WikiHelper.getAbsolutePagePath(pagePath, wikiPage);

		// Vaterseiten bestimmen + sortieren
		try {
			WikiFile wikiFile = wikiService.getWikiFile(pagePath);
			List<String> list = new ArrayList<>(wikiFile.getParents());
			list.sort(Collator.getInstance(Locale.GERMAN));
			return generateListOfPageLinks(list, listParents);
		}
		catch (ServiceException e) {
			// es handelt sich um eine künstliche Seite
			logger.write("Error reading wiki page to list parents", e);
			return null;
		}
	}

	@Nullable
	private PageElement transform(@NotNull ListChildren listChildren) {
		// Kontext suchen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(listChildren, listChildren.isGlobalContext());

		// absoluten Pfad bestimmen
		String pagePath = listChildren.getPagePath();
		pagePath = WikiHelper.getAbsolutePagePath(pagePath, wikiPage);

		// Kindseiten bestimmen + sortieren
		try {
			WikiFile wikiFile = wikiService.getWikiFile(pagePath);
			List<String> list = new ArrayList<>(wikiFile.getChildren());
			list.sort(Collator.getInstance(Locale.GERMAN));
			return generateListOfPageLinks(list, listChildren);
		}
		catch (ServiceException e) {
			// es handelt sich um eine künstliche Seite
			logger.write("Error reading wiki page to list children", e);
			return null;
		}
	}

	@Nullable
	private PageElement transform(@NotNull ListPages listPages) {
		// Kontext suchen
		WikiPage wikiPage = WikiHelper.getContextWikiPage(listPages, listPages.isGlobalContext());
		if (wikiPage == null) {
			return null;
		}

		// absoluten Pfad bestimmen
		String folder = listPages.getFolder();
		if (folder == null) {
			folder = PathUtils.extractWebFolder(wikiPage.getPagePath());
		}
		folder = WikiHelper.getAbsolutePagePath(folder, wikiPage);

		// Liste aller Seitennamen holen
		Set<String> allPages = wikiService.getWikiFilePaths();

		// Liste filtern
		List<String> list = new ArrayList<>(allPages.size());
		for (String pagePath : allPages) {
			if (folder == null || PathUtils.extractWebFolder(pagePath).startsWith(folder)) {
				list.add(pagePath);
			}
		}
		list.sort(Collator.getInstance(Locale.GERMAN));
		return generateListOfPageLinks(list, listPages);
	}

	@Nullable
	private PageElement transform(@NotNull ListWantedPages listWantedPages) {
		Set<String> allPagePaths = wikiService.getWikiFilePaths();

		// intakte Links entfernen
		Set<String> allPageLinks = extractAllPageLinks(allPagePaths);
		allPageLinks.removeAll(allPagePaths);

		// Links auf Indexseiten entfernen
		allPageLinks.removeIf(pagePath -> pagePath.endsWith("/"));

		// Liste sortieren
		List<String> wantedPagePathsList = new ArrayList<>(allPageLinks);
		wantedPagePathsList.sort(Collator.getInstance(Locale.GERMAN));

		return generateListOfPageLinks(wantedPagePathsList, listWantedPages);
	}

	@Nullable
	private PageElement transform(@NotNull ListUnlinkedPages listUnlinkedPages) {
		Set<String> allPagePaths = wikiService.getWikiFilePaths();

		// verlinkte Seiten entfernen
		Set<String> result = new HashSet<>(allPagePaths);
		Set<String> allPageLinks = extractAllPageLinks(allPagePaths);
		String indexPageName = settings.getIndexPageName();
		for (String pagePath : allPageLinks) {
			// Indexseiten können implizit verlinkt sein -> auflösen
			if (pagePath.endsWith("/") && indexPageName != null) {
				result.remove(pagePath + indexPageName);
			} else {
				result.remove(pagePath);
			}
		}

		// Vater- und Kindseiten als verlinkt betrachten und entfernen
		for (String pagePath : allPagePaths) {
			try {
				WikiFile wikiFile = wikiService.getWikiFile(pagePath);
				if (listUnlinkedPages.isHideParents()) {
					result.removeAll(wikiFile.getParents());
				}
				if (listUnlinkedPages.isHideChildren()) {
					result.removeAll(wikiFile.getChildren());
				}
			}
			catch (ServiceException e) {
				logger.write("Error reading wiki file to get parents and children, ignoring it", e);
			}
		}

		// Startseite als implizit verlinkte Seiten entfernen
		result.remove(settings.getStartpagePath());

		// Liste sortieren
		List<String> unlinkedPagePathsList = new ArrayList<>(result);
		unlinkedPagePathsList.sort(Collator.getInstance(Locale.GERMAN));

		return generateListOfPageLinks(unlinkedPagePathsList, listUnlinkedPages);
	}

	/**
	 * Durchkämmt die angegebene Liste von Wikiseiten und sammelt alle Links auf
	 * Wikiseiten.
	 * 
	 * @return Liste aller Links auf Wikiseiten. Nicht null.
	 */
	@NotNull
	private Set<String> extractAllPageLinks(@NotNull Set<String> pagePaths) {
		LinkPageCollector linkPageCollector = new LinkPageCollector();
		for (String pagePath : pagePaths) {
			try {
				WikiFile wikiFile = wikiService.getWikiFile(pagePath);
				WikiHelper.viewPageElements(wikiFile.getWikiPage(), linkPageCollector, LinkPage.class, true);
			}
			catch (ServiceException e) {
				logger.write("Error reading wiki page to scan for links, ignoring it", e);
			}
		}
		return linkPageCollector.getLinkedPagePaths();
	}

	/**
	 * Gibt eine Liste von Wiki-Seiten aus. Die Einträge sind verlinkt, so dass
	 * ein Klick darauf direkt zur Wiki-Seite führt.
	 */
	@Nullable
	private PageElement generateListOfPageLinks(@NotNull List<String> pagePaths, @NotNull Listable listoptions) {
		// Liste leer? -> Text für diesen Fall ausgeben
		if (pagePaths.isEmpty()) {
			if (listoptions.getOutputOnEmpty() != null) {
				return new TextOnly(listoptions.getOutputOnEmpty());
			} else {
				return null;
			}
		}

		else {
			PageElementList pageElementList = new PageElementList();
			for (int i = 0; i < pagePaths.size(); i++) {
				String pagePath = pagePaths.get(i);

				// Link zusammenbauen
				String pageName;
				switch (listoptions.getPageNameFormat()) {
				case PAGE_FOLDER:
					pageName = PathUtils.extractWebFolder(pagePath);
					break;
				case PAGE_TITLE:
					pageName = PathUtils.extractWebName(pagePath);
					break;
				default:
					pageName = pagePath;
				}
				LinkPage linkPage = new LinkPage(pagePath, new TextOnly(pageName));

				// Listeneintrag ausgeben
				if (listoptions.isShowInline()) {
					// Listenelemente in einer Zeile ausgeben
					if (i > 0 && listoptions.getInlineListSeparator() != null) {
						pageElementList.add(new TextOnly(listoptions.getInlineListSeparator()));
					}
					pageElementList.add(linkPage);
				} else {
					// Listenelemente als Aufzählung ausgeben
					pageElementList.add(new UnorderedListItem(1, linkPage, null, null));
				}
			}
			return pageElementList;
		}
	}

	/**
	 * Hilfsklasse zum Sammeln aller Überschriften.
	 */
	private static class HeadingCollector implements PageElementViewer<Heading> {

		@NotNull
		private final List<Heading> headingList = new ArrayList<>();

		public void viewPageElement(@NotNull Heading heading) {
			headingList.add(heading);
		}

		@NotNull
		public List<Heading> getHeadingList() {
			return headingList;
		}
	}

	/**
	 * Hilfsklasse zum Sammeln aller Links auf Wikiseiten.
	 */
	private static class LinkPageCollector implements PageElementViewer<LinkPage> {

		@NotNull
		private final Set<String> linkedPagePaths = new HashSet<>();

		public void viewPageElement(@NotNull LinkPage linkPage) {
			WikiPage contextWikiPage = WikiHelper.getContextWikiPage(linkPage, false);
			String absolutePagePath = WikiHelper.getAbsolutePagePath(linkPage.getPagePath(), contextWikiPage);
			linkedPagePaths.add(absolutePagePath);
		}

		@NotNull
		public Set<String> getLinkedPagePaths() {
			return linkedPagePaths;
		}
	}
}
