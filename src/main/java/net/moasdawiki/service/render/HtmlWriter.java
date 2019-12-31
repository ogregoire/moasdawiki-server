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

package net.moasdawiki.service.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Erzeugt formatiertes HTML. Transportklasse.
 * 
 * @author Herbert Reiter
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
	 * Inhalt des HTML-Header. Nicht null.
	 */
	@NotNull
	private final ArrayList<String> headText;

	/**
	 * Optionale Parameter des body-Tags. null -> keine Parameter.
	 */
	@Nullable
	private String bodyParams;

	/**
	 * Inhalt des HTML-Body. Nicht null.
	 */
	@NotNull
	private final ArrayList<String> bodyText;

	/**
	 * Stack der geöffneten HTML-Tags. Nicht null.
	 */
	@NotNull
	private final Stack<String> tagStack;

	/**
	 * Weiteren HTML-Text in eine neue Zeile schreiben?
	 */
	private boolean continueInNewLine;

	public HtmlWriter() {
		super();
		headText = new ArrayList<>();
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
	 * Fügt eine Weiterleitung auf eine andere URL in den HTML-Header ein.
	 * 
	 * @param url muss korrekten String enthalten.
	 * @param sec Anzahl Sekunden, nach denen die Weiterleitung erfolgen soll.
	 */
	public void setRedirect(@NotNull String url, int sec) {
		headText.add("<meta http-equiv=\"Refresh\" content=\"" + sec + "; URL=" + url + "\" />");
	}

	/**
	 * Bindet eine CSS-Datei ein.
	 * 
	 * @param url Adresse der CSS-Datei.
	 */
	public void addStylesheet(@NotNull String url) {
		headText.add("<link rel=\"stylesheet\"" + " type=\"text/css\" href=\"" + url + "\" />");
	}

	/**
	 * Bindet eine JavaScript-Datei ein.
	 * 
	 * @param url Adresse der JavaScript-Datei.
	 */
	public void addJavaScript(@NotNull String url) {
		headText.add("<script type=\"text/javascript\" src=\"" + url + "\"></script>");
	}

	/**
	 * Gibt die Header-Zeilen zurück.
	 * 
	 * @return Liste der Header-Zeilen. Nicht null.
	 */
	public List<String> getHeaderLines() {
		return headText;
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
		String methodName;
		if (method == Method.GET) {
			methodName = "get";
		} else {
			methodName = "post";
		}

		String params = "method=\"" + EscapeUtils.escapeHtml(methodName)
				+ "\" action=\"" + EscapeUtils.escapeHtml(action) + "\" "
				+ "enctype=\"application/x-www-form-urlencoded; charset=utf-8\"";
		if (name != null) {
			params += " name=\"" + EscapeUtils.escapeHtml(name) + '\"';
		}
		return openTag("form", params);
	}

	public void closeTag() {
		String tag = tagStack.pop();
		htmlText("</" + tag + ">");
	}

	public int getStackDepth() {
		return tagStack.size();
	}

	/**
	 * Schließt offene Tags bis zur angegebenen Stacktiefe. 0 = alle Tags
	 * schließen.
	 */
	public void closeTags(int stackDepth) {
		while (tagStack.size() > stackDepth) {
			closeTag();
		}
	}

	public void closeAllTags() {
		while (!tagStack.isEmpty()) {
			closeTag();
		}
	}

	/**
	 * Gibt das zuletzt geöffnete, noch offene Tag zurück. Wenn kein Tag offen
	 * ist, wird null zurückgegeben.
	 */
	@Nullable
	public String getCurrentTag() {
		return getCurrentTag(0);
	}

	/**
	 * Gibt ein zuvor geöffnetes Tag zurück. Der Parameter downStack gibt an,
	 * wie viele Schritte vorher das Tag geöffnet wurde.
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
	public void addHtmlWriter(HtmlWriter htmlWriter) {
		if (htmlWriter == null) {
			return;
		}
		htmlWriter.closeAllTags(); // evtl. offene Tags schließen
		for (int i = 0; i < htmlWriter.bodyText.size(); i++) {
			setContinueInNewLine();
			htmlText(htmlWriter.bodyText.get(i));
		}
		setContinueInNewLine();
	}

	@NotNull
	public List<String> getBodyLines() {
		return bodyText;
	}
}
