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

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.plugin.PluginService;
import net.moasdawiki.server.HttpResponse;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.WikiPage;
import net.moasdawiki.util.EscapeUtils;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hilfsmethoden zum Generieren von HTML-Seiten.
 * 
 * @author Herbert Reiter
 */
public class HtmlService {

	private static final String HTML_DOCUMENT_TYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";

	private final Logger logger;
	private final Settings settings;
	private final Messages messages;
	private final WikiService wikiService;
	private final PluginService pluginService;

	/**
	 * Konstruktor.
	 */
	public HtmlService(@NotNull Logger logger, @NotNull Settings settings, @NotNull Messages messages, @NotNull WikiService wikiService, @NotNull PluginService pluginService) {
		super();
		this.logger = logger;
		this.settings = settings;
		this.messages = messages;
		this.wikiService = wikiService;
		this.pluginService = pluginService;
	}

	/**
	 * Konvertiert eine HTML-Seite in ein HTTP-Response.
	 */
	@NotNull
	public HttpResponse convertHtml(@NotNull HtmlWriter htmlWriter) {
		// evtl. offene Tags schließen
		htmlWriter.closeAllTags();

		// HTML ausgeben
		StringBuilder sb = new StringBuilder();
		sb.append(HTML_DOCUMENT_TYPE);
		sb.append('\n');
		sb.append("<html>\n");
		sb.append("<head>\n");

		// Seitentitel ausgeben
		String title = htmlWriter.getTitle();
		if (title != null) {
			title += " | " + settings.getProgramName();
		} else {
			title = settings.getProgramName();
		}
		sb.append("  <title>");
		sb.append(EscapeUtils.escapeHtml(title));
		sb.append("</title>\n");

		for (String line : htmlWriter.getHeaderLines()) {
			sb.append("  ");
			sb.append(line);
			sb.append('\n');
		}

		// konfigurierte HTML-Headerzeilen aus Wikidatei ausgeben
		try {
			String htmlHeaderPagePath = settings.getHtmlHeaderPagePath();
			if (htmlHeaderPagePath != null) {
				WikiFile htmlHeaderFile = wikiService.getWikiFile(htmlHeaderPagePath);
				sb.append(htmlHeaderFile.getWikiText());
			}
		}
		catch (ServiceException e) {
			logger.write("Error reading HTML header page, ignoring it");
		}

		sb.append("</head>\n");

		// HTML-Body ausgeben
		sb.append("<body");
		if (htmlWriter.getBodyParams() != null) {
			sb.append(' ');
			sb.append(htmlWriter.getBodyParams());
		}
		sb.append(">\n");
		for (String line : htmlWriter.getBodyLines()) {
			sb.append("  ");
			sb.append(line);
			sb.append('\n');
		}
		sb.append("</body>\n");
		sb.append("</html>\n");

		HttpResponse response = new HttpResponse();
		response.setContent(sb.toString());
		return response;
	}

	/**
	 * Wandelt eine Wikiseite inkl. Navigation in die HTML-Darstellung um und
	 * gibt sie aus.
	 */
	@NotNull
	public HttpResponse convertPage(@NotNull WikiPage wikiPage) {
		// Platzhalter füllen und weitere Transformationen durch Plugins
		wikiPage = pluginService.applyTransformations(wikiPage);

		// in HTML umwandeln
		WikiPage2Html html = new WikiPage2Html(settings, messages, wikiService, true);
		HtmlWriter writer = html.generate(wikiPage);
		writer.setTitle(PathUtils.extractWebName(wikiPage.getPagePath()));
		return convertHtml(writer);
	}

	/**
	 * Erzeugt einen HTTP-Redirect auf eine Wikiseite. Thread-safe.
	 */
	@NotNull
	public HttpResponse generateRedirectToWikiPage(@NotNull String pagePath) {
		HttpResponse response = new HttpResponse();
		String url = PathUtils.concatWebPaths("/view/", pagePath);
		url = EscapeUtils.pagePath2Url(url);
		response.setRedirectUrl(url);
		return response;
	}

	/**
	 * Generiert eine Seite mit einem Hinweistext. Thread-safe.
	 */
	@NotNull
	public HttpResponse generateMessagePage(@NotNull String messageKey, Object... arguments) {
		HtmlWriter writer = new HtmlWriter();
		String msg = messages.getMessage(messageKey, arguments);
		writer.htmlText("<b>" + EscapeUtils.escapeHtml(msg) + "</b>");
		return convertHtml(writer);
	}

	/**
	 * Generiert eine Fehlerseite. Thread-safe.
	 */
	@NotNull
	public HttpResponse generateErrorPage(int statusCode, @NotNull String messageKey, Object... arguments) {
		String msg = messages.getMessage(messageKey, arguments);
		return generateErrorPageWithDetails(statusCode, msg, null);
	}

	/**
	 * Generiert eine Fehlerseite mit einer Exception. Thread-safe.
	 */
	@NotNull
	public HttpResponse generateErrorPage(int statusCode, @NotNull Throwable t, @NotNull String messageKey, Object... arguments) {
		String msg = messages.getMessage(messageKey, arguments);
		return generateErrorPageWithDetails(statusCode, msg, t.getClass().getCanonicalName() + ": " + t.getMessage());
	}

	@NotNull
	private HttpResponse generateErrorPageWithDetails(int statusCode, @NotNull String message, @Nullable String details) {
		HtmlWriter writer = new HtmlWriter();
		writer.setTitle(messages.getMessage("wiki.errorpage.title"));
		String msg = messages.getMessage("wiki.errorpage.message", message);
		writer.htmlText("<b>" + EscapeUtils.escapeHtml(msg) + "</b>");
		writer.htmlNewLine();
		if (details != null) {
			writer.htmlText(EscapeUtils.escapeHtml(details));
			writer.htmlNewLine();
		}
		writer.htmlNewLine();
		writer.htmlText(messages.getMessage("wiki.errorpage.linkToStartpage"));

		HttpResponse result = convertHtml(writer);
		result.setStatusCode(statusCode);
		return result;
	}
}
