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
 * Listet alle nicht vorhandenen Wikiseiten auf, auf die es mindestens einen
 * Link gibt. Diese Wikiseiten werden "wanted pages" bezeichnet.
 * 
 * @author Herbert Reiter
 */
public class ListWantedPages extends PageElement implements Listable {

	@NotNull
	private final PageNameFormat pageNameFormat;
	private final boolean showInline; // Listenelemente inline darstellen?
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
