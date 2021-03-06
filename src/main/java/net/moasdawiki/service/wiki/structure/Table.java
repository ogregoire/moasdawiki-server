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
 * Enthält eine Tabelle.
 */
public class Table extends PageElement {

	/**
	 * Tabellenzeilen.
	 */
	@NotNull
	private final List<TableRow> rows;

	/**
	 * CSS-Klassen zur Formatierung in HTML.
	 * Mehrere Klassen müssen durch Leerzeichen getrennt sein.
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
	 * CSS-Klassen zur Formatierung in HTML.
	 * Mehrere Klassen müssen durch Leerzeichen getrennt sein.
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
