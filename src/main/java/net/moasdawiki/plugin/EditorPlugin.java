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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.server.HttpRequest;
import net.moasdawiki.server.HttpResponse;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.render.HtmlWriter;
import net.moasdawiki.service.render.HtmlWriter.Method;
import net.moasdawiki.service.render.WikiPage2Html;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.WikiText;
import net.moasdawiki.service.wiki.structure.WikiPage;
import net.moasdawiki.util.EscapeUtils;
import net.moasdawiki.util.JavaScriptUtils;
import net.moasdawiki.util.PathUtils;
import net.moasdawiki.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ein einfacher Texteditor zum Editieren einer Wikiseite.
 * 
 * @author Herbert Reiter
 */
public class EditorPlugin implements Plugin {

	private Logger logger;
	private Settings settings;
	private Messages messages;
	private RepositoryService repositoryService;
	private WikiService wikiService;
	private HtmlService htmlService;
	private PluginService pluginService;

	public void setServiceLocator(@NotNull ServiceLocator serviceLocator) {
		this.logger = serviceLocator.getLogger();
		this.settings = serviceLocator.getSettings();
		this.messages = serviceLocator.getMessages();
		this.repositoryService = serviceLocator.getRepositoryService();
		this.wikiService = serviceLocator.getWikiService();
		this.htmlService = serviceLocator.getHtmlService();
		this.pluginService = serviceLocator.getPluginService();
	}

	@Nullable
	@PathPattern(multiValue = { "/edit/.*", "/upload/.*" })
	public HttpResponse handleRequest(@NotNull HttpRequest request) {
		String urlPath = request.urlPath;
		if (urlPath.startsWith("/edit/")) {
			return handleEditRequest(request);
		} else if (urlPath.startsWith("/upload")) {
			return handleUploadRequest(request);
		} else {
			return htmlService.generateErrorPage(400, "EditorPlugin.url.invalid", urlPath);
		}
	}

	/**
	 * Editiert eine Wikiseite. Je nach Requestparameter wird die Editorseite
	 * angezeigt oder ein Kommando ausgeführt.
	 * 
	 * @param request HTTP-Requestdaten, enthält die Formulardaten. Nicht
	 *        <code>null</code>.
	 * @return HTTP-Response, der zum Browser geschickt wird. Nicht
	 *         <code>null</code>.
	 */
	@NotNull
	private HttpResponse handleEditRequest(@NotNull HttpRequest request) {
		try {
			// Präfix "/edit/" wegschneiden
			String pagePath = request.urlPath.substring(5);
			pagePath = EscapeUtils.url2PagePath(pagePath);

			if (request.urlParameters.get("cancel") != null) {
				// Benutzer hat auf Abbrechen geklickt
				return cancelEditing(pagePath);

			} else if (request.urlParameters.get("delete") != null) {
				// Benutzer hat auf Löschen geklickt
				return deleteWikiPage(pagePath);

			} else if (request.urlParameters.get("save") != null) {
				// Benutzer hat auf Speichern geklickt
				String newPagePath = request.urlParameters.get("titleeditor");
				String newWikiText = request.urlParameters.get("contenteditor");
				String fromPos = request.urlParameters.get("fromPos");
				String toPos = request.urlParameters.get("toPos");
				return saveWikiText(pagePath, newPagePath, newWikiText, fromPos, toPos);

			} else {
				// Editor wird neu aufgerufen;
				// pagePath gibt es initialen Pfad in der Titelleiste an;
				// wenn die entsprechende Wikiseite existiert, wird
				// dessen Inhalt in den Editor übernommen, ansonsten wird
				// eine neue Wikiseite erstellt;
				String fromPos = request.urlParameters.get("fromPos");
				String toPos = request.urlParameters.get("toPos");
				return showEditor(pagePath, fromPos, toPos);
			}
		} catch (ServiceException e) {
			return htmlService.generateErrorPage(500, e, "EditorPlugin.error", e.getMessage());
		}
	}

