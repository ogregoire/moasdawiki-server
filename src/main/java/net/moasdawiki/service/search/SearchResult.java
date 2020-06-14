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

package net.moasdawiki.service.search;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Search result.
 * The list is sorted ascending by relevance.
 *
 * @author Herbert Reiter
 */
public class SearchResult {

	/**
	 * Search query.
	 */
	@NotNull
	private final SearchQuery searchQuery;

	/**
	 * Matching wiki pages.
	 */
	@NotNull
	private final List<PageDetails> resultList;

	/**
	 * Constructor.
	 */
	public SearchResult(@NotNull SearchQuery searchQuery) {
		this.searchQuery = searchQuery;
		this.resultList = new ArrayList<>();
	}

	@Contract(pure = true)
	@NotNull
	public SearchQuery getSearchQuery() {
		return searchQuery;
	}

	@Contract(pure = true)
	@NotNull
	public List<PageDetails> getResultList() {
		return resultList;
	}

	/**
	 * Search result for a single wiki page.
	 * Contains the paragraphs with a matching word.
	 */
	public static class PageDetails {

		/**
		 * Wiki page path.
		 */
		@NotNull
		private final String pagePath;

		/**
		 * Matching words in the page title.
		 */
		@NotNull
		private final MatchingLine titleLine;

		/**
		 * Matching paragraphs in the wiki page content.
		 */
		@NotNull
		private final List<MatchingLine> textLines;

		/**
		 * Matching relevance. Positive integer.
		 */
		public final int relevance;

		/**
		 * Constructor.
		 */
		public PageDetails(@NotNull String pagePath, @NotNull MatchingLine titleLine, @NotNull List<MatchingLine> textLines, int relevance) {
			this.pagePath = pagePath;
			this.titleLine = titleLine;
			this.textLines = textLines;
			this.relevance = relevance;
		}

		@Contract(pure = true)
		@NotNull
		public String getPagePath() {
			return pagePath;
		}

		@Contract(pure = true)
		@NotNull
		public MatchingLine getTitleLine() {
			return titleLine;
		}

		@Contract(pure = true)
		@NotNull
		public List<MatchingLine> getTextLines() {
			return textLines;
		}

		@Contract(pure = true)
		public int getRelevance() {
			return relevance;
		}
	}

	/**
	 * Represents a single paragraph that matches.
	 */
	public static class MatchingLine {

		/**
		 * Paragraph with at least one match.
		 */
		@NotNull
		private final String line;

		/**
		 * Matching text positions.
		 * Can be empty (e.g. for page title).
		 * The entries are ordered by ascending position.
		 */
		@NotNull
		private final List<Marker> positions;

		/**
		 * Constructor.
		 */
		public MatchingLine(@NotNull String line) {
			this.line = line;
			this.positions = new ArrayList<>();
		}

		@Contract(pure = true)
		@NotNull
		public String getLine() {
			return line;
		}

		@Contract(pure = true)
		@NotNull
		public List<Marker> getPositions() {
			return positions;
		}
	}

	/**
	 * Matching position in a paragraph.
	 */
	public static class Marker {

		/**
		 * Index of first character.
		 */
		private final int from;
		
		/**
		 * Index after last character.
		 */
		private final int to;

		/**
		 * Constructor.
		 */
		public Marker(int from, int to) {
			super();
			this.from = from;
			this.to = to;
		}

		@Contract(pure = true)
		public int getFrom() {
			return from;
		}

		@Contract(pure = true)
		public int getTo() {
			return to;
		}
	}
}
