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

import java.util.ArrayList;
import java.util.List;

/**
 * Enthält eine Zeile einer Tabelle.
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
