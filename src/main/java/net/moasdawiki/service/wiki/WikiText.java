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

package net.moasdawiki.service.wiki;

/**
 * DTO mit dem ganzen Wikitext einer Wikidatei oder einen Ausschnitt davon. Wird
 * verwendet, um eine Wikiseite im Texteditor zu editieren.
 *
 * @author Herbert Reiter
 */
public class WikiText {

	/**
	 * Der Inhalt der Wikidatei, Text in Wikisyntax. Nicht <code>null</code>.
	 */
	public String text;

	/**
	 * Gibt die Position des ersten Zeichens in der Wikidatei an.<br>
	 * <br>
	 * <code>fromPos</code> und <code>toPos</code> geben den Ausschnitt in der
	 * Wikidatei an, dem der Text in {@link #text} entspricht. Ist
	 * <code>fromPos == toPos</code>, wird eine leere Zeichenkette referenziert.
	 * Wenn beide Werte gesetzt sind, muss <code>0 <= fromPos <= toPos</code>
	 * gelten. <code>null</code> -> gesamte Wikidatei.
	 */
	public Integer fromPos;

	/**
	 * Gibt die Position hinter dem letzten Zeichen an in der Wikidatei an.<br>
	 * <br>
	 * <code>fromPos</code> und <code>toPos</code> geben den Ausschnitt in der
	 * Wikidatei an, dem der Text in {@link #text} entspricht. Ist
	 * <code>fromPos == toPos</code>, wird eine leere Zeichenkette referenziert.
	 * Wenn beide Werte gesetzt sind, muss <code>0 <= fromPos <= toPos</code>
	 * gelten. <code>null</code> -> gesamte Wikidatei.
	 */
	public Integer toPos;
}
