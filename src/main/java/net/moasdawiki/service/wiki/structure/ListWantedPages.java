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
 * Listet alle nicht vorhandenen Wikiseiten auf, auf die es mindestens einen Link gibt.
 * Diese Wikiseiten werden "wanted pages" bezeichnet.
 */
public class ListWantedPages extends PageElement implements Listable {

	@NotNull
	private final PageNameFormat pageNameFormat;

	/**
	 * Listenelemente inline darstellen?
	 */
	private final boolean showInline;

	@Nullable
	private final String inlineListseparator;

	@Nullable
	private final String outputOnEmpty;

	public ListWantedPages(@NotNull PageNameFormat pageNameFormat, boolean showInline, @Nullable String inlineListseparator,
						   @Nullable String outputOnEmpty, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.pageNameFormat = pageNameFormat;
		this.showInline = showInline;
		this.inlineListseparator = inlineListseparator;
		this.outputOnEmpty = outputOnEmpty;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@NotNull
	public PageNameFormat getPageNameFormat() {
		return pageNameFormat;
	}

	public boolean isShowInline() {
		return showInline;
	}

	@Nullable
	public String getInlineListSeparator() {
		return inlineListseparator;
	}

	@Nullable
	public String getOutputOnEmpty() {
		return outputOnEmpty;
	}

	public boolean isInline() {
		return showInline;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new ListWantedPages(pageNameFormat, showInline, inlineListseparator, outputOnEmpty, fromPos, toPos);
	}
}
