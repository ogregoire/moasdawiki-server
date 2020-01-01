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

package net.moasdawiki.plugin;

import net.moasdawiki.server.HttpRequest;
import net.moasdawiki.server.HttpResponse;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface für alle Wiki-Plugins.<br>
 * <br>
 * Damit ein Wiki-Plugin geladen wird, muss es in der Datei config.txt
 * registriert werden. Wurden mehrere Plugin registriert, werden diese in der
 * Reihenfolge ihrer Registrierung aufgerufen. Es wird empfohlen, das
 * Standard-Plugin stets als letztes Plugin anzugeben, da dieses einige
 * Platzhalter entfernt und durch entsprechenden Text ersetzt.<br>
 * <br>
 * Ein Plugin kann beliebige <b>Transformationen</b> einer Wiki-Seite
 * durchführen. Dazu wird die Wiki-Seite in geparster Form als Wiki-Baum (die
 * Zwischendarstellung) der Methode {@link #transformWikiPage(WikiPage)}
 * übergeben. Diesen Wiki-Baum kann das Plugin dann traversieren und nach
 * Belieben verändern.<br>
 * <br>
 * Um beliebig anspruchsvolle Anwendungen zu ermöglichen, können HTML-Formulare
 * direkt an ein Plugin geschickt werden. Hierzu werden die Formulardaten per
 * POST an die URL <tt>/plugin/PluginName</tt>, wobei <i>PluginName</i> der
 * Klassenname des Plugins ohne Packagename ist. Es wird dann die Methode
 * {@link #handleRequest(HttpRequest)} mit den Formulardaten aufgerufen. Das
 * Ergebnis wird an den Browser zurückgeschickt. Mit diesem Mechanismus fungiert
 * MoasdaWiki als Applikationsserver.
 * 
 * @author Herbert Reiter
 */
public interface Plugin {

	/**
	 * Übergibt eine Referenz auf die Serviceschicht (API). Diese Methode wird
	 * unmittelbar nach Instantiieren des Plugins aufgerufen.
	 */
	void setServiceLocator(@NotNull ServiceLocator serviceLocator);

	/**
	 * Methode, die zum Transformieren einer Wikiseite aufgerufen wird. Die
	 * Reihenfolge der Methodenaufrufe der Plugins wird über die Annotation
	 * {@link CallOrder} festgelegt.
	 * 
	 * @param wikiPage Wikiseite, die zu transformieren ist. Nicht
	 *            <code>null</code>.
	 * @return Transformierte Wikiseite. Nicht <code>null</code>.
	 */
	@NotNull
	default WikiPage transformWikiPage(@NotNull WikiPage wikiPage) {
		return wikiPage;
	}

	/**
	 * Wird aufgerufen, wenn ein HTML-Formular an ein Plugin geschickt wird. Die
	 * Zuordnung des URL-Pfads an ein Plugin erfolgt über die Annotation
	 * {@link PathPattern}. Ist eine Methode nicht annotiert, wird sie nie
	 * aufgerufen.
	 * 
	 * @param request HTTP-Requestdaten, enthält die Formulardaten. Nicht
	 *            <code>null</code>.
	 * @return HTTP-Response, der zum Browser geschickt wird.
	 *         <code>null</code> -> Request URL nicht implementiert, führt zu 404-Antwort.
	 */
	@Nullable
	default HttpResponse handleRequest(@NotNull HttpRequest request) {
		return new HttpResponse();
	}
}
