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
 * Enthält einen Absatz mit Einzug und/oder Zentrierung.<br>
 * <br>
 * Wenn keine derartige Formatierung gewünscht ist, müssen die Seitenelemente
 * nicht in einen Paragraph gepackt werden.
 */
public class Paragraph extends PageElementWithChild {
	private final boolean centered;
	private final int indention; // 0 = kein Einzug;
	private final boolean verticalSpacing; // Absätze optisch trennen

	public Paragraph(boolean centered, int indention, boolean verticalSpacing, @Nullable PageElement content, @Nullable Integer fromPos,
					 @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.centered = centered;
		this.indention = Math.max(indention, 0);
		this.verticalSpacing = verticalSpacing;
	}

	public boolean isCentered() {
		return centered;
	}

	public int getIndention() {
		return indention;
	}

	public boolean hasVerticalSpacing() {
		return verticalSpacing;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new Paragraph(centered, indention, verticalSpacing, child.clonePageElement(), fromPos, toPos);
		} else {
			return new Paragraph(centered, indention, verticalSpacing, null, fromPos, toPos);
		}
	}
}
