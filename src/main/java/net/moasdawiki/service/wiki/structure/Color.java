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
 * Enthält einen Textbaustein, der in einer bestimmten Farbe dargestellt werden soll.
 *
 * Beispiele:
 * {{color:red}}...{{/color}}
 * {{color:#ff0000}}...{{/color}}
 */
public class Color extends PageElementWithChild {

	/**
	 * Name oder Code der Farbe.
	 */
	@NotNull
	private final String colorName;

	public Color(@NotNull String colorName, @Nullable PageElement content, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.colorName = colorName;
	}

	/**
	 * Gibt die Farbe zurück.
	 */
	@NotNull
	public String getColorName() {
		return colorName;
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new Color(colorName, child.clonePageElement(), fromPos, toPos);
		} else {
			return new Color(colorName, null, fromPos, toPos);
		}
	}
}
