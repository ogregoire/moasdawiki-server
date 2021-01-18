/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
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

package net.moasdawiki.service.handler;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.HttpResponse;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.search.SearchQuery;
import net.moasdawiki.service.search.SearchResult;
import net.moasdawiki.service.search.SearchResult.Marker;
import net.moasdawiki.service.search.SearchResult.MatchingLine;
import net.moasdawiki.service.search.SearchResult.PageDetails;
import net.moasdawiki.service.search.SearchService;
import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.*;
import org.jetbrains.annotations.NotNull;

/**
 * Einfache Volltextsuche in allen Wikiseiten im Repository.
 */
public class SearchHandler {

	private static final String ERROR_KEY = "SearchHandler.error";
	private static final String SUMMARY_ONE_KEY = "SearchHandler.summary.one";
	private static final String SUMMARY_MANY_KEY = "SearchHandler.summary.more";
	private static final String TITLE_KEY = "SearchHandler.title";

	private final Logger logger;
	private final Settings settings;
	private final Messages messages;
	private final WikiService wikiService;
	private final SearchService searchService;
	private final HtmlService htmlService;

	/**
	 * Constructor.
	 */
	public SearchHandler(@NotNull Logger logger, @NotNull Settings settings, @NotNull Messages messages,
						 @NotNull WikiService wikiService, @NotNull SearchService searchService,
						 @NotNull HtmlService htmlService) {
		this.logger = logger;
		this.settings = settings;
		this.messages = messages;
		this.wikiService = wikiService;
		this.searchService = searchService;
		this.htmlService = htmlService;
	}

	@NotNull
	public HttpResponse handleSearchRequest(@NotNull String query) {
		SearchQuery searchQuery = SearchService.parseQueryString(query);
		try {
			SearchResult searchResult = searchService.searchInRepository(searchQuery);
			WikiPage wikiPage = generateSearchResultPage(searchResult);
			wikiPage = WikiHelper.extendWikiPage(wikiPage, true, false, false,
					logger, settings, wikiService);
			return htmlService.convertPage(wikiPage);
		} catch (ServiceException e) {
			return htmlService.generateErrorPage(500, e, ERROR_KEY);
		}
	}

	@NotNull
	private WikiPage generateSearchResultPage(@NotNull SearchResult searchResult) {
		PageElementList pageContent = new PageElementList();

		// Seitenname ausgeben
		String pageTitle = messages.getMessage(TITLE_KEY, searchResult.getSearchQuery().getQueryString());
		pageContent.add(new Heading(1, new TextOnly(pageTitle), null, null));

		// Anzahl Suchergebnisse ausgeben
		int count = searchResult.getResultList().size();
		String countText;
		if (count == 1) {
			countText = messages.getMessage(SUMMARY_ONE_KEY);
		} else {
			countText = messages.getMessage(SUMMARY_MANY_KEY, count);
		}
		pageContent.add(new TextOnly(countText));

		// Suchergebnisse ausgeben
		for (PageDetails pageDetails : searchResult.getResultList()) {
			PageElementList formattedpageDetails = generate(pageDetails);
			pageContent.add(new ListItem(1, false, formattedpageDetails, null, null));
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
