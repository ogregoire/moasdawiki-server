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

package net.moasdawiki.service.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Erzeugt formatiertes HTML. Transportklasse.
 */
public class HtmlWriter {

	/**
	 * Mögliche Request-Methoden in HTML-Formularen.
	 */
	public enum Method {
		GET, POST
	}

	/**
	 * Titel der HTML-Seite. null -> kein Titel.
	 */
	@Nullable
	private String title;

	/**
	 * Optionale Parameter des body-Tags. null -> keine Parameter.
	 */
	@Nullable
	private String bodyParams;

	/**
	 * Inhalt des HTML-Body.
	 */
	@NotNull
	private final ArrayList<String> bodyText;

	/**
	 * Stack der geöffneten HTML-Tags.
	 */
	@NotNull
	private final Stack<String> tagStack;

	/**
	 * Weiteren HTML-Text in eine neue Zeile schreiben?
	 */
	private boolean continueInNewLine;

	public HtmlWriter() {
		super();
		bodyText = new ArrayList<>();
		tagStack = new Stack<>();
		// an Anfang muss eine neue Zeile erzeugt werden
		continueInNewLine = true;
	}

	/**
	 * Setzt den Titel der HTML-Seite.
	 * 
	 * @param title Titel der HTML-Seite.
	 */
	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	/**
	 * Gibt den Titel der HTML-Seite zurück.
	 */
	@Nullable
	public String getTitle() {
		return title;
	}

	/**
	 * Gibt die Parameter des body-Tags zurück.
	 */
	@Nullable
	public String getBodyParams() {
		return bodyParams;
	}

	/**
	 * Setzt die Parameter des body-Tags.
	 * 
	 * @param bodyParams Parameter. null -> keine Parameter.
	 */
	public void setBodyParams(@Nullable String bodyParams) {
		this.bodyParams = bodyParams;
	}

	/**
	 * Schreibt künftigen HTML-Text in eine neue Zeile.
	 */
	public void setContinueInNewLine() {
		continueInNewLine = true;
	}

	/**
	 * Fügt einen beliebigen HTML-Text ein.
	 */
	public void htmlText(@Nullable String text) {
		// ggf. eine neue Zeile anfangen
		if (continueInNewLine) {
			// Einzug mit Leerzeichen ermitteln und eintragen
			StringBuilder s = new StringBuilder();
			for (int i = 1; i <= tagStack.size(); i++) {
				s.append("  ");
			}
			bodyText.add(s.toString());

			continueInNewLine = false;
		}

		// Text an aktuelle Zeile anhängen
		if (text != null) {
			int index = bodyText.size() - 1;
			String line = bodyText.get(index);
			line += text;
			bodyText.set(index, line);
		}
	}

	/**
	 * Fügt einen HTML-Zeilenumbruch &lt;br&gt; ein.
	 */
	public void htmlNewLine() {
		htmlText("<br>");
		setContinueInNewLine();
	}

	public int openTag(@NotNull String tagName) {
		return openTag(tagName, null);
	}

	/**
	 * Öffnet ein Tag in der HTML-Ausgabe.
	 * 
	 * @param tagName Name des Tags.
	 * @param params Evtl. Parameter im HTML-Format. null = keine Parameter
	 * @return Gibt die Stacktiefe vor dem Öffnen des neuen Tags zurück. Diese
	 *         kann für {@link #closeTags(int)} verwendet werden, um offen
	 *         gebliebene Tags (z.B. einer Aufzählung) zu schließen).
	 */
	public int openTag(@NotNull String tagName, @Nullable String params) {
		if (params != null && params.length() > 0) {
			htmlText("<" + tagName + " " + params + ">");
		} else {
			htmlText("<" + tagName + ">");
		}
		tagStack.push(tagName);
		return tagStack.size() - 1;
	}

	public int openDivTag(@NotNull String stylesheetClass) {
		return openDivTag(stylesheetClass, null);
	}

	public int openDivTag(@Nullable String stylesheetClass, @Nullable String params) {
		StringBuilder tagParams = new StringBuilder();
		if (stylesheetClass != null) {
			tagParams.append("class=\"");
			tagParams.append(EscapeUtils.escapeHtml(stylesheetClass));
			tagParams.append("\"");
		}
		if (params != null) {
			if (tagParams.length() > 0) {
				tagParams.append(' ');
			}
			tagParams.append(params);
		}
		return openTag("div", tagParams.toString());
	}

	public int openSpanTag(@Nullable String stylesheetClass) {
		return openTag("span", "class=\"" + EscapeUtils.escapeHtml(stylesheetClass) + "\"");
	}

	@SuppressWarnings("UnusedReturnValue")
	public int openFormTag(@Nullable String name) {
		return openFormTag(name, null, null);
	}

	public int openFormTag(@Nullable String name, @Nullable String action, @Nullable Method method) {
		StringBuilder params = new StringBuilder();
		params.append("method=\"");
		if (method == Method.GET) {
			params.append(EscapeUtils.escapeHtml("get"));
		} else {
			params.append(EscapeUtils.escapeHtml("post"));
		}
		params.append('"');
		if (action != null) {
			params.append(" action=\"");
			params.append(EscapeUtils.escapeHtml(action));
			params.append('"');
		}
		params.append(" enctype=\"application/x-www-form-urlencoded; charset=utf-8\"");
		if (name != null) {
			params.append(" name=\"");
			params.append(EscapeUtils.escapeHtml(name));
			params.append('"');
		}
		return openTag("form", params.toString());
	}

	/**
	 * Schließt das zuletzt geöffnete Tag.
	 * Wenn kein Tag offen ist, passiert nichts.
	 */
	public void closeTag() {
		if (!tagStack.empty()) {
			String tag = tagStack.pop();
			htmlText("</" + tag + ">");
		}
	}

	/**
	 * Schließt offene Tags bis zur angegebenen Stacktiefe.
	 * 0 = alle Tags schließen.
	 */
	public void closeTags(int stackDepth) {
		while (tagStack.size() > stackDepth) {
			closeTag();
		}
	}

	public void closeAllTags() {
		closeTags(0);
	}

	/**
	 * Gibt das zuletzt geöffnete, noch offene Tag zurück.
	 * Wenn kein Tag offen ist, wird null zurückgegeben.
	 */
	@Nullable
	public String getCurrentTag() {
		return getCurrentTag(0);
	}

	/**
	 * Gibt ein zuvor geöffnetes Tag zurück.
	 *
	 * @param downStack Anzahl Stack-Ebenen tiefer nachschauen; 0 -> zuletzt geöffnetes Tag.
	 */
	@Nullable
	public String getCurrentTag(int downStack) {
		if (tagStack.size() > downStack && downStack >= 0) {
			return tagStack.elementAt(tagStack.size() - 1 - downStack);
		} else {
			// Stack ist nicht so groß
			return null;
		}
	}

	/**
	 * Fügt den Body des angegebenen HtmlWriter ein.
	 */
	public void addHtmlWriter(@NotNull HtmlWriter htmlWriter) {
		// evtl. offene Tags schließen
		htmlWriter.closeAllTags();
		for (String line : htmlWriter.bodyText) {
			setContinueInNewLine();
			htmlText(line);
		}
		setContinueInNewLine();
	}

	@NotNull
	public List<String> getBodyLines() {
		return bodyText;
	}
}
