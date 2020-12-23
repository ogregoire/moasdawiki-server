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

package net.moasdawiki.service.wiki;

import net.moasdawiki.service.wiki.structure.PageElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dieses Interface beschreibt eine Callback-Methode, die bei der Transformation
 * eines Wikibaums für jeden Knoten aufgerufen wird.
 */
@FunctionalInterface
public interface PageElementTransformer {

	/**
	 * Transformiert ein Seitenelement.<br>
	 * <br>
	 * Diese Methode wird für alle Seitenelemente aufgerufen, außer für
	 * PageElementList-Typen. Wenn die Methode null zurückgibt, wird das
	 * Seitenelement samt Unterbaum ersatzlos gelöscht.<br>
	 * <br>
	 * Der Parameter <tt>pageElement</tt> darf dabei auch modifiziert werden.
	 * 
	 * @param pageElement Ein Knoten im Wikibaum.
	 * @return Transformiertes Seitenelement, das das ursprüngliche ersetzen
	 *         soll. null -> Der Knoten samt Unterbaum wird ersatzlos aus dem
	 *         Wikibaum entfernt.
	 */
	@Nullable
	PageElement transformPageElement(@NotNull PageElement pageElement);
}
