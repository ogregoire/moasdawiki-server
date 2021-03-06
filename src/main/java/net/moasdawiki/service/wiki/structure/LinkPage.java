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
 * Link auf eine andere Wiki-Seite, ggf. auf einen Anker in der Seite.
 */
public class LinkPage extends PageElementWithChild {

	/**
	 * Name der Wikiseite, auf die der Link verweist.
	 * Wenn der Name nicht mit '/' beginnt, handelt es sich um eine relative Adressierung zur Wikiseite, die den Link enthält.
	 * null -> der Link verweist auf die Wikiseite, die das Link-Tag enthält (sinnvoll bei Ankerlinks).
	 */
	@Nullable
	private final String pagePath;

	/**
	 * Anker (ohne führendes '#') innerhalb einer Wikiseite, auf die der Link verweist.
	 * null -> keinen Anker verwenden.
	 */
	@Nullable
	private final String anchor;

	public LinkPage(@Nullable String pagePath, @Nullable PageElement alternativeText) {
		this(pagePath, null, alternativeText, null, null);
	}

	public LinkPage(@Nullable String pagePath, @Nullable String anchor, @Nullable PageElement alternativeText, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(alternativeText, fromPos, toPos);
		this.pagePath = pagePath;
		this.anchor = anchor;
	}

	/**
	 * Gibt den Namen der Wikiseite zurück, auf die der Link verweist.
	 *
	 * Wenn der Name nicht mit '/' beginnt, handelt es sich um eine relative Adressierung zur Wikiseite, die den Link enthält.
	 * null -> der Link verweist auf die Wikiseite, die den Link enthält (sinnvoll bei Ankerlinks).
	 */
	@Nullable
	public String getPagePath() {
		return pagePath;
	}

	/**
	 * Gibt den Anker (ohne führendes '#') innerhalb einer Wikiseite zurück, auf die der Link verweist.
	 * null -> keinen Anker verwenden.
	 */
	@Nullable
	public String getAnchor() {
		return anchor;
	}

	/**
	 * Gibt den alternativen Text zurück, der anstelle des Seitennamens angezeigt werden soll.
	 * null -> kein alternativer Text vorhanden.
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
			return new LinkPage(pagePath, anchor, child.clonePageElement(), fromPos, toPos);
		} else {
			return new LinkPage(pagePath, anchor, null, fromPos, toPos);
		}
	}
}
