/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstrakte Oberklasse für alle Seitenelemente einer Wiki-Seite.<br>
 * <br>
 * {@link #isInline()} bestimmt, wie das Seitenelement auf der Seite angeordnet
 * werden soll.
 * <ul>
 * <li>Ist isInline() == false, beansprucht das Seitenelement einen eigenen
 * horizontalen Abschnitt.</li>
 * <li>Aufeinanderfolgende Element mit isInline() == true werden in dieselbe
 * Zeile gepackt.</li>
 * </ul>
 */
public abstract class PageElement {

	/**
	 * Verweis auf den Vater-Knoten, der das Seitenelement enthält. null -> es
	 * gibt keinen Vater-Knoten, d.h. es handelt sich um die Wurzel.
	 */
	@Nullable
	protected PageElement parent;

	/**
	 * Geben den Ausschnitt in der Wikidatei an, aus dem dieses Seitenelement
	 * entstanden ist. <code>fromPos</code> gibt die Position des ersten
	 * Zeichens, <code>toPos</code> die Position hinter dem letzten Zeichen an.<br>
	 * <br>
	 * null -> Position unbekannt, z.B. bei künstlich erzeigten Seitenelementen.
	 */
	@Nullable
	protected Integer fromPos;
	@Nullable
	protected Integer toPos;

	/**
	 * Bestimmt, ob das Seitenelement mit mehreren anderen Seitenelementen in
	 * einer Zeile dargestellt werden soll (true) oder ob das Seitenelement
	 * einen eigenen Abschnitt beansprucht (false).
	 */
	public abstract boolean isInline();

	/**
	 * Gibt den Vater-Knoten zurück.
	 * 
	 * @return Der Vater-Knoten. null -> es gibt keinen Vater-Knoten, d.h. es
	 *         handelt sich um die Wurzel.
	 */
	@Nullable
	public PageElement getParent() {
		return parent;
	}

	/**
	 * Setzt den Vater-Knoten des Seitenelements.
	 */
	public void setParent(@Nullable PageElement parent) {
		this.parent = parent;
	}

	/**
	 * Gibt die Position des ersten Zeichens innerhalb der gesamten Wikiseite
	 * an, aus dem dieses Seitenelement entstanden ist.<br>
	 * <br>
	 * null -> Position unbekannt, z.B. bei künstlich erzeigten Seitenelementen.
	 * 
	 * @return Position des ersten Zeichens.
	 */
	@Nullable
	public Integer getFromPos() {
		return fromPos;
	}

	/**
	 * Gibt die Position nach dem letzten Zeichen innerhalb der gesamten
	 * Wikiseite an, aus dem dieses Seitenelement entstanden ist.<br>
	 * <br>
	 * null -> Position unbekannt, z.B. bei künstlich erzeigten Seitenelementen.
	 * 
	 * @return Position nach dem letzten Zeichen.
	 */
	@Nullable
	public Integer getToPos() {
		return toPos;
	}

	/**
	 * Setzt den Ausschnitt in der Wikidatei, aus dem dieses Seitenelement
	 * entstanden ist.
	 * 
	 * @param fromPos Position des ersten Zeichens
	 * @param toPos Position hinter dem letzten Zeichen
	 */
	public void setFromToPos(int fromPos, int toPos) {
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Erstellt eine tiefe Kopie des Seitenelements.
	 */
	@NotNull
	public abstract PageElement clonePageElement();
}
