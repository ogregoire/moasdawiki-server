/*
 * Copyright (c) 2008 - 2019 Dr. Herbert Reiter (support@moasdawiki.net)
 * 
 * This file is part of MoasdaWiki.
 * 
 * MoasdaWiki is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * MoasdaWiki is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MoasdaWiki. If not, see <http://www.gnu.org/licenses/>.
 */

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;

/**
 * Repräsentiert das aktuelle Datum und die aktuelle Uhrzeit.
 * 
 * @author Herbert Reiter
 */
public class DateTime extends PageElement {

	/**
	 * Format, in dem der Zeitstempel dargestellt werden soll.
	 */
	@NotNull
	private final Format format;

	public DateTime(@NotNull Format format, Integer fromPos, Integer toPos) {
		super();
		this.format = format;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Gibt das Ausgabeformat zurück.
	 */
	@NotNull
	public Format getFormat() {
		return format;
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new DateTime(format, fromPos, toPos);
	}

	/**
	 * Ausgabeformat.
	 */
	public enum Format {
		SHOW_DATE, SHOW_TIME, SHOW_DATETIME
	}
}
