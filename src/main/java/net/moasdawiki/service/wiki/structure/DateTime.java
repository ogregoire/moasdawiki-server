/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;

/**
 * Repräsentiert das aktuelle Datum und die aktuelle Uhrzeit.
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
