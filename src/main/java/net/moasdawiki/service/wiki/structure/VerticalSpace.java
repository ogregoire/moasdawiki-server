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
 * Vertikaler Abstand von ca. einer Zeile. Wird z.B. zwischen zwei Textabsätzen
 * verwendet.
 */
public class VerticalSpace extends PageElement {

	public VerticalSpace() {
		this(null, null);
	}

	public VerticalSpace(@Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new VerticalSpace(fromPos, toPos);
	}
}
