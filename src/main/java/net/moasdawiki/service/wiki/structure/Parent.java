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
 * Verweist auf eine Vater-Seite. Dies ist eine logische Information, die eine
 * Beziehung zu einer anderen Wiki-Seite beschreibt. Sie wird normalerweise
 * nicht direkt angezeigt.
 */
public class Parent extends PageElement {
	/**
	 * Name der Vaterseite. Relativ zur Wikiseite oder absolut.
	 */
	@NotNull
	private final String parentPagePath;

	public Parent(@NotNull String parentPagePath, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.parentPagePath = parentPagePath;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@NotNull
	public String getParentPagePath() {
		return parentPagePath;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new Parent(parentPagePath, fromPos, toPos);
	}
}