	/**
	 * Wird aufgerufen, wenn der Editor abgebrochen wird. Wechselt zur normalen
	 * Darstellung der Wikiseite bzw. zur Startseite.
	 */
	@NotNull
	private HttpResponse cancelEditing(@Nullable String pagePath) {
		if (pagePath != null) {
			// Seite existiert bereits, diese anzeigen
			return htmlService.generateRedirectToWikiPage(pagePath);
		} else {
			// das Anlegen einer neuen Seite wurde abgebrochen,
			// Startseite anzeigen
			return htmlService.generateRedirectToWikiPage(settings.getStartpagePath());
		}
	}

	/**
	 * Löscht die angegebene Wikiseite und wechselt dann zur Startseite.
	 */
	@NotNull
	private HttpResponse deleteWikiPage(@NotNull String pagePath) throws ServiceException {
		try {
			wikiService.deleteWikiFile(pagePath);
			return htmlService.generateRedirectToWikiPage(settings.getStartpagePath());
		} catch (ServiceException e) {
			logger.write("Error deleting wiki page '" + pagePath + "'", e);
			String msg = messages.getMessage("EditorPlugin.delete.error", e.getMessage());
			throw new ServiceException(msg, e);
		}
	}

	/**
	 * Speichert eine Wikiseite.
	 * 
	 * @param oldPagePath Name der zu bearbeitenden Wikiseite. Wenn
	 *        <code>newPagePath</code> davon abweicht, wird die Wikiseite
	 *        umbenannt. <code>null</code> -> keine bestehende Seite, eine neue
	 *        Wikiseite wird erstellt.
	 * @param newPagePath Vom Benutzer eingegebener Name der Wikiseite. Wenn er
	 *        von <code>oldPagePath</code> abweicht, wird die Wikiseite
	 *        umbenannt. Nicht <code>null</code>. Muss mind. 1 Zeichen
	 *        enthalten.
	 * @param newWikiText Vom Benutzer eingegebener bzw. geänderter Wikitext.
	 *        <code>null</code> -> wird als "" interpretiert.
	 * @param fromPos <code>null</code> -> gesamte Wikiseite.
	 * @param toPos <code>null</code> -> gesamte Wikiseite.
	 * @return Redirect auf die Wikiseite. Nicht <code>null</code>.
	 */
	@NotNull
	private HttpResponse saveWikiText(@Nullable String oldPagePath, @NotNull String newPagePath, @Nullable String newWikiText,
									  @Nullable String fromPos, @Nullable String toPos) throws ServiceException {
		// Ungültigen Seitennamen abfangen
		newPagePath = newPagePath.trim();
		if (newPagePath.length() == 0 || newPagePath.endsWith("/")) {
			logger.write("Cannot save wiki page with invalid name '" + newPagePath + "'");
			String msg = messages.getMessage("EditorPlugin.save.invalidName", newPagePath);
			throw new ServiceException(msg);
		}
		if (newPagePath.charAt(0) != '/') {
			newPagePath = '/' + newPagePath;
		}

		// fehlenden Wikitext abfangen
		if (newWikiText == null) {
			newWikiText = "";
		}

		// Überschreiben einer bestehenden Seite verhindern
		if ((oldPagePath == null || !oldPagePath.equals(newPagePath)) && wikiService.existsWikiFile(newPagePath)) {
			logger.write("Cannot create wiki page '" + newPagePath + "' as there is already a page with the same name");
			String msg = messages.getMessage("EditorPlugin.save.alreadyExisting", newPagePath);
			throw new ServiceException(msg);
		}

		try {
			// weitere Parameter parsen
			Integer fromPosInt = StringUtils.parseInteger(fromPos);
			Integer toPosInt = StringUtils.parseInteger(toPos);

			// ggf. Seite umbenennen;
			// wenn der alte Name mit "/" endet, ist es nur ein
			// Verzeichnisvorschlag und nicht der alte Name einer Wikiseite
			if (oldPagePath != null && !oldPagePath.endsWith("/") && !oldPagePath.equals(newPagePath)) {
				wikiService.deleteWikiFile(oldPagePath);
			}

			// neuen Seiteninhalt schreiben
			WikiText wikiText = new WikiText(newWikiText, fromPosInt, toPosInt);
			wikiService.writeWikiText(newPagePath, wikiText);
		} catch (ServiceException e) {
			logger.write("Error saving wiki page '" + newPagePath + "'", e);
			String msg = messages.getMessage("EditorPlugin.save.error", newPagePath, e.getMessage());
			throw new ServiceException(msg, e);
		}

		// Wiki-Seite anzeigen
		return htmlService.generateRedirectToWikiPage(newPagePath);
	}

