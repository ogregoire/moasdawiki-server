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

import java.util.ArrayList;
import java.util.List;

/**
 * DTO mit dem Suchergebnis. Die Liste ist absteigend nach Relevanz sortiert.
 * 
 * @author Herbert Reiter
 */
public class SearchResult {
	/**
	 * Suchbedingungen, die den Suchergebnissen zugrunde liegen.
	 */
	public SearchQuery searchQuery;

	/**
	 * Trefferliste.
	 */
	public final List<PageDetails> resultList = new ArrayList<>();

	/**
	 * Enthält die Suchergebnisse für eine einzelne Wiki-Seite. Als Suchergebnis
	 * werden die Absätze eingetragen, die den Suchstring enthalten.
	 */
	public static class PageDetails {
		/**
		 * Wikiseite. Nicht <code>null</code>.
		 */
		public String pagePath;

		/**
		 * Treffer im Seitennamen der Wikiseite. Nicht <code>null</code>.
		 */
		public final MatchingLine titleLine = new MatchingLine();

		/**
		 * Trefferzeilen im Text der Wikiseite. Nicht <code>null</code>.
		 */
		public final List<MatchingLine> textLines = new ArrayList<>();

		/**
		 * Trefferrelevanz. Positive Zahl.
		 */
		public int relevance;
	}

	/**
	 * Enthält eine Zeile einer Wikiseite mit einem Suchtreffer.
	 */
	public static class MatchingLine {
		/**
		 * Textzeile, in der mindestens eine Fundstelle enthalten ist. Nicht
		 * <code>null</code>.
		 */
		public String line;

		/**
		 * Fundstellen in der Textzeile. Nicht <code>null</code>, hat i.d.R.
		 * mindestens einen Eintrag, muss aber nicht (z.B. im Titel). Die
		 * Einträge sind in aufsteigender Positon sortiert.
		 */
		public final List<Marker> positions = new ArrayList<>();
	}

	/**
	 * Gibt an, wo in einem String der gefundene Text ist.
	 */
	public static class Marker {
		/**
		 * Index des ersten Zeichen.
		 */
		public final int from;
		
		/**
		 * Index hinter dem letzten Zeichen.
		 */
		public final int to;

		public Marker(int from, int to) {
			super();
			this.from = from;
			this.to = to;
		}
	}
}
