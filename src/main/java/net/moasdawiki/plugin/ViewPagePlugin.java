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
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.server.HttpRequest;
import net.moasdawiki.server.HttpResponse;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.WikiPage;
import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stellt eine Wikiseite als HTML-Seite dar. Dieses Plugin wird über die URLs
 * <tt>/view/...</tt> und <tt>/</tt> (Root-Pfad) aufgerufen.<br>
 * <br>
 * Ordnerlisting: Endet der Pfad mit <tt>/</tt>, dann wird der Ordner-Index
 * angezeigt. Dieser besteht aus der Liste aller im Ordner enthaltenen
 * Wikiseiten und Unterordner. Zur Darstellung des Ordner-Index wird nach einer
 * Wikiseite mit dem Namen <tt>Index</tt> im Ordner gesucht. Wenn es keine
 * solche Wikiseite gibt, die die Standard-Indexseite verwendet. Dies kann über
 * die Einstellungen <tt>page.index.name</tt> und <tt>page.index.default</tt>
 * konfiguriert werden. Sind beide Einstellungen nicht definiert, ist das
 * Ordnerlisting abgeschaltet.
 * 
 * @author Herbert Reiter
 */
public class ViewPagePlugin implements Plugin {

	private Logger logger;
	private ServiceLocator serviceLocator;
	private Settings settings;
	private WikiService wikiService;
	private HtmlService htmlService;

	public void setServiceLocator(@NotNull ServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
		this.logger = serviceLocator.getLogger();
		this.settings = serviceLocator.getSettings();
		this.wikiService = serviceLocator.getWikiService();
		this.htmlService = serviceLocator.getHtmlService();
	}

	@Nullable
	@PathPattern(multiValue = { "/view/.*", "/" })
	public HttpResponse handleRequest(@NotNull HttpRequest request) {
		// Wikiseite bestimmen
		String filePath;
		String urlPath = request.urlPath;
		if ("/".equals(urlPath)) {
			filePath = settings.getStartpagePath();
		} else if (urlPath.startsWith("/view/")) {
			filePath = urlPath.substring(5);
			filePath = EscapeUtils.url2PagePath(filePath);
		} else {
			// unbekannte URL
			return null;
		}

		// Ordner-Index und Wikiseite unterscheiden
		if (filePath.endsWith("/")) {
			return showFolderIndex(filePath);
		} else {
			return showWikiPage(filePath);
		}
	}

	/**
	 * Die Seiten-URL endet mit '/', daher den Index anzeigen. Wenn in dem
	 * Ordner eine Indexseite (Wikiseite mit Namen "Index") vorhanden ist, wird
	 * diese einfach angezeigt. Falls diese nicht vorhanden ist, wird die
	 * Standard-Indexseite (sofern definiert) angezeigt. Falls das auch nicht
	 * geht, wird ein Fehlertext ausgegeben.
	 * 
	 * @param folderPath Die URL des Ordners.
	 */
	@NotNull
	private HttpResponse showFolderIndex(@NotNull String folderPath) {
		String indexName = settings.getIndexPageName();
		String indexDefaultPagePath = settings.getIndexFallbackPagePath();
		if (indexName == null || indexDefaultPagePath == null) {
			return htmlService.generateErrorPage(403, "ViewPagePlugin.index.disabled");
		}

		// Indexseite für den Ordner vorhanden?
		if (wikiService.existsWikiFile(folderPath + indexName)) {
			// Indexseite für den Ordner wurde angelegt
			// und kann ganz normal angezeigt werden
			return showWikiPage(folderPath + indexName);
		}

		// Globale Indexseitenvorlage vorhanden?
		try {
			WikiFile indexWikiFile = wikiService.getWikiFile(indexDefaultPagePath);

			// künstliche Wikiseite erstellen, die den Ordner repräsentiert
			WikiPage wikiPage = new WikiPage(folderPath + indexName, indexWikiFile.getWikiPage(), null, null);

			// Navigation, Header und Footer hinzufügen
			wikiPage = WikiHelper.extendWikiPage(wikiPage, true, true, true, serviceLocator);

			// Seite ausgeben
			return htmlService.convertPage(wikiPage);
		}
		catch (ServiceException e) {
			logger.write("Error reading default index page, sending 404", e);
			return htmlService.generateErrorPage(404, "ViewPagePlugin.index.notfound", folderPath);
		}
	}

	/**
	 * Zeigt die angegebene Wikiseite an. Wenn die Seite nicht existiert, wird
	 * ein Fehlertext ausgegeben.
	 * 
	 * @param filePath Name der Wikiseite.
	 */
	@NotNull
	private HttpResponse showWikiPage(@NotNull String filePath) {
		// Wikiseite in HTML umwandeln
		try {
			WikiFile wikiFile = wikiService.getWikiFile(filePath);

			// Navigation, Header und Footer hinzufügen
			WikiPage wikiPage = WikiHelper.extendWikiPage(wikiFile.getWikiPage(), true, true, true, serviceLocator);

			// in Liste der zuletzt besuchten Seiten aufnehmen
			if (wikiPage.getPagePath() != null) {
				wikiService.addLastViewedWikiFile(wikiPage.getPagePath());
			}

			// Seite ausgeben
			return htmlService.convertPage(wikiPage);
		}
		catch (ServiceException e) {
			logger.write("Error reading wiki page, sending 404", e);
			return htmlService.generateErrorPage(404, "ViewPagePlugin.page.notfound", filePath);
		}
	}
}
