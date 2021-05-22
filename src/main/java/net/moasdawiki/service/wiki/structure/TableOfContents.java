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
 * Repräsentiert das Inhaltsverzeichnis der Wiki-Seite.
 */
public class TableOfContents extends PageElement {

	public TableOfContents(@Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new TableOfContents(fromPos, toPos);
	}
}
