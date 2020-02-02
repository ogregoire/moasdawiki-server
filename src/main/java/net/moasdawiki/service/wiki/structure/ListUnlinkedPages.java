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
 * Listet alle Wiki-Seiten im Repository auf, die sich im Ordner der Wikiseite oder in einem Unterordner befinden.
 * Alternativ kann der Pfad direkt angegeben.
 * 
 * @author Herbert Reiter
 */
public class ListUnlinkedPages extends PageElement implements Listable {

	/**
	 * Sollen alle Vater-Seiten ebenfalls als verlinkt behandelt und somit von der Liste entfernt werden?
	 * Das ist sinnvoll, wenn die Liste der Vater-Seiten z.B. im Seiten-Footer angegeben (und damit verlinkt) ist.
	 */
	private final boolean hideParents;

	/**
	 * Sollen alle Kind-Seiten ebenfalls als verlinkt behandelt und somit von der Liste entfernt werden?
	 * Das ist sinnvoll, wenn die Liste der Kind-Seiten z.B. im Seiten-Footer angegeben (und damit verlinkt) ist.
	 */
	private final boolean hideChildren;

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

	public ListUnlinkedPages(boolean hideParents, boolean hideChildren, @NotNull PageNameFormat pageNameFormat,
			boolean showInline, @Nullable String inlineListseparator, @Nullable String outputOnEmpty, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.hideParents = hideParents;
		this.hideChildren = hideChildren;
		this.pageNameFormat = pageNameFormat;
		this.showInline = showInline;
		this.inlineListseparator = inlineListseparator;
		this.outputOnEmpty = outputOnEmpty;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	public boolean isHideParents() {
		return hideParents;
	}

	public boolean isHideChildren() {
		return hideChildren;
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
		return new ListUnlinkedPages(hideParents, hideChildren, pageNameFormat, showInline, inlineListseparator,
				outputOnEmpty, fromPos, toPos);
	}
}
