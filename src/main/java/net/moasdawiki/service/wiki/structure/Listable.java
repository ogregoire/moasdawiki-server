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