	/**
	 * Zeigt den Editor an. Wenn pagePath eine existierende Wikiseite ist, wird
	 * dessen Inhalt in den Editor übernommen. Ansonsten wird eine neue
	 * Wikiseite erstellt.
	 * 
	 * @param pagePath Name der Wikiseite. Nicht <code>null</code>.
	 * @param fromPos Position des ersten Zeichens des Ausschnitts der
	 *        Wikiseite. <code>null</code> -> ganze Wikiseite editieren.
	 * @param toPos Position nach dem letzten Zeichen des Ausschnitts der
	 *        Wikiseite. <code>null</code> -> ganze Wikiseite editieren.
	 */
	@NotNull
	private HttpResponse showEditor(@NotNull String pagePath, @Nullable String fromPos, @Nullable String toPos) throws ServiceException {
		// Inhaltsvorlagen holen und sortieren
		Map<String, String> templates = readTemplates();

		// weitere Parameter parsen
		Integer fromPosInt = StringUtils.parseInteger(fromPos);
		Integer toPosInt = StringUtils.parseInteger(toPos);

		WikiText wikiText = null;
		if (wikiService.existsWikiFile(pagePath)) {
			try {
				// Wikiseite lesen
					wikiText = wikiService.readWikiText(pagePath, fromPosInt, toPosInt);
			} catch (ServiceException e) {
				logger.write("Wiki page '" + pagePath + "' not found", e);
				String msg = messages.getMessage("EditorPlugin.editor.error", pagePath, e.getMessage());
				throw new ServiceException(msg, e);
			}
		} else {
			logger.write("Wiki page '" + pagePath + "' not found, show empty editor to create a new page");
		}

		// Editor generieren
		WikiPage menuPage = getMenuPage();
		HtmlWriter html = generateWikiEditor(pagePath, wikiText, menuPage, templates);
		return htmlService.convertHtml(html);
	}

	/**
	 * Inhaltsvorlagen holen und sortieren.
	 */
	@NotNull
	private Map<String, String> readTemplates() {
		String templatesPagePath = settings.getTemplatesPagePath();
		if (templatesPagePath == null) {
			return Collections.emptyMap();
		}

		try {
			Map<String, String> templates = new TreeMap<>();
			WikiFile templateParentPage = wikiService.getWikiFile(templatesPagePath);
			for (String pagePath : templateParentPage.getChildren()) {
				WikiFile templateWikiFile = wikiService.getWikiFile(pagePath);
				String templateContent = templateWikiFile.getWikiText();

				// erste Textzeile mit Verweis auf Template-Vaterseite
				// entfernen
				final String templateParent = "{{parent:" + templatesPagePath + "}}";
				if (templateContent.startsWith(templateParent)) {
					templateContent = templateContent.substring(templateParent.length());
					if (templateContent.startsWith("\r")) {
						templateContent = templateContent.substring(1);
					}
					if (templateContent.startsWith("\n")) {
						templateContent = templateContent.substring(1);
					}
				}

				templates.put(pagePath, templateContent);
			}
			return templates;
		}
		catch (ServiceException e) {
			logger.write("Error reading template pages", e);
			return Collections.emptyMap();
		}
	}

