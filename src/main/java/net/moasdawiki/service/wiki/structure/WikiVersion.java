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

/**
 * Repräsentiert Name und Version des Wiki-Servers.
 * 
 * @author Herbert Reiter
 */
public class WikiVersion extends PageElement {

	public WikiVersion(Integer fromPos, Integer toPos) {
		super();
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Rückgabewert ist egal, weil dieses Objekt sowieso vom Standard-Plugin
	 * ersetzt wird.
	 * 
	 * @see net.moasdawiki.service.wiki.structure.PageElement#isInline()
	 */
	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new WikiVersion(fromPos, toPos);
	}
}
