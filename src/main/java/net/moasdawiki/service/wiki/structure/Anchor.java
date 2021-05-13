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
