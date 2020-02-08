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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO mit dem Suchergebnis.
 * Die Liste ist absteigend nach Relevanz sortiert.
 * 
 * @author Herbert Reiter
 */
public class SearchResult {

	/**
	 * Suchbedingungen, die den Suchergebnissen zugrunde liegen.
	 */
	@NotNull
	private final SearchQuery searchQuery;

	/**
	 * Trefferliste.
	 */
	@NotNull
	private final List<PageDetails> resultList;

	public SearchResult(@NotNull SearchQuery searchQuery) {
		this.searchQuery = searchQuery;
		this.resultList = new ArrayList<>();
	}

	@NotNull
	public SearchQuery getSearchQuery() {
		return searchQuery;
	}

	@NotNull
	public List<PageDetails> getResultList() {
		return resultList;
	}

	/**
	 * Enthält die Suchergebnisse für eine einzelne Wiki-Seite.
	 * Als Suchergebnis werden die Absätze eingetragen, die den Suchstring enthalten.
	 */
	public static class PageDetails {

		/**
		 * Wikiseite. Nicht <code>null</code>.
		 */
		@NotNull
		private final String pagePath;

		/**
		 * Treffer im Seitennamen der Wikiseite.
		 */
		@NotNull
		private final MatchingLine titleLine;

		/**
		 * Trefferzeilen im Text der Wikiseite.
		 */
		@NotNull
		private final List<MatchingLine> textLines;

		/**
		 * Trefferrelevanz. Positive Zahl.
		 */
		public final int relevance;

		public PageDetails(@NotNull String pagePath, @NotNull MatchingLine titleLine, @NotNull List<MatchingLine> textLines, int relevance) {
			this.pagePath = pagePath;
			this.titleLine = titleLine;
			this.textLines = textLines;
			this.relevance = relevance;
		}

		@NotNull
		public String getPagePath() {
			return pagePath;
		}

		@NotNull
		public MatchingLine getTitleLine() {
			return titleLine;
		}

		@NotNull
		public List<MatchingLine> getTextLines() {
			return textLines;
		}

		public int getRelevance() {
			return relevance;
		}
	}

	/**
	 * Enthält eine Zeile einer Wikiseite mit einem Suchtreffer.
	 */
	public static class MatchingLine {

		/**
		 * Textzeile, in der mindestens eine Fundstelle enthalten ist.
		 */
		@NotNull
		private final String line;

		/**
		 * Fundstellen in der Textzeile.
		 * Hat i.d.R. mindestens einen Eintrag, muss aber nicht (z.B. im Titel).
		 * Die Einträge sind in aufsteigender Positon sortiert.
		 */
		@NotNull
		private final List<Marker> positions;

		public MatchingLine(@NotNull String line) {
			this.line = line;
			this.positions = new ArrayList<>();
		}

		@NotNull
		public String getLine() {
			return line;
		}

		@NotNull
		public List<Marker> getPositions() {
			return positions;
		}
	}

	/**
	 * Gibt an, wo in einem String der gefundene Text ist.
	 */
	public static class Marker {

		/**
		 * Index des ersten Zeichen.
		 */
		private final int from;
		
		/**
		 * Index hinter dem letzten Zeichen.
		 */
		private final int to;

		public Marker(int from, int to) {
			super();
			this.from = from;
			this.to = to;
		}

		public int getFrom() {
			return from;
		}

		public int getTo() {
			return to;
		}
	}
}
