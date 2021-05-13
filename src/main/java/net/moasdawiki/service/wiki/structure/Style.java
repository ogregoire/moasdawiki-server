/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Formatiert einen Textbaustein mit Hilfe von CSS-Klassen.
 * Der Text wird dabei mit einem &lt;span>-Tag umschlossen.
 */
public class Style extends PageElementWithChild {

	/**
	 * CSS-Klassen für die Formatierung.
	 */
	@NotNull
	private final String[] cssClasses;

	public Style(@NotNull String[] cssClasses, @Nullable PageElement content, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.cssClasses = cssClasses;
	}

	/**
	 * Gibt die CSS-Klassen für die Formatierung zurück.
	 */
	@NotNull
	public String[] getCssClasses() {
		return cssClasses;
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new Style(cssClasses, child.clonePageElement(), fromPos, toPos);
		} else {
			return new Style(cssClasses, null, fromPos, toPos);
		}
	}
}
