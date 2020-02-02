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

import java.util.Date;
import java.util.List;
import java.util.Set;

import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.wiki.structure.PageElementList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Serviceschicht für den Zugriff auf alle Wikidateien im Wiki-Repository.
 * 
 * @author Herbert Reiter
 */
public interface WikiService {

	/**
	 * Löscht den Cacheinhalt und initialisiert den Service neu, so wie beim Programmstart.
	 */
	void reset();

	/**
	 * Listet die Namen aller Wikidateien auf.
	 * 
	 * @return Wikidateien. Nicht <code>null</code>. Darf nicht modifiziert
	 *         werden.
	 */
	@NotNull
	Set<String> getWikiFilePaths();

	/**
	 * Gibt zurück, ob die angegebene Wikidatei existiert.
	 */
	boolean existsWikiFile(@NotNull String wikiFilePath);

	/**
	 * Gibt die angeforderte Wikidatei zurück. Erstellt eine Kopie, damit das
	 * Original nicht verändert werden kann.
	 * 
	 * @param wikiFilePath Name der Wikidatei.
	 * @return Wikiseite
	 * @throws ServiceException if file doesn't exist
	 */
	@NotNull
	WikiFile getWikiFile(@NotNull String wikiFilePath) throws ServiceException;

	/**
	 * Löscht die angegebene Wikidatei aus dem Cache und dem Repository.
	 * 
	 * @param wikiFilePath Name der Wikidatei.
	 */
	void deleteWikiFile(@NotNull String wikiFilePath) throws ServiceException;

	/**
	 * Gibt einen Ausschnitt des Wikitexts einer Wikidatei zurück. Sind die
	 * Parameter <code>fromPos</code> und <code>toPos</code> gesetzt, wird nur
	 * ein Ausschnitt der Wikidatei zurückgegeben. Dies ist z.B. beim Editieren
	 * eines Unterkapitels nützlich.
	 * 
	 * @param wikiFilePath Name der Wikidatei.
	 * @param fromPos Position des ersten Zeichens des Ausschnitts der
	 *        Wikidatei. <code>null</code> --> ganze Wikidatei lesen.
	 * @param toPos Position nach dem letzten Zeichen des Ausschnitts der
	 *        Wikidatei. <code>null</code> --> ganze Wikidatei lesen.
	 * @return Text der Wikidatei. <code>null</code> --> Wikidatei existiert
	 *         nicht.
	 */
	@NotNull
	WikiText readWikiText(@NotNull String wikiFilePath, @Nullable Integer fromPos, @Nullable Integer toPos) throws ServiceException;

	/**
	 * Schreibt den Wikitext einer Wikidatei. Aktualisiert den Cache und das
	 * Repository. Wenn die angegebene Wikidatei noch nicht existiert, wird sie
	 * angelegt.<br>
	 * <br>
	 * Wenn der Parameter <code>wikiText</code> nur einen Ausschnitt einer
	 * Wikidatei enthält, wird dieser in die komplette Wikidatei eingearbeitet,
	 * indem der angegebene Ausschnitt ersetzt wird.
	 * 
	 * @param wikiFilePath Name der Wikidatei.
	 * @param wikiText Text der Wikidatei.
	 */
	@SuppressWarnings("UnusedReturnValue")
	@NotNull
	WikiFile writeWikiText(@NotNull String wikiFilePath, @NotNull WikiText wikiText) throws ServiceException;

	/**
	 * Listet alle nach dem angegebenen Zeitpunkt im Repository geänderten
	 * Wikiseiten auf. Wird zum effizienten Aktualisieren von internen Caches
	 * verwendet.
	 * 
	 * @param modifiedAfter Frühester Änderungszeitpunkt (exklusive).
	 *        <code>null</code> --> alle Wikiseiten auflisten.
	 * @return Wikiseiten.
	 */
	@NotNull
	Set<String> getModifiedAfter(@Nullable Date modifiedAfter);

	/**
	 * Gibt eine Liste der zuletzt geänderten Wiki-Seiten zurück.
	 * 
	 * @param count maximale Anzahl der Einträge; -1 = keine Beschränkung
	 * @return Liste der Wikiseitennamen, absteigend sortiert nach
	 *         Änderungsdatum. Nicht <code>null</code>.
	 */
	@NotNull
	List<String> getLastModified(int count);

	/**
	 * Gibt die zuletzt besuchten Wiki-Seiten zurück.
	 * 
	 * @param count maximale Anzahl der Einträge; -1 = alle Einträge.
	 * @return Liste, absteigend sortiert nach Besuchsreihenfolge.
	 */
	@NotNull
	List<String> getLastViewedWikiFiles(int count);

	/**
	 * Trägt eine besuchte Wikiseite in die Liste ein. Vermeidet Duplikate.
	 */
	void addLastViewedWikiFile(@NotNull String wikiFilePath);

	/**
	 * Parst einen Wikitext.
	 * 
	 * @param wikiText Wikitext, der geparst werden soll.
	 * @return Vom Parser erzeugter Parsebaum.
	 */
	@NotNull
	PageElementList parseWikiText(@NotNull String wikiText) throws ServiceException;
}