	/**
	 * Holt die Menü-Seite und wendet die Plugins darauf an.
	 * 
	 * @return <code>null</code>, wenn die Menüseite nicht vorhanden ist.
	 */
	@Nullable
	private WikiPage getMenuPage() {
		String navigationPagePath = settings.getNavigationPagePath();
		if (navigationPagePath == null) {
			return null;
		}

		try {
			WikiFile menuWikiFile = wikiService.getWikiFile(navigationPagePath);
			WikiPage wikiPage = new WikiPage(null, menuWikiFile.getWikiPage(), null, null);
			return pluginService.applyTransformations(wikiPage);
		} catch (ServiceException e) {
			logger.write("Error reading menu wiki page", e);
			return null;
		}
	}

	/**
	 * Zeigt den Seiteneditor an.
	 * 
	 * @param pagePath Name der Wikidatei. Nicht <code>null</code>.
	 * @param wikiText Inhalt der Wikidatei. <code>null</code> --> Neue
	 *        Wikidatei soll erstellt werden.
	 * @param menuPage Wikiseite, die das Menü (Navigation) enthält.
	 *        <code>null</code> -> kein Menü.
	 * @param templates Liste alle Vorlagentexte. Nicht <code>null</code>.
	 */
	@NotNull
	private HtmlWriter generateWikiEditor(@NotNull String pagePath, @Nullable WikiText wikiText, @Nullable WikiPage menuPage, @NotNull Map<String, String> templates) {
		HtmlWriter writer = new HtmlWriter();
		if (wikiText != null) {
			writer.setTitle(PathUtils.extractWebName(pagePath));
			writer.setBodyParams("onload=\"initPage(false)\"");
		} else {
			String msg = messages.getMessage("EditorPlugin.editor.title.newPage");
			writer.setTitle(msg);
			writer.setBodyParams("onload=\"initPage(true)\"");
		}

		// JavaScript-Konstanten ausgeben
		writer.openTag("script", "type=\"text/javascript\"");
		writer.htmlText("\n");
		writer.htmlText("  var folderPath = '" + JavaScriptUtils.escapeJavaScript(PathUtils.extractWebFolder(pagePath)) + "';\n");
		writer.closeTag(); // script

		// Menü ausgeben
		writer.openDivTag("menu"); // in Menü-Kontext einbetten
		if (menuPage != null) {
			WikiPage2Html html = new WikiPage2Html(settings, messages, wikiService, false);
			writer.addHtmlWriter(html.generate(menuPage));
		}
		writer.closeTag(); // menu

		writer.openDivTag("wikipage editor"); // in Wikiseiten-Kontext einbetten
		writer.setContinueInNewLine();

		// Name der Wikiseite in URL kodieren, um Umbenennung zu ermöglichen
		String formPath = PathUtils.concatWebPaths("/edit/", pagePath);
		writer.openFormTag("EditForm", EscapeUtils.encodeUrl(EscapeUtils.pagePath2Url(formPath)), Method.POST);
		writer.setContinueInNewLine();

		// Metadaten ausgeben
		if (wikiText != null && wikiText.getFromPos() != null) {
			writer.htmlText("<input type=\"hidden\" name=\"fromPos\" value=\"" + wikiText.getFromPos() + "\" />");
			writer.setContinueInNewLine();
		}
		if (wikiText != null && wikiText.getToPos() != null) {
			writer.htmlText("<input type=\"hidden\" name=\"toPos\" value=\"" + wikiText.getToPos() + "\" />");
			writer.setContinueInNewLine();
		}

		// Seitenname-Editor ausgeben
		String pagePathInEditor = pagePath;
		if ("/".equals(pagePathInEditor)) {
			pagePathInEditor = ""; // soll nicht null sein
		}
		String titleeditorHint = messages.getMessage("EditorPlugin.editor.input.title");
		writer.htmlText("<input type=\"text\" class=\"titleinput\" name=\"titleeditor\" placeholder=\"" + EscapeUtils.escapeHtml(titleeditorHint)
				+ "\" value=\"" + EscapeUtils.escapeHtml(pagePathInEditor) + "\" />");
		writer.setContinueInNewLine();

		// Wiki-Editor ausgeben
		String contenteditorHint = messages.getMessage("EditorPlugin.editor.input.content");
		writer.openTag("textarea",
				"cols=\"80\" rows=\"10\" name=\"contenteditor\" class=\"textinput\" placeholder=\"" + EscapeUtils.escapeHtml(contenteditorHint) + "\"");
		// damit Leerzeilen oben nicht vom Browser entfernt werden
		writer.htmlText("\n");
		if (wikiText != null) {
			writer.htmlText(EscapeUtils.escapeHtml(wikiText.getText()));
		}
		writer.closeTag(); // textarea
		writer.setContinueInNewLine();

		// Bedienpanel ausgeben
		writer.openDivTag("controlarea", "id=\"controlarea\"");
		writer.setContinueInNewLine();

		writer.openDivTag("section"); // Block 1
		String saveTitle = messages.getMessage("EditorPlugin.editor.input.save");
		writer.htmlText("<button type=\"submit\" name=\"save\" class=\"save\">" + EscapeUtils.escapeHtml(saveTitle) + "</button>");
		String cancelTitle = messages.getMessage("EditorPlugin.editor.input.cancel");
		writer.htmlText("<button type=\"submit\" name=\"cancel\" class=\"cancel\">" + EscapeUtils.escapeHtml(cancelTitle) + "</button>");
		writer.closeTag(); // div Block 1

		writer.openDivTag("section"); // Block 2
		String disabledAttribute = "";
		if (!wikiService.existsWikiFile(pagePath)) {
			disabledAttribute = " disabled=\"true\"";
		}
		String deleteTitle = messages.getMessage("EditorPlugin.editor.input.delete");
		writer.htmlText("<button type=\"button\" name=\"deleteButton\" class=\"delete\"" + " onclick=\"sendDelete()\"" + disabledAttribute
				+ ">" + EscapeUtils.escapeHtml(deleteTitle) + "</button>");
		writer.closeTag(); // div Block 2

		writer.openDivTag("section"); // Block 3
		writer.openTag("select", "name=\"TemplateSelect\" class=\"TemplateSelect\"" + " onchange=\"insertTemplate(this.selectedIndex)\"");
		String templateSelectOption = messages.getMessage("EditorPlugin.editor.input.templateSelect");
		writer.htmlText("<option>" + EscapeUtils.escapeHtml(templateSelectOption) + "</option>");
		writer.closeTag(); // select
		String helpTitle = messages.getMessage("EditorPlugin.editor.help");
		writer.htmlText("<a href=\"/view/wiki/syntax/\" target=\"_blank\">" + EscapeUtils.escapeHtml(helpTitle) + "</a>");
		writer.closeTag(); // div Block 3

		String uploadHint = messages.getMessage("EditorPlugin.editor.input.upload.hint");
		writer.openDivTag("uploadarea", "id=\"uploadarea\" onclick=\"showPanel('uploadPanelId')\" title=\"" + EscapeUtils.escapeHtml(uploadHint) + "\"");
		String uploadTitle = messages.getMessage("EditorPlugin.editor.input.upload.title");
		writer.htmlText(EscapeUtils.escapeHtml(uploadTitle));
		writer.closeTag(); // uploadarea
		writer.closeTag(); // div controlarea

		writer.closeTag(); // form
		writer.closeTag(); // wikipage

		writeUploadPanel(writer);
		putTemplateList(writer, templates);

		return writer;
	}

