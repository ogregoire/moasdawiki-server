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
 * Listet alle Wiki-Seiten im Repository auf, die sich im Ordner der Wikiseite oder in einem Unterordner befinden.
 * Alternativ kann der Pfad direkt angegeben.
 */
public class ListPages extends PageElement implements Listable {

	/**
	 * Nur Wikiseiten in diesem Ordner und allen Unterordnern auflisten.
	 * null -> Ordner der Wikiseite verwenden.
	 */
	@Nullable
	private final String folder;

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

	private final boolean globalContext;

	public ListPages(@Nullable String folder, @NotNull PageNameFormat pageNameFormat, boolean showInline, @Nullable String inlineListseparator,
					 @Nullable String outputOnEmpty, boolean globalContext, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.folder = folder;
		this.pageNameFormat = pageNameFormat;
		this.showInline = showInline;
		this.inlineListseparator = inlineListseparator;
		this.outputOnEmpty = outputOnEmpty;
		this.globalContext = globalContext;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@Nullable
	public String getFolder() {
		return folder;
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

	public boolean isGlobalContext() {
		return globalContext;
	}

	public boolean isInline() {
		return showInline;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new ListPages(folder, pageNameFormat, showInline, inlineListseparator, outputOnEmpty, globalContext,
				fromPos, toPos);
	}
}
