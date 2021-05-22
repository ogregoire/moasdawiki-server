/*
 * MoasdaWiki Server
 *
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3 as
 * published by the Free Software Foundation (AGPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/agpl-3.0.html>.
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
