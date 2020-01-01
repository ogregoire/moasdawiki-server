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

/**
 * Enthält eine einzelne Tabellenzelle.
 * 
 * @author Herbert Reiter
 */
public class TableCell {

	/**
	 * Inhalt der Zelle.
	 */
	@Nullable
	private PageElement content;

	/**
	 * true -> th anstatt td verwenden.
	 */
	private final boolean isHeader;

	/**
	 * CSS-Klassen zur Formatierung in HTML. Mehrere Klassen müssen durch
	 * Leerzeichen getrennt sein.<br>
	 * null -> keine Angaben.
	 */
	@Nullable
	private final String params;

	/**
	 * Tabellenzeile, in der die Zelle enthalten ist.
	 */
	@Nullable
	private TableRow parentRow;

	public TableCell(@Nullable PageElement content, boolean isHeader, @Nullable String params) {
		super();
		this.content = content;
		this.isHeader = isHeader;
		this.params = params;
		// Hinweis der Vaterknoten von "content" wird später gesetzt,
		// da er noch nicht bekannt ist.
	}

	/**
	 * Gibt den Inhalt der Zelle zurück.
	 */
	@Nullable
	public PageElement getContent() {
		return content;
	}

	/**
	 * Setzt einen neuen Inhalt der Zelle.
	 */
	public void setContent(@Nullable PageElement content) {
		this.content = content;

		if (content != null && parentRow != null) {
			content.setParent(parentRow.getParentTable());
		}
	}

	public boolean getIsHeader() {
		return isHeader;
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
	public TableRow getParentRow() {
		return parentRow;
	}

	public void setParentRow(@Nullable TableRow parentRow) {
		this.parentRow = parentRow;

		if (content != null && parentRow != null) {
			content.setParent(parentRow.getParentTable());
		}
	}

	@NotNull
	public TableCell cloneTyped() {
		if (content != null) {
			return new TableCell(content.clonePageElement(), isHeader, params);
		} else {
			return new TableCell(null, isHeader, params);
		}
	}
}
