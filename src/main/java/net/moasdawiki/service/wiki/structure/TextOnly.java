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

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enthält reinen Text ohne weiter zu interpretierende Elemente.
 */
public class TextOnly extends PageElement {

	/**
	 * Darzustellender Text.
	 */
	@NotNull
	private final String text;

	/**
	 * Konstruktor.
	 * 
	 * @param text darf nicht null sein
	 */
	public TextOnly(@NotNull String text) {
		this(text, null, null);
	}

	/**
	 * Konstruktor.
	 * 
	 * @param text darf nicht null sein
	 */
	public TextOnly(@NotNull String text, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.text = text;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@NotNull
	public String getText() {
		return text;
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new TextOnly(text, fromPos, toPos);
	}
}
