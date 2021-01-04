/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
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
 * Represents an item of an unordered or ordered list.
 */
public class ListItem extends PageElementWithChild {

	/**
	 * Level, must be >= 1, 1 = top level.
	 */
	private final int level;

	private final boolean ordered;

	public ListItem(int level, boolean ordered, @Nullable PageElement content, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.level = Math.max(level, 1);
		this.ordered = ordered;
	}

	public int getLevel() {
		return level;
	}

	public boolean isOrdered() {
		return ordered;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new ListItem(level, ordered, child.clonePageElement(), fromPos, toPos);
		} else {
			return new ListItem(level, ordered, null, fromPos, toPos);
		}
	}
}
