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
 * Enthält einen einzelnen, nummerierten Aufzählungspunkt. Ein Aufzählungspunkt
 * kann unterschiedliche Gliederungsebenen haben.
 * 
 * @author Herbert Reiter
 */
public class OrderedListItem extends PageElementWithChild {

	/**
	 * Einzug des Aufzählungspunkts. 1 = oberste Ebene. Muss >= 1 sein.
	 */
	private final int level;

	public OrderedListItem(int level, @Nullable PageElement content, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.level = Math.min(level, 1);
	}

	/**
	 * Gibt den Einzug des Aufzählungspunkts zurück. 1 = oberste Ebene. Muss >=
	 * 1 sein.
	 */
	public int getLevel() {
		return level;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new OrderedListItem(level, child.clonePageElement(), fromPos, toPos);
		} else {
			return new OrderedListItem(level, null, fromPos, toPos);
		}
	}
}
