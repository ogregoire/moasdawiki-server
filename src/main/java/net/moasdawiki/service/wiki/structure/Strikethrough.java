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
 * Enthält einen Textbaustein, der durchgestrichen dargestellt werden soll.
 */
public class Strikethrough extends PageElementWithChild {

	public Strikethrough(@Nullable PageElement content, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new Strikethrough(child.clonePageElement(), fromPos, toPos);
		} else {
			return new Strikethrough(null, fromPos, toPos);
		}
	}
}
