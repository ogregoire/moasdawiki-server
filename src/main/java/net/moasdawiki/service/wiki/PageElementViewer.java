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

/**
 * Dieses Interface beschreibt eine Callback-Methode, die beim Traversieren
 * eines Wikibaums oder Teilbaums für jeden Knoten aufgerufen wird. Der Baum
 * wird dabei nur gelesen, darf aber nicht modifiziert werden.<br>
 * <br>
 * Für Modifikationen ist das Interface {@link PageElementTransformer}
 * vorgesehen.
 * 
 * @param <T> Typ des Seitenelements, die traversiert werden sollen.
 */
public interface PageElementViewer<T extends PageElement> {

	/**
	 * Wird für jedes Seitenelement eine Wikibaums aufgerufen, das den passenden
	 * Typ hat.<br>
	 * <br>
	 * Wichtig: Der Parameter <tt>pageElement</tt> darf niemals modifiziert
	 * werden. Es dürfen nur lesende Zugriffe erfolgen.
	 * 
	 * @param pageElement Ein Knoten im Wikibaum. Darf nicht modifiziert werden.
	 *            Nicht null.
	 */
	void viewPageElement(@NotNull T pageElement);
}
