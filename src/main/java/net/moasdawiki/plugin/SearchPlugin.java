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
import net.moasdawiki.service.wiki.structure.*;
import net.moasdawiki.util.EscapeUtils;
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
		String query = request.urlParameters.get("text");
		boolean scanWikiText = Boolean.parseBoolean(request.urlParameters.get("scanWikiText"));
		SearchQuery searchQuery = searchService.parseQueryString(query);
		try {
			SearchResult searchResult = searchService.searchInRepository(searchQuery, scanWikiText);
			WikiPage wikiPage = generateSearchResultPage(searchResult, scanWikiText);
			wikiPage = WikiHelper.extendWikiPage(wikiPage, true, false, false, serviceLocator);
			return htmlService.convertPage(wikiPage);
		} catch (ServiceException e) {
			return htmlService.generateErrorPage(500, e, "SearchPlugin.error");
		}
	}

	@NotNull
	private WikiPage generateSearchResultPage(@NotNull SearchResult searchResult, boolean scanWikiText) {
		PageElementList pageContent = new PageElementList();

		// Seitenname ausgeben
		String pageTitle = messages.getMessage("SearchPlugin.title", searchResult.getSearchQuery().getQueryString());
		pageContent.add(new Heading(1, new TextOnly(pageTitle), null, null));

		if (!scanWikiText) {
			String url = "/search/?text=" + EscapeUtils.encodeUrlParameter(searchResult.getSearchQuery().getQueryString()) + "&scanWikiText=true";
			String extendedSearch = messages.getMessage("SearchPlugin.includeWikiText");
			LinkExternal linkExternal = new LinkExternal(url, new TextOnly(extendedSearch), null, null);
			pageContent.add(new Paragraph(false, 0, false,  linkExternal, null, null));
		}

		// Anzahl Suchergebnisse ausgeben
		int count = searchResult.getResultList().size();
		String countText;
		if (count == 1) {
			countText = messages.getMessage("SearchPlugin.summary.one");
		} else {
			countText = messages.getMessage("SearchPlugin.summary.more", count);
		}
		pageContent.add(new TextOnly(countText));

		// Suchergebnisse ausgeben
		for (PageDetails pageDetails : searchResult.getResultList()) {
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
		PageElementList pageName = highlightMatching(pageDetails.getTitleLine());
		pageElementList.add(new LinkPage(pageDetails.getPagePath(), pageName));
		pageElementList.add(new LineBreak());

		// Treffer im Seitentext anzeigen
		for (int i = 0; i < pageDetails.getTextLines().size() && i < 5; i++) {
			MatchingLine matchingLine = pageDetails.getTextLines().get(i);
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
		for (Marker marker : matchingLine.getPositions()) {
			// Text vor der Markierung übernehmen
			String textBefore = matchingLine.getLine().substring(lastIndex, marker.getFrom());
			pageElementList.add(new TextOnly(textBefore));

			// Suchstring hervorheben
			String textHighlighted = matchingLine.getLine().substring(marker.getFrom(), marker.getTo());
			pageElementList.add(new Bold(new TextOnly(textHighlighted), null, null));

			// Index nachziehen
			lastIndex = marker.getTo();
		}

		// Rest komplett übernehmen
		String textAfter = matchingLine.getLine().substring(lastIndex);
		pageElementList.add(new TextOnly(textAfter));

		return pageElementList;
	}
}