	private void writeUploadPanel(@NotNull HtmlWriter writer) {
		writer.setContinueInNewLine();
		writer.openDivTag(null, "id=\"uploadPanelId\" style=\"display: none;\"");

		writer.openDivTag("panelbackground");
		writer.closeTag();

		writer.openDivTag("panel uploadpanel");
		writer.openDivTag("header");
		String title = messages.getMessage("EditorPlugin.editor.uploadPanel.title");
		writer.htmlText(EscapeUtils.escapeHtml(title));
		writer.closeTag(); // div.header

		writer.openDivTag("body");
		writer.openFormTag("uploadForm");

		writer.openDivTag("section", "id=\"fileSelectId\"");
		String fileLabel = messages.getMessage("EditorPlugin.editor.uploadPanel.file");
		writer.htmlText("<label for=\"fileInputId\">" + EscapeUtils.escapeHtml(fileLabel) + "</label>");
		writer.htmlNewLine();
		writer.htmlText("<input type=\"file\" id=\"fileInputId\" name=\"fileSelect\" />");
		writer.closeTag(); // div.section

		writer.openDivTag("section");
		String repositoryLabel = messages.getMessage("EditorPlugin.editor.uploadPanel.repositoryPath");
		writer.htmlText("<label for=\"uploadRepositoryPathId\">" + EscapeUtils.escapeHtml(repositoryLabel) + "</label>");
		writer.htmlNewLine();
		writer.htmlText("<input type=\"text\" id=\"uploadRepositoryPathId\" name=\"uploadRepositoryPath\" />");
		writer.closeTag(); // div.section

		writer.openDivTag("section");
		writer.htmlText("<input type=\"checkbox\" id=\"generateImageTagId\" name=\"generateImageTag\" checked=\"true\">");
		String imageTagLabel = messages.getMessage("EditorPlugin.editor.uploadPanel.imageTag");
		writer.htmlText("<label id=\"generateImageTagLabelId\" for=\"generateImageTagId\">" + EscapeUtils.escapeHtml(imageTagLabel) + "</label>");
		writer.htmlNewLine();
		writer.htmlText("<input type=\"checkbox\" id=\"generateFileTagId\" name=\"generateFileTag\" checked=\"true\">");
		String fileTagLabel = messages.getMessage("EditorPlugin.editor.uploadPanel.fileTag");
		writer.htmlText("<label id=\"generateFileTagLabelId\" for=\"generateFileTagId\">" + EscapeUtils.escapeHtml(fileTagLabel) + "</label>");
		writer.closeTag(); // div.section

		writer.openDivTag("footer");
		String saveButton = messages.getMessage("EditorPlugin.editor.uploadPanel.save");
		writer.htmlText("<button type=\"button\" name=\"uploadButton\" class=\"save\" onclick=\"handleFileUpload('uploadPanelId')\">"
				+ EscapeUtils.escapeHtml(saveButton) + "</button>");
		String cancelButton = messages.getMessage("EditorPlugin.editor.uploadPanel.cancel");
		writer.htmlText("<button type=\"button\" name=\"cancelButton\" class=\"cancel\" onclick=\"hidePanel('uploadPanelId')\">"
				+ EscapeUtils.escapeHtml(cancelButton) + "</button>");
		writer.closeTag(); // div.footer

		writer.closeTag(); // form
		writer.closeTag(); // div.body
		writer.closeTag(); // div.panel

		writer.closeTag(); // div#uploadPanelId
	}

