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

import net.moasdawiki.service.wiki.structure.Listable.PageNameFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Platzhalter, an dem der Name der Wiki-Seite eingesetzt werden soll.
 * 
 * @author Herbert Reiter
 */
public class PageName extends PageElement {

	/**
	 * Format, in dem der Seitenname ausgegeben werden soll.
	 */
	@Nullable
	private final PageNameFormat pageNameFormat;

	/**
	 * Soll der Text verlinkt werden?
	 */
	private final boolean linked;

	/**
	 * true -> Das Tag bezieht sich auf die Wurzel des Wikibaums.<br>
	 * false -> Das Tag bezieht sich auf die Wikiseite, die dieses Tag direkt
	 * enthält.
	 */
	private final boolean globalContext;

	public PageName(@Nullable PageNameFormat pageNameFormat, boolean linked, boolean globalContext, @Nullable Integer fromPos, @Nullable Integer toPos) {
		this.pageNameFormat = pageNameFormat;
		this.linked = linked;
		this.globalContext = globalContext;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@Nullable
	public PageNameFormat getPageNameFormat() {
		return pageNameFormat;
	}

	public boolean isLinked() {
		return linked;
	}

	public boolean isGlobalContext() {
		return globalContext;
	}

	/**
	 * Rückgabewert ist egal, weil dieses Objekt sowieso vom Standard-Plugin
	 * ersetzt wird.
	 * 
	 * @see net.moasdawiki.service.wiki.structure.PageElement#isInline()
	 */
	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new PageName(pageNameFormat, linked, globalContext, fromPos, toPos);
	}
}
