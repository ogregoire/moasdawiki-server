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
 * DTO, enthält den Inhalt einer Wiki-Seite und dient als Wurzel einer
 * Wiki-Seite.
 * 
 * @author Herbert Reiter
 */
public class WikiPage extends PageElementWithChild {

	/**
	 * Eindeutiger Name der Wikiseite. Enthält den vollständigen Pfad (Ordner +
	 * Seitentitel) der Wikiseite. null -> keine Wikiseite aus dem Repository,
	 * sondern eine künstliche Wikiseite, z.B. Seite mit Suchergebnissen.
	 */
	@Nullable
	private final String pagePath;

	/**
	 * Erzeugt eine Wikiseite.
	 * 
	 * @param pagePath Absoluter Name der Wikiseite. Dieser enthält auch den
	 *        Ordner, in dem die Wikiseite enthalten ist.
	 * @param content Inhalt der Wikiseite.
	 */
	public WikiPage(@Nullable String pagePath, @Nullable PageElement content, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.pagePath = pagePath;
	}

	/**
	 * Gibt den Pfad der Wikiseite zurück. Dieser enthält den Ordner, in dem die
	 * Wikiseite enthalten ist, und den Seitentitel. null -> keine Wikiseite aus
	 * dem Repository, sondern eine künstliche Wikiseite, z.B. Seite mit
	 * Suchergebnissen.<br>
	 * <br>
	 * Beispiel: /wiki/Help
	 */
	@Nullable
	public String getPagePath() {
		return pagePath;
	}

	public boolean isInline() {
		return false;
	}

	/**
	 * Erstellt eine tiefe Kopie der Wikiseite.
	 */
	@NotNull
	public WikiPage cloneTyped() {
		if (child != null) {
			return new WikiPage(pagePath, child.clonePageElement(), fromPos, toPos);
		} else {
			return new WikiPage(pagePath, null, fromPos, toPos);
		}
	}

	@NotNull
	public PageElement clonePageElement() {
		return cloneTyped();
	}
}
