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
 * Repräsentiert die Liste der zuletzt angesehenen Wiki-Seiten.
 */
public class ListViewHistory extends PageElement implements Listable {

	@NotNull
	private final PageNameFormat pageNameFormat;

	/**
	 * Listenelemente inline darstellen
 	 */
	private final boolean showInline;

	@Nullable
	private final String inlineListseparator;

	@Nullable
	private final String outputOnEmpty;

	/**
	 * maximale Anzahl der Einträge, -1 = unbegrenzt
 	 */
	private final int maxLength;

	public ListViewHistory(@NotNull PageNameFormat pageNameFormat, boolean showInline, @Nullable String inlineListseparator,
						   @Nullable String outputOnEmpty, int maxLength, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.pageNameFormat = pageNameFormat;
		this.showInline = showInline;
		this.inlineListseparator = inlineListseparator;
		this.outputOnEmpty = outputOnEmpty;
		this.maxLength = maxLength;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@NotNull
	public PageNameFormat getPageNameFormat() {
		return pageNameFormat;
	}

	public boolean isInline() {
		return showInline;
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

	public int getMaxLength() {
		return maxLength;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new ListViewHistory(pageNameFormat, showInline, inlineListseparator, outputOnEmpty, maxLength, fromPos,
				toPos);
	}
}
