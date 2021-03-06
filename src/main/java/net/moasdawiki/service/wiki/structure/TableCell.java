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
 * Enthält eine einzelne Tabellenzelle.
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
	private final boolean header;

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

	public TableCell(@Nullable PageElement content, boolean header, @Nullable String params) {
		super();
		this.content = content;
		this.header = header;
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

	public boolean isHeader() {
		return header;
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
			return new TableCell(content.clonePageElement(), header, params);
		} else {
			return new TableCell(null, header, params);
		}
	}
}
