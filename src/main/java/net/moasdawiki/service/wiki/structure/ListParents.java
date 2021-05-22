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
 * Listet alle Vater-Seiten der Wiki-Seite auf.
 */
public class ListParents extends PageElement implements Listable {

	/**
	 * null -> aktuelle Wikiseite
	 */
	@Nullable
	private final String pagePath;

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

	private final boolean globalContext;

	public ListParents(@Nullable String pagePath, @NotNull PageNameFormat pageNameFormat, boolean showInline, @Nullable String inlineListseparator,
					   @Nullable String outputOnEmpty, boolean globalContext, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.pagePath = pagePath;
		this.pageNameFormat = pageNameFormat;
		this.showInline = showInline;
		this.inlineListseparator = inlineListseparator;
		this.outputOnEmpty = outputOnEmpty;
		this.globalContext = globalContext;
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

	@Nullable
	public String getPagePath() {
		return pagePath;
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

	@NotNull
	public PageElement clonePageElement() {
		return new ListParents(pagePath, pageNameFormat, showInline, inlineListseparator, outputOnEmpty, globalContext,
				fromPos, toPos);
	}
}
