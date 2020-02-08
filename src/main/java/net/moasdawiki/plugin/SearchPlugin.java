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

import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.server.HttpRequest;
import net.moasdawiki.server.HttpResponse;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.search.SearchQuery;
import net.moasdawiki.service.search.SearchResult;
import net.moasdawiki.service.search.SearchService;
import net.moasdawiki.service.search.SearchResult.Marker;
import net.moasdawiki.service.search.SearchResult.MatchingLine;
import net.moasdawiki.service.search.SearchResult.PageDetails;
import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.structure.Bold;
import net.moasdawiki.service.wiki.structure.Heading;
import net.moasdawiki.service.wiki.structure.LineBreak;
import net.moasdawiki.service.wiki.structure.LinkPage;
import net.moasdawiki.service.wiki.structure.PageElementList;
import net.moasdawiki.service.wiki.structure.TextOnly;
import net.moasdawiki.service.wiki.structure.UnorderedListItem;
import net.moasdawiki.service.wiki.structure.VerticalSpace;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Einfache Volltextsuche in allen Wikiseiten im Repository.
 * 
 * @author Herbert Reiter
 */
public class SearchPlugin implements Plugin {

	private ServiceLocator serviceLocator;
	private Messages messages;
	private SearchService searchService;
	private HtmlService htmlService;

	public void setServiceLocator(@NotNull ServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
		this.messages = serviceLocator.getMessages();
		this.searchService = serviceLocator.getSearchService();
		this.htmlService = serviceLocator.getHtmlService();
	}

	@Nullable
	@PathPattern("/search/.*")
	public HttpResponse handleRequest(@NotNull HttpRequest request) {
		try {
			String query = request.urlParameters.get("text");
			SearchQuery searchQuery = searchService.parseQueryString(query);
			SearchResult searchResult = searchService.searchInRepository(searchQuery);
			WikiPage wikiPage = generateSearchResultPage(searchResult);
			wikiPage = WikiHelper.extendWikiPage(wikiPage, true, false, false, serviceLocator);
			return htmlService.convertPage(wikiPage);
		} catch (ServiceException e) {
			return htmlService.generateErrorPage(500, e, "SearchPlugin.error");
		}
	}

	@NotNull
	private WikiPage generateSearchResultPage(@NotNull SearchResult searchResult) {
		PageElementList pageContent = new PageElementList();

		// Seitenname ausgeben
		String pageTitle = messages.getMessage("SearchPlugin.title", searchResult.searchQuery.getQueryString());
		pageContent.add(new Heading(1, new TextOnly(pageTitle), null, null));

		// Anzahl Suchergebnisse ausgeben
		int count = searchResult.resultList.size();
		String countText;
		if (count == 1) {
			countText = messages.getMessage("SearchPlugin.summary.one");
		} else {
			countText = messages.getMessage("SearchPlugin.summary.more", count);
		}
		pageContent.add(new TextOnly(countText));

		// Suchergebnisse ausgeben
		for (PageDetails pageDetails : searchResult.resultList) {
			PageElementList formattedpageDetails = generate(pageDetails);
			pageContent.add(new UnorderedListItem(1, formattedpageDetails, null, null));
		}

		return new WikiPage(null, pageContent, null, null);
	}

	/**
	 * Konvertiert ein einzelnes Suchergebnis in einen Wiki-Teilbaum.
	 */
	@NotNull
	private PageElementList generate(@NotNull PageDetails pageDetails) {
		PageElementList pageElementList = new PageElementList();

		// Seitenname verlinkt anzeigen
		PageElementList pageName = highlightMatching(pageDetails.titleLine);
		pageElementList.add(new LinkPage(pageDetails.pagePath, pageName));
		pageElementList.add(new LineBreak());

		// Treffer im Seitentext anzeigen
		for (int i = 0; i < pageDetails.textLines.size() && i < 5; i++) {
			MatchingLine matchingLine = pageDetails.textLines.get(i);
			PageElementList formattedLine = highlightMatching(matchingLine);
			pageElementList.add(formattedLine);
			pageElementList.add(new LineBreak());
		}

		// Relevanz anzeigen
		String relevanceText = messages.getMessage("SearchPlugin.relevance", pageDetails.relevance);
		pageElementList.add(new TextOnly(relevanceText));
		pageElementList.add(new VerticalSpace());

		return pageElementList;
	}

	/**
	 * Hebt alle Fundstellen des Suchstrings einer Zeile hervor.
	 */
	private static PageElementList highlightMatching(@NotNull MatchingLine matchingLine) {
		PageElementList pageElementList = new PageElementList();

		int lastIndex = 0;
		for (Marker marker : matchingLine.positions) {
			// Text vor der Markierung übernehmen
			String textBefore = matchingLine.line.substring(lastIndex, marker.from);
			pageElementList.add(new TextOnly(textBefore));

			// Suchstring hervorheben
			String textHighlighted = matchingLine.line.substring(marker.from, marker.to);
			pageElementList.add(new Bold(new TextOnly(textHighlighted), null, null));

			// Index nachziehen
			lastIndex = marker.to;
		}

		// Rest komplett übernehmen
		String textAfter = matchingLine.line.substring(lastIndex);
		pageElementList.add(new TextOnly(textAfter));

		return pageElementList;
	}
}
