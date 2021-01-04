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
 * Beschreibt eine Überschrift in einer Wiki-Seite.<br>
 * <br>
 * Eine Überschrift kann unterschiedliche Gliederungsebenen haben. Die
 * Nummerierung wird beim Rendern einer Wiki-Seite bestimmt und ausgegeben.
 */
public class Heading extends PageElementWithChild {

	/**
	 * Ebene der Überschrift. 1 = oberste Ebene.
	 */
	private final int level;

	/**
	 * Erzeugt eine neue Überschrift.
	 * 
	 * @param level Ebene der Überschrift. 1 = oberste Ebene.
	 * @param content Inhalt der Überschrift.
	 * @param fromPos Position des ersten Zeichens
	 * @param toPos Position hinter dem letzten Zeichen
	 */
	public Heading(int level, @Nullable PageElement content, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.level = Math.max(level, 1);
	}

	/**
	 * Gibt die Ebene der Überschrift zurück. 1 = oberste Ebene.
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
			return new Heading(level, child.clonePageElement(), fromPos, toPos);
		} else {
			return new Heading(level, null, fromPos, toPos);
		}
	}
}
