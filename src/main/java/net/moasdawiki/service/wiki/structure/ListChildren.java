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
import org.jetbrains.annotations.Nullable;

/**
 * Listet alle Kind-Seiten einer Wiki-Seite auf. Kind-Seiten haben die
 * angegebene Wiki-Seite als Vater angegeben.
 * 
 * @author Herbert Reiter
 */
public class ListChildren extends PageElement implements Listable {

	@Nullable
	private final String pagePath; // null -> aktuelle Wikiseite
	@NotNull
	private final PageNameFormat pageNameFormat;
	private final boolean showInline; // Listenelemente inline darstellen
	@Nullable
	private final String inlineListseparator;
	@Nullable
	private final String outputOnEmpty;
	private final boolean globalContext;

	public ListChildren(@Nullable String pagePath, @NotNull PageNameFormat pageNameFormat, boolean showInline, @Nullable String inlineListseparator,
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
		return new ListChildren(pagePath, pageNameFormat, showInline, inlineListseparator, outputOnEmpty,
				globalContext, fromPos, toPos);
	}
}
