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
import java.util.List;

/**
 * Enthält eine Tabelle.
 * 
 * @author Herbert Reiter
 */
public class Table extends PageElement {

	/**
	 * Tabellenzeilen. Nicht null.
	 */
	@NotNull
	private final List<TableRow> rows;

	/**
	 * CSS-Klassen zur Formatierung in HTML. Mehrere Klassen müssen durch
	 * Leerzeichen getrennt sein.<br>
	 * null -> keine Angaben.
	 */
	@Nullable
	private final String params;

	public Table(@Nullable String params, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.rows = new ArrayList<>();
		this.params = params;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Fügt die angegebene Zeile ein.
	 */
	public void addRow(@NotNull TableRow row) {
		rows.add(row);
		row.setParent(this);
	}

	/**
	 * Erstellt eine neue, leere Tabellenzeile.
	 */
	public void newRow(@Nullable String rowParams) {
		addRow(new TableRow(rowParams));
	}

	/**
	 * Fügt eine neue Zelle am Ende der letzten Zeile ein.
	 */
	public void addCell(@NotNull TableCell cell) {
		if (rows.size() > 0) {
			rows.get(rows.size() - 1).addCell(cell);
		}
	}

	@NotNull
	public List<TableRow> getRows() {
		return rows;
	}

	/**
	 * CSS-Klassen zur Formatierung in HTML. Mehrere Klassen müssen durch
	 * Leerzeichen getrennt sein.
	 * 
	 * @return <code>null</code> -> keine Angaben.
	 */
	@Nullable
	public String getParams() {
		return params;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		Table newTable = new Table(params, fromPos, toPos);
		for (TableRow row : rows) {
			newTable.addRow(row.cloneTyped());
		}
		return newTable;
	}
}
