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
 * Fügt eine komplette Wiki-Seite an dieser Stelle ein. Das Include-Tag wird
 * durch das WikiPage-Objekt ersetzt, an dem der Inhalt der Unterseite hängt.
 * 
 * @author Herbert Reiter
 */
public class IncludePage extends PageElement {
	/**
	 * Name der Wikiseite, auf die der Link verweist. Wenn der Name nicht mit
	 * '/' beginnt, handelt es sich um eine relative Adressierung zur Wikiseite,
	 * die den Link enthält. Nicht null.
	 */
	@NotNull
	private final String pagePath;

	public IncludePage(@NotNull String pagePath, Integer fromPos, Integer toPos) {
		super();
		this.pagePath = pagePath;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@NotNull
	public String getPagePath() {
		return pagePath;
	}

	/**
	 * Rückgabewert ist egal, weil dieses Objekt sowieso vom Standard-Plugin
	 * ersetzt wird.
	 * 
	 * @see net.moasdawiki.service.wiki.structure.PageElement#isInline()
	 */
	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new IncludePage(pagePath, fromPos, toPos);
	}
}
