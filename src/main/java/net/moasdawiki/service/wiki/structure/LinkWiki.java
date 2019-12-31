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
 * Wiki-interner Link. Wird für Links auf virtuelle Seiten oder Wiki-Befehle
 * verwendet.
 * 
 * @author Herbert Reiter
 */
public class LinkWiki extends PageElementWithChild {

	/**
	 * Wiki-Befehl.
	 */
	@NotNull
	private final String command;

	public LinkWiki(@NotNull String command, @Nullable PageElement alternativeText, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(alternativeText, fromPos, toPos);
		this.command = command;
	}

	/**
	 * Gibt den Wiki-Befehl zurück.
	 */
	@NotNull
	public String getCommand() {
		return command;
	}

	/**
	 * Gibt den alternativen Text zurück, der anstelle des Wikibefehls angezeigt
	 * werden soll. null -> kein alternativer Text vorhanden.
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
			return new LinkWiki(command, child.clonePageElement(), fromPos, toPos);
		} else {
			return new LinkWiki(command, null, fromPos, toPos);
		}
	}
}
