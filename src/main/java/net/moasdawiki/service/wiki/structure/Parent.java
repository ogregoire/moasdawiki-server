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

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Verweist auf eine Vater-Seite. Dies ist eine logische Information, die eine
 * Beziehung zu einer anderen Wiki-Seite beschreibt. Sie wird normalerweise
 * nicht direkt angezeigt.
 */
public class Parent extends PageElement {
	/**
	 * Name der Vaterseite. Relativ zur Wikiseite oder absolut.
	 */
	@NotNull
	private final String parentPagePath;

	public Parent(@NotNull String parentPagePath, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.parentPagePath = parentPagePath;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@NotNull
	public String getParentPagePath() {
		return parentPagePath;
	}

	/**
	 * Rückgabewert ist egal, weil dieses Objekt sowieso vom Standard-Plugin
	 * entfernt wird.
	 * 
	 * @see net.moasdawiki.service.wiki.structure.PageElement#isInline()
	 */
	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new Parent(parentPagePath, fromPos, toPos);
	}
}
