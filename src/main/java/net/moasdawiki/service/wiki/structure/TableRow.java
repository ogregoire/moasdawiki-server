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

import java.util.ArrayList;
import java.util.List;

/**
 * Enthält eine Zeile einer Tabelle.
 * 
 * @author Herbert Reiter
 */
public class TableRow {

	/**
	 * Zellen in der Tabellenzeile. Nicht null.
	 */
	@NotNull
	private final List<TableCell> cells;

	/**
	 * CSS-Klassen zur Formatierung in HTML. Mehrere Klassen müssen durch
	 * Leerzeichen getrennt sein.<br>
	 * null -> keine Angaben.
	 */
	@Nullable
	private final String params;

	/**
	 * Tabelle, in der die Zeile enthalten ist.
	 */
	@Nullable
	private Table parentTable;

	public TableRow(@Nullable String params) {
		super();
		cells = new ArrayList<>();
		this.params = params;
	}

	/**
	 * Fügt eine Zelle ein.
	 * 
	 * @param cell Nicht null.
	 */
	public void addCell(@NotNull TableCell cell) {
		cells.add(cell);
		cell.setParentRow(this);
	}

	@NotNull
	public List<TableCell> getCells() {
		return cells;
	}

	/**
	 * CSS-Klassen zur Formatierung in HTML. Mehrere Klassen müssen durch
	 * Leerzeichen getrennt sein.
	 * 
	 * @return null -> keine Angaben.
	 */
	@Nullable
	public String getParams() {
		return params;
	}

	@Nullable
	public Table getParentTable() {
		return parentTable;
	}

	/**
	 * Setzt eine Tabelle als Vater-Knoten.
	 */
	void setParent(@NotNull Table parentTable) {
		this.parentTable = parentTable;

		// Vaterknoten der Zellinhalte aktualisieren
		for (TableCell cell : cells) {
			cell.setParentRow(this);
		}
	}

	@NotNull
	public TableRow cloneTyped() {
		TableRow newRow = new TableRow(params);
		for (TableCell cell : cells) {
			newRow.addCell(cell.cloneTyped());
		}
		return newRow;
	}
}
