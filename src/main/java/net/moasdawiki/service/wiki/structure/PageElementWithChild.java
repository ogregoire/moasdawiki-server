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

import org.jetbrains.annotations.Nullable;

/**
 * Viele Seitenelemente können Kindknoten des Typs {@link PageElement}
 * enthalten. Mit Hilfe dieser Klasse werden diese zusammengefasst. Dies ist bei
 * Baumtraversierungen und Baumtransformationen nützlich.
 */
public abstract class PageElementWithChild extends PageElement {

	/**
	 * Verweis auf den Kind-Knoten, d.h. den Unterbaum, den das Seitenelement
	 * enthält. Der Kind-Knoten kann je nach Implementierung eine
	 * unterschiedliche Bedeutung haben. Kann auch null sein.
	 */
	@Nullable
	protected PageElement child;

	/**
	 * Initialisiert den Kind-Knoten gleich mit.
	 * 
	 * @param child Der Kind-Knoten.
	 */
	protected PageElementWithChild(@Nullable PageElement child, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		setChild(child);
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Gibt den Kindknoten zurück. Dieser darf für Baumtransformationen
	 * verändert werden.
	 * 
	 * @return Der Kindknoten. Kann auch null sein.
	 */
	@Nullable
	public PageElement getChild() {
		return child;
	}

	/**
	 * Ersetzt den bisherigen Kindknoten durch einen neuen. Diese Methode wird
	 * bei Baumtransformationen aufgerufen. Dabei wird die Parent-Referenz
	 * automatisch aktualisiert.
	 * 
	 * @param child Neuer Kindknoten. Kann auch null sein.
	 */
	public void setChild(@Nullable PageElement child) {
		this.child = child;
		if (child != null) {
			child.setParent(this);
		}
	}
}
