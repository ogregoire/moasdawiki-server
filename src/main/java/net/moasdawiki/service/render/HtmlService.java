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

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.HttpResponse;
import net.moasdawiki.service.transform.TransformerService;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.WikiPage;
import net.moasdawiki.util.EscapeUtils;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Methods to generate HTML output.
 */
public class HtmlService {

	private static final String ERROR_PAGE_TITLE_KEY = "wiki.errorpage.title";
	private static final String ERROR_PAGE_MESSAGE_KEY = "wiki.errorpage.message";
	private static final String ERROR_PAGE_STARTPAGE_LINK_KEY = "wiki.errorpage.linkToStartpage";

	private final Logger logger;
	private final Settings settings;
	private final Messages messages;
	private final WikiService wikiService;
	private final TransformerService transformerService;

	/**
	 * Constructor.
	 */
	public HtmlService(@NotNull Logger logger, @NotNull Settings settings, @NotNull Messages messages,
					   @NotNull WikiService wikiService, @NotNull TransformerService transformerService) {
		super();
		this.logger = logger;
		this.settings = settings;
		this.messages = messages;
		this.wikiService = wikiService;
		this.transformerService = transformerService;
	}

	/**
	 * Write a HTML page to a HTTP response.
	 */
	@NotNull
	public HttpResponse convertHtml(@NotNull HtmlWriter htmlWriter) {
		// close open tags
		htmlWriter.closeAllTags();

		// generate HTML
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>"); // HTML 5
		sb.append('\n');
		sb.append("<html lang=\"");
		sb.append(getLanguageCode());
		sb.append("\">\n");
		sb.append("<head>\n");

		// page title
		String title = htmlWriter.getTitle();
		if (title != null) {
			title += " | " + settings.getProgramName();
		} else {
			title = settings.getProgramName();
		}
		sb.append("  <title>");
		sb.append(EscapeUtils.escapeHtml(title));
		sb.append("</title>\n");

		// include HTML header lines from configuration file
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

		// HTML body
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

	private String getLanguageCode() {
		String lang = messages.getPureMessage(Messages.MESSAGEFORMAT_LOCALE_KEY);
		if (lang == null) {
			lang = "en";
		}
		return lang;
	}

	/**
	 * Apply transformers to a wiki page and convert it to HTML.
	 */
	@NotNull
	public HttpResponse convertPage(@NotNull WikiPage wikiPage) {
		// Platzhalter füllen und weitere Transformer ausführen
		wikiPage = transformerService.applyTransformations(wikiPage);

		// in HTML umwandeln
		WikiPage2Html html = new WikiPage2Html(settings, messages, wikiService, true);
		HtmlWriter writer = html.generate(wikiPage);
		writer.setTitle(PathUtils.extractWebName(wikiPage.getPagePath()));
		return convertHtml(writer);
	}

	/**
	 * Generate a HTTP redirect response.
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
	 * Generate a web page with a message.
	 */
	@NotNull
	public HttpResponse generateMessagePage(@NotNull String messageKey, Object... arguments) {
		HtmlWriter writer = new HtmlWriter();
		String msg = messages.getMessage(messageKey, arguments);
		writer.htmlText("<b>" + EscapeUtils.escapeHtml(msg) + "</b>");
		return convertHtml(writer);
	}

	/**
	 * Generate an error page.
	 */
	@NotNull
	public HttpResponse generateErrorPage(int statusCode, @NotNull String messageKey, Object... arguments) {
		String msg = messages.getMessage(messageKey, arguments);
		return generateErrorPageWithDetails(statusCode, msg, null);
	}

	/**
	 * Generate an error page with an Exception.
	 */
	@NotNull
	public HttpResponse generateErrorPage(int statusCode, @NotNull Throwable t, @NotNull String messageKey, Object... arguments) {
		String msg = messages.getMessage(messageKey, arguments);
		return generateErrorPageWithDetails(statusCode, msg, t.getClass().getCanonicalName() + ": " + t.getMessage());
	}

	@NotNull
	private HttpResponse generateErrorPageWithDetails(int statusCode, @NotNull String message, @Nullable String details) {
		HtmlWriter writer = new HtmlWriter();
		writer.setTitle(messages.getMessage(ERROR_PAGE_TITLE_KEY));
		String msg = messages.getMessage(ERROR_PAGE_MESSAGE_KEY, message);
		writer.htmlText("<b>" + EscapeUtils.escapeHtml(msg) + "</b>");
		writer.htmlNewLine();
		if (details != null) {
			writer.htmlText(EscapeUtils.escapeHtml(details));
			writer.htmlNewLine();
		}
		writer.htmlNewLine();
		writer.htmlText(messages.getMessage(ERROR_PAGE_STARTPAGE_LINK_KEY));

		HttpResponse result = convertHtml(writer);
		result.statusCode = statusCode;
		return result;
	}
}