	/**
	 * Kodiert die Template-Liste als JSON-String in die HTML-Seite. Die Daten
	 * werden später per JavaScript ausgewertet.
	 * 
	 * @param templates enthält die Seitenvorlagen in der Form Name -> Inhalt.
	 */
	private void putTemplateList(@NotNull HtmlWriter writer, @NotNull Map<String, String> templates) {
		// Template-Namen aufbereiten und sortieren
		List<Template> templateList = new ArrayList<>();
		for (String templateName : templates.keySet()) {
			String name = PathUtils.extractWebName(templateName);
			Template t = new Template(name, templates.get(templateName));
			templateList.add(t);
		}
		Collections.sort(templateList);

		// Liste in einen JSON-String umwandeln
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < templateList.size(); i++) {
			if (i >= 1) {
				sb.append(", ");
			}
			Template t = templateList.get(i);
			sb.append("{\"name\": \"");
			sb.append(JavaScriptUtils.escapeJavaScript(t.getName()));
			sb.append("\", \"content\": \"");
			sb.append(JavaScriptUtils.escapeJavaScript(t.getContent()));
			sb.append("\"}");
		}
		sb.append(']');

		// Inhaltsvorlagen in JavaScript-Konstanten ablegen
		writer.setContinueInNewLine();
		writer.openTag("script", "type=\"text/javascript\"");
		writer.htmlText("\n");
		writer.htmlText("var templates = " + sb.toString() + ";\n");
		writer.closeTag(); // script
	}

	/**
	 * Lädt eine Datei ins Repository hoch.
	 * 
	 * @param request HTTP-Requestdaten, enthält die Formulardaten. Nicht
	 *        <code>null</code>.
	 * @return HTTP-Response, der zum Browser geschickt wird. Nicht
	 *         <code>null</code>.
	 */
	@NotNull
	private HttpResponse handleUploadRequest(@NotNull HttpRequest request) {
		// Präfix "/upload" wegschneiden
		String filePath = request.urlPath.substring(7).trim();

		// ungültigen Dateinamen abfangen
		if (filePath.length() == 0 || filePath.endsWith("/")) {
			logger.write("Upload file name '" + filePath + "' is invalid");
			String msg = messages.getMessage("EditorPlugin.upload.invalidName", filePath);
			return generateJsonResponse(400, msg);
		}
		// auf Rückwärtsnavigation prüfen
		if (filePath.contains("..")) {
			logger.write("Upload file name '" + filePath + "' contains illegal parent navigation");
			String msg = messages.getMessage("EditorPlugin.upload.parentNavigation", filePath);
			return generateJsonResponse(400, msg);
		}
		if (filePath.charAt(0) != '/') {
			filePath = '/' + filePath;
		}

		try {
			// Datei speichern
			if (repositoryService.getFile(filePath) != null) {
				logger.write("Upload file name '" + filePath + "' already exists");
				String msg = messages.getMessage("EditorPlugin.upload.alreadyExisting", filePath);
				throw new Exception(msg);
			}
			AnyFile anyFile = new AnyFile(filePath);
			repositoryService.writeBinaryFile(anyFile, request.httpBody, null);
			logger.write("File '" + filePath + "' successfully uploaded");
		} catch (Exception e) {
			logger.write("Error uploading file '" + filePath + "'", e);
			return generateJsonResponse(500, e.getMessage());
		}

		return generateJsonResponse(200, "File upload successful: " + filePath);
	}

	private HttpResponse generateJsonResponse(int statusCode, @NotNull String jsonText) {
		HttpResponse result = new HttpResponse();
		result.setStatusCode(statusCode);
		result.setContentType(HttpResponse.CONTENT_TYPE_JSON_UTF8);
		result.setContent(JavaScriptUtils.generateJson(jsonText));
		return result;
	}

	/**
	 * Hilfsklasse zum Aufnehmen eines Template-Namen und -Inhalts. Wird
	 * benötigt, um die Templates nach Name zu sortieren.
	 */
	static class Template implements Comparable<Template> {
		@NotNull
		private final String name;

		@NotNull
		private final String content;

		public Template(@NotNull String name, @NotNull String content) {
			super();
			this.name = name;
			this.content = content;
		}

		@NotNull
		public String getName() {
			return name;
		}

		@NotNull
		public String getContent() {
			return content;
		}

		public int compareTo(@NotNull Template o) {
			return name.compareTo(o.name);
		}
	}
}
