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
 * Link auf eine URL im Internet.<br>
 * <br>
 * Beispiel-URL: <tt>http://www.google.de/</tt>
 * 
 * @author Herbert Reiter
 */
public class LinkExternal extends PageElementWithChild {

	/**
	 * Vollständige URL, auf die verlinkt werden soll.
	 */
	@NotNull
	private final String url;

	public LinkExternal(@NotNull String url, @Nullable PageElement alternativeText, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(alternativeText, fromPos, toPos);
		this.url = url;
	}

	/**
	 * Gibt die verlinkte URL zurück.
	 */
	@NotNull
	public String getUrl() {
		return url;
	}

	/**
	 * Gibt den alternativen Text zurück, der anstelle der URL angezeigt werden
	 * soll. null -> kein alternativer Text vorhanden.
	 */
	@Nullable
	public PageElement getAlternativeText() {
		return getChild();
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new LinkExternal(url, child.clonePageElement(), fromPos, toPos);
		} else {
			return new LinkExternal(url, null, fromPos, toPos);
		}
	}
}
