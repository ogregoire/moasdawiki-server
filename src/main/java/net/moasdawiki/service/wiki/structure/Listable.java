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
 * Dieses Interface definiert Parameter, mit denen die Darstellung einer Liste beeinflusst werden können.
 */
public interface Listable {

	/**
	 * Gibt die Darstellungsform für den Namen einer Wikiseite (oder einer
	 * anderen Datei) zurück.
	 * 
	 * @return Nicht null.
	 */
	@NotNull
	PageNameFormat getPageNameFormat();

	/**
	 * Gibt an, ob die Listeelemente inline in einer Zeile dargestellt werden
	 * sollen.
	 */
	boolean isShowInline();

	/**
	 * Trennzeichen zwischen aufeinanderfolgenden Listenelementen. Wird nur bei
	 * isInlineList()==true verwendet.
	 * 
	 * @return null --> kein Trennzeichen
	 */
	@Nullable
	String getInlineListSeparator();

	/**
	 * Die Ausgabe, falls die Liste leer ist.
	 * 
	 * @return null --> keine Ausgabe
	 */
	@Nullable
	String getOutputOnEmpty();

	/**
	 * Mögliche Darstellungsformen für den Namen einer Wikiseite (oder einer
	 * anderen Datei).
	 */
	enum PageNameFormat {
		PAGE_PATH, PAGE_FOLDER, PAGE_TITLE
	}
}
