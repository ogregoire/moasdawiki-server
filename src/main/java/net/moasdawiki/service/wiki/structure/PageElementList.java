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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Enthält eine Liste mit Seitenelementen.<br>
 * <br>
 * Diese Liste kann selbst wieder als Seitenelement verwendet werden. Der
 * Listeninhalt wird dann so behandelt, als ob er direkt in der übergeordneten
 * Liste enthalten wäre, stellt also eine Teilliste dar.
 * 
 * @author Herbert Reiter
 */
public class PageElementList extends PageElement implements Iterable<PageElement> {

	/**
	 * Listeneinträge. Nicht <code>null</code>.
	 */
	@NotNull
	private final ArrayList<PageElement> elementList;

	/**
	 * Erzeugt eine neue PageElementList.
	 */
	public PageElementList() {
		this(null, null);
	}

	/**
	 * Erzeugt eine neue PageElementList.
	 */
	public PageElementList(@Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		elementList = new ArrayList<>();
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Übernimmt das angegebene Seitenelement. Die Parent-Referenz wird dabei
	 * automatisch gesetzt.
	 */
	public void add(@NotNull PageElement pageElement) {
		elementList.add(pageElement);
		pageElement.setParent(this);
	}

	/**
	 * Übernimmt alle Seitenelemente in die eigene Liste. Die Parent-Referenzen
	 * werden dabei umgehängt.
	 */
	public void addAll(@NotNull PageElementList pageElementList) {
		for (PageElement pe : pageElementList.elementList) {
			add(pe);
		}
	}

	/**
	 * Entfernt das Listenelement mit dem angegebenen Index.
	 */
	public void remove(int index) {
		elementList.remove(index);
	}

	/**
	 * Gibt das Element mit dem angegebenen Index zurück.
	 */
	@NotNull
	public PageElement get(int index) {
		return elementList.get(index);
	}

	public void set(int index, @NotNull PageElement pageElement) {
		elementList.set(index, pageElement);
		pageElement.setParent(this);
	}

	/**
	 * Gibt die Anzahl der Listeneinträge zurück.
	 */
	public int size() {
		return elementList.size();
	}

	/**
	 * Rückgabewert ist hier irrelevant, weil die Listenelemente individuell
	 * interpretiert werden.
	 * 
	 * @see net.moasdawiki.service.wiki.structure.PageElement#isInline()
	 */
	public boolean isInline() {
		return true;
	}

	@NotNull
	public Iterator<PageElement> iterator() {
		return elementList.iterator();
	}

	/**
	 * Erstellt eine tiefe Kopie der Seitenelementliste.
	 */
	@NotNull
	public PageElementList cloneTyped() {
		PageElementList result = new PageElementList(fromPos, toPos);
		for (PageElement pe : elementList) {
			result.add(pe.clonePageElement());
		}
		return result;
	}

	@NotNull
	public PageElement clonePageElement() {
		return cloneTyped();
	}
}
