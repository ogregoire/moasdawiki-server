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

import net.moasdawiki.service.wiki.structure.Listable.PageNameFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Platzhalter, an dem der Name der Wiki-Seite eingesetzt werden soll.
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
	 * false -> Das Tag bezieht sich auf die Wikiseite, die dieses Tag direkt enthält.
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

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new PageName(pageNameFormat, linked, globalContext, fromPos, toPos);
	}
}
