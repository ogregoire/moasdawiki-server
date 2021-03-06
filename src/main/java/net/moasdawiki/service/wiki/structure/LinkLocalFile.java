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
 * Link auf eine beliebige Datei im Repository, z.B. eine ZIP-Datei.
 */
public class LinkLocalFile extends PageElementWithChild {

	/**
	 * Pfad der Datei.
	 * Wenn der Pfad nicht mit '/' beginnt, handelt es sich um eine relative Adressierung zur Wikiseite, die den Link enthält.
	 */
	@NotNull
	private final String filePath;

	public LinkLocalFile(@NotNull String filePath, @Nullable PageElement alternativeText, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(alternativeText, fromPos, toPos);
		this.filePath = filePath;
	}

	/**
	 * Gibt den Dateipfad zurück.
	 */
	@NotNull
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Gibt den alternativen Text zurück, der anstelle des Dateinamens angezeigt werden soll.
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
			return new LinkLocalFile(filePath, child.clonePageElement(), fromPos, toPos);
		} else {
			return new LinkLocalFile(filePath, null, fromPos, toPos);
		}
	}
}
