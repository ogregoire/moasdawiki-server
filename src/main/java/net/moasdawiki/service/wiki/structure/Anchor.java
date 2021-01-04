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
 * Enthält einen Anker in der Wiki-Seite. Dieser Anker kann direkt durch Links
 * angesprungen werden.
 */
public class Anchor extends PageElement {
	/**
	 * Name des Ankers. Nicht null.
	 */
	@NotNull
	private final String name;

	public Anchor(@NotNull String name) {
		this(name, null, null);
	}

	public Anchor(@NotNull String name, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.name = name;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Gibt den Namen des Ankers zurück.
	 */
	@NotNull
	public String getName() {
		return name;
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new Anchor(name);
	}
}
