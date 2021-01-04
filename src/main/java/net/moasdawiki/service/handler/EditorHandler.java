/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
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

package net.moasdawiki.service.handler;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.HttpResponse;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.render.HtmlWriter;
import net.moasdawiki.service.render.HtmlWriter.Method;
import net.moasdawiki.service.render.WikiPage2Html;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.transform.TransformerService;
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

import java.util.*;

/**
 * A simple text editor to edit a wiki page.
 */
public class EditorHandler {

    private static final String ERROR_KEY = "EditorHandler.error";
    private static final String DELETE_ERROR_KEY = "EditorHandler.delete.error";
    private static final String DELETE_CONFIRMATION_KEY = "EditorHandler.delete.confirmation";
    private static final String SAVE_INVALID_NAME_KEY = "EditorHandler.save.invalidName";
    private static final String SAVE_ALREADY_EXISTING_KEY = "EditorHandler.save.alreadyExisting";
    private static final String SAVE_ERROR_KEY = "EditorHandler.save.error";
    private static final String EDITOR_ERROR_KEY = "EditorHandler.editor.error";
    private static final String EDITOR_TITLE_NEW_PAGE_KEY = "EditorHandler.editor.title.newPage";
    private static final String EDITOR_INPUT_TITLE_KEY = "EditorHandler.editor.input.title";
    private static final String EDITOR_INPUT_CONTENT_KEY = "EditorHandler.editor.input.content";
    private static final String EDITOR_INPUT_SAVE_KEY = "EditorHandler.editor.input.save";
    private static final String EDITOR_INPUT_CANCEL_KEY = "EditorHandler.editor.input.cancel";
    private static final String EDITOR_INPUT_DELETE_KEY = "EditorHandler.editor.input.delete";
    private static final String EDITOR_INPUT_TEMPLATE_SELECT_KEY = "EditorHandler.editor.input.templateSelect";
    private static final String EDITOR_INPUT_UPLOAD_HINT_KEY = "EditorHandler.editor.input.upload.hint";
    private static final String EDITOR_INPUT_UPLOAD_TITLE_KEY = "EditorHandler.editor.input.upload.title";
    private static final String EDITOR_HELP_KEY = "EditorHandler.editor.help";
    private static final String EDITOR_UPLOADPANEL_TITLE_KEY = "EditorHandler.editor.uploadPanel.title";
    private static final String EDITOR_UPLOADPANEL_FILE_KEY = "EditorHandler.editor.uploadPanel.file";
    private static final String EDITOR_UPLOADPANEL_REPOSITORY_PATH_KEY = "EditorHandler.editor.uploadPanel.repositoryPath";
    private static final String EDITOR_UPLOADPANEL_IMAGE_TAG_KEY = "EditorHandler.editor.uploadPanel.imageTag";
    private static final String EDITOR_UPLOADPANEL_FILE_TAG_KEY = "EditorHandler.editor.uploadPanel.fileTag";
    private static final String EDITOR_UPLOADPANEL_SAVE_KEY = "EditorHandler.editor.uploadPanel.save";
    private static final String EDITOR_UPLOADPANEL_CANCEL_KEY = "EditorHandler.editor.uploadPanel.cancel";
    private static final String UPLOAD_NO_FILE_SELECTED_KEY = "EditorHandler.upload.no-file-selected";
    private static final String UPLOAD_MULTIPLE_FILES_SELECTED_KEY = "EditorHandler.upload.multiple-files-selected";
    private static final String UPLOAD_FILE_TOO_BIG_KEY = "EditorHandler.upload.file-too-big";
    private static final String UPLOAD_INVALID_NAME_KEY = "EditorHandler.upload.invalidName";
    private static final String UPLOAD_PARENT_NAVIGATION_KEY = "EditorHandler.upload.parentNavigation";
    private static final String UPLOAD_ALREADY_EXISTING_KEY = "EditorHandler.upload.alreadyExisting";

    private final Logger logger;
    private final Settings settings;
    private final Messages messages;
    private final RepositoryService repositoryService;
    private final WikiService wikiService;
    private final TransformerService transformerService;
    private final HtmlService htmlService;

    /**
     * Constructor.
     */
    public EditorHandler(@NotNull Logger logger, @NotNull Settings settings, @NotNull Messages messages,
                         @NotNull RepositoryService repositoryService, @NotNull WikiService wikiService,
                         @NotNull TransformerService transformerService, @NotNull HtmlService htmlService) {
        this.logger = logger;
        this.settings = settings;
        this.messages = messages;
        this.repositoryService = repositoryService;
        this.wikiService = wikiService;
        this.transformerService = transformerService;
        this.htmlService = htmlService;
    }

    /**
     * Handles all requests regarding the editor page.
     * <p>
     * URL: /edit/pagename
     */
    @NotNull
    public HttpResponse handleEditRequest(@NotNull String urlPath, @NotNull Map<String, String> urlParameters) {
        try {
            // cut off "/edit/" prefix
            String pagePath = urlPath.substring(5);
            pagePath = EscapeUtils.url2PagePath(pagePath);

            if (urlParameters.get("cancel") != null) {
                // user has clicked cancel button
                return cancelEditing(pagePath);

            } else if (urlParameters.get("delete") != null) {
                // user has clicked delete button
                return deleteWikiPage(pagePath);

            } else if (urlParameters.get("save") != null) {
                // user has clicked save button
                String newPagePath = urlParameters.get("titleeditor");
                String newWikiText = urlParameters.get("contenteditor");
                String fromPos = urlParameters.get("fromPos");
                String toPos = urlParameters.get("toPos");
                return saveWikiText(pagePath, newPagePath, newWikiText, fromPos, toPos);

            } else {
                // No action parameters -> show editor page
                String fromPos = urlParameters.get("fromPos");
                String toPos = urlParameters.get("toPos");
                return showEditor(pagePath, fromPos, toPos);
            }
        } catch (ServiceException e) {
            return htmlService.generateErrorPage(500, e, ERROR_KEY, e.getMessage());
        }
    }

    /**
     * Closes the editor, redirects to the page view.
     */
    @NotNull
    private HttpResponse cancelEditing(@Nullable String pagePath) {
        if (pagePath != null) {
            // wiki page already exists -> show wiki page
            return htmlService.generateRedirectToWikiPage(pagePath);
        } else {
            // no existing wiki page -> show start page
            return htmlService.generateRedirectToWikiPage(settings.getStartpagePath());
        }
    }

    /**
     * Deletes the wiki page and redirects to the start page.
     */
    @NotNull
    private HttpResponse deleteWikiPage(@NotNull String pagePath) throws ServiceException {
        try {
            wikiService.deleteWikiFile(pagePath);
            return htmlService.generateRedirectToWikiPage(settings.getStartpagePath());
        } catch (ServiceException e) {
            logger.write("Error deleting wiki page '" + pagePath + "'", e);
            String msg = messages.getMessage(DELETE_ERROR_KEY, e.getMessage());
            throw new ServiceException(msg, e);
        }
    }

    /**
     * Saves the editor content and redirects to the page view.
     *
     * @param oldPagePath Name of the wiki page to be edited.
     *                    null -> create new wiki page.
     * @param newPagePath New name of the wiki page in the page title bar.
     *                    If the user changes the wiki page name its file name is also renamed.
     *                    Must have at least one character.
     * @param newWikiText Wiki page content.
     *                    null -> empty (= "").
     * @param fromPos     null -> whole wiki page.
     * @param toPos       null -> whole wiki page.
     */
    @NotNull
    private HttpResponse saveWikiText(@Nullable String oldPagePath, @NotNull String newPagePath, @Nullable String newWikiText,
                                      @Nullable String fromPos, @Nullable String toPos) throws ServiceException {
        // catch invalid page name
        newPagePath = newPagePath.trim();
        if (newPagePath.length() == 0 || newPagePath.endsWith("/")) {
            logger.write("Cannot save wiki page with invalid name '" + newPagePath + "'");
            String msg = messages.getMessage(SAVE_INVALID_NAME_KEY, newPagePath);
            throw new ServiceException(msg);
        }
        if (newPagePath.charAt(0) != '/') {
            newPagePath = '/' + newPagePath;
        }

        // empty wiki content
        if (newWikiText == null) {
            newWikiText = "";
        }

        // catch renaming to an existing page
        if ((oldPagePath == null || !oldPagePath.equals(newPagePath)) && wikiService.existsWikiFile(newPagePath)) {
            logger.write("Cannot create wiki page '" + newPagePath + "' as there is already a page with the same name");
            String msg = messages.getMessage(SAVE_ALREADY_EXISTING_KEY, newPagePath);
            throw new ServiceException(msg);
        }

        try {
            // parse other parameters
            Integer fromPosInt = StringUtils.parseInteger(fromPos);
            Integer toPosInt = StringUtils.parseInteger(toPos);

            // delete old wiki page if it is renamed;
            if (oldPagePath != null && !oldPagePath.endsWith("/") && !oldPagePath.equals(newPagePath)) {
                wikiService.deleteWikiFile(oldPagePath);
            }

            // save wiki page
            WikiText wikiText = new WikiText(newWikiText, fromPosInt, toPosInt);
            wikiService.writeWikiText(newPagePath, wikiText);
        } catch (ServiceException e) {
            logger.write("Error saving wiki page '" + newPagePath + "'", e);
            String msg = messages.getMessage(SAVE_ERROR_KEY, newPagePath, e.getMessage());
            throw new ServiceException(msg, e);
        }

        return htmlService.generateRedirectToWikiPage(newPagePath);
    }

    /**
     * Shows the wiki editor.
     *
     * @param pagePath wiki page path
     * @param fromPos  position of first character for section editing;
     *                 null -> edit whole wiki page.
     * @param toPos    position after the last character for section editing;
     *                 null -> edit whole wiki page.
     */
    @NotNull
    private HttpResponse showEditor(@NotNull String pagePath, @Nullable String fromPos, @Nullable String toPos) throws ServiceException {
        Map<String, String> templates = readTemplates();
        Integer fromPosInt = StringUtils.parseInteger(fromPos);
        Integer toPosInt = StringUtils.parseInteger(toPos);

        WikiText wikiText = null;
        if (wikiService.existsWikiFile(pagePath)) {
            try {
                wikiText = wikiService.readWikiText(pagePath, fromPosInt, toPosInt);
            } catch (ServiceException e) {
                logger.write("Wiki page '" + pagePath + "' not found", e);
                String msg = messages.getMessage(EDITOR_ERROR_KEY, pagePath, e.getMessage());
                throw new ServiceException(msg, e);
            }
        } else {
            logger.write("Wiki page '" + pagePath + "' not found, show empty editor to create a new page");
        }

        WikiPage menuPage = getMenuPage();
        HtmlWriter html = generateWikiEditor(pagePath, wikiText, menuPage, templates);
        return htmlService.convertHtml(html);
    }

    /**
     * Reads the wiki templates and sorts them by name.
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

                // remove first line with parent link to template page
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
        } catch (ServiceException e) {
            logger.write("Error reading template pages", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Read navigation page and apply transformers.
     *
     * @return null -> navigation page not available
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
            return transformerService.applyTransformations(wikiPage);
        } catch (ServiceException e) {
            logger.write("Error reading menu wiki page", e);
            return null;
        }
    }

    /**
     * Renders the editor page.
     */
    @NotNull
    private HtmlWriter generateWikiEditor(@NotNull String pagePath, @Nullable WikiText wikiText, @Nullable WikiPage menuPage, @NotNull Map<String, String> templates) {
        HtmlWriter writer = new HtmlWriter();
        if (wikiText != null) {
            writer.setTitle(PathUtils.extractWebName(pagePath));
            writer.setBodyParams("onload=\"initPage(false)\"");
        } else {
            String msg = messages.getMessage(EDITOR_TITLE_NEW_PAGE_KEY);
            writer.setTitle(msg);
            writer.setBodyParams("onload=\"initPage(true)\"");
        }

        // JavaScript constants
        writer.openTag("script", "type=\"text/javascript\"");
        writer.htmlText("\n");
        writer.htmlText("  var folderPath = '" + JavaScriptUtils.escapeJavaScript(PathUtils.extractWebFolder(pagePath)) + "';\n");
        writer.htmlText("  var deleteConfirmationMsg = '" + JavaScriptUtils.escapeJavaScript(messages.getMessage(DELETE_CONFIRMATION_KEY)) + "';\n");
        writer.htmlText("  var uploadNoFileMsg = '" + JavaScriptUtils.escapeJavaScript(messages.getMessage(UPLOAD_NO_FILE_SELECTED_KEY)) + "';\n");
        writer.htmlText("  var uploadMultipleFilesMsg = '" + JavaScriptUtils.escapeJavaScript(messages.getMessage(UPLOAD_MULTIPLE_FILES_SELECTED_KEY)) + "';\n");
        writer.htmlText("  var uploadTooBigMsg = '" + JavaScriptUtils.escapeJavaScript(messages.getMessage(UPLOAD_FILE_TOO_BIG_KEY)) + "';\n");
        writer.closeTag(); // script

        // navigation
        writer.openTag("nav");
        if (menuPage != null) {
            WikiPage2Html html = new WikiPage2Html(settings, messages, wikiService, false);
            writer.addHtmlWriter(html.generate(menuPage));
        }
        writer.closeTag(); // nav

        writer.openDivTag("wikipage editor");
        writer.setContinueInNewLine();

        String formPath = PathUtils.concatWebPaths("/edit/", pagePath);
        writer.openFormTag("EditForm", EscapeUtils.encodeUrl(EscapeUtils.pagePath2Url(formPath)), Method.POST);
        writer.setContinueInNewLine();

        if (wikiText != null && wikiText.getFromPos() != null) {
            writer.htmlText("<input type=\"hidden\" name=\"fromPos\" value=\"" + wikiText.getFromPos() + "\" />");
            writer.setContinueInNewLine();
        }
        if (wikiText != null && wikiText.getToPos() != null) {
            writer.htmlText("<input type=\"hidden\" name=\"toPos\" value=\"" + wikiText.getToPos() + "\" />");
            writer.setContinueInNewLine();
        }

        // wiki page title edit
        String pagePathInEditor = pagePath;
        if ("/".equals(pagePathInEditor)) {
            pagePathInEditor = ""; // avoid null value
        }
        String titleeditorHint = messages.getMessage(EDITOR_INPUT_TITLE_KEY);
        writer.htmlText("<input type=\"text\" class=\"titleinput\" name=\"titleeditor\" placeholder=\"" + EscapeUtils.escapeHtml(titleeditorHint)
                + "\" value=\"" + EscapeUtils.escapeHtml(pagePathInEditor) + "\" />");
        writer.setContinueInNewLine();

        // wiki page content editor
        String contenteditorHint = messages.getMessage(EDITOR_INPUT_CONTENT_KEY);
        writer.openTag("textarea",
                "cols=\"80\" rows=\"10\" name=\"contenteditor\" class=\"textinput\" placeholder=\"" + EscapeUtils.escapeHtml(contenteditorHint) + "\"");
        // avoid that the browser ignore initial white space
        writer.htmlText("\n");
        if (wikiText != null) {
            writer.htmlText(EscapeUtils.escapeHtml(wikiText.getText()));
        }
        writer.closeTag(); // textarea
        writer.setContinueInNewLine();

        // button panel
        writer.openDivTag("controlarea", "id=\"controlarea\"");
        writer.setContinueInNewLine();

        writer.openDivTag("section"); // block 1
        String saveTitle = messages.getMessage(EDITOR_INPUT_SAVE_KEY);
        writer.htmlText("<button type=\"submit\" name=\"save\" class=\"save\">" + EscapeUtils.escapeHtml(saveTitle) + "</button>");
        String cancelTitle = messages.getMessage(EDITOR_INPUT_CANCEL_KEY);
        writer.htmlText("<button type=\"submit\" name=\"cancel\" class=\"cancel\">" + EscapeUtils.escapeHtml(cancelTitle) + "</button>");
        writer.closeTag(); // div block 1

        writer.openDivTag("section"); // block 2
        String disabledAttribute = "";
        if (!wikiService.existsWikiFile(pagePath)) {
            disabledAttribute = " disabled=\"true\"";
        }
        String deleteTitle = messages.getMessage(EDITOR_INPUT_DELETE_KEY);
        writer.htmlText("<button type=\"button\" name=\"deleteButton\" class=\"delete\"" + " onclick=\"sendDelete()\"" + disabledAttribute
                + ">" + EscapeUtils.escapeHtml(deleteTitle) + "</button>");
        writer.closeTag(); // div block 2

        writer.openDivTag("section"); // block 3
        writer.openTag("select", "name=\"TemplateSelect\" class=\"TemplateSelect\"" + " onchange=\"insertTemplate(this.selectedIndex)\"");
        String templateSelectOption = messages.getMessage(EDITOR_INPUT_TEMPLATE_SELECT_KEY);
        writer.htmlText("<option>" + EscapeUtils.escapeHtml(templateSelectOption) + "</option>");
        writer.closeTag(); // select
        String helpTitle = messages.getMessage(EDITOR_HELP_KEY);
        writer.htmlText("<a href=\"/view/wiki/syntax/\" target=\"_blank\">" + EscapeUtils.escapeHtml(helpTitle) + "</a>");
        writer.closeTag(); // div block 3

        String uploadHint = messages.getMessage(EDITOR_INPUT_UPLOAD_HINT_KEY);
        writer.openDivTag("uploadarea", "id=\"uploadarea\" onclick=\"showPanel('uploadPanelId')\" title=\"" + EscapeUtils.escapeHtml(uploadHint) + "\"");
        String uploadTitle = messages.getMessage(EDITOR_INPUT_UPLOAD_TITLE_KEY);
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
        String title = messages.getMessage(EDITOR_UPLOADPANEL_TITLE_KEY);
        writer.htmlText(EscapeUtils.escapeHtml(title));
        writer.closeTag(); // div.header

        writer.openDivTag("body");
        writer.openFormTag("uploadForm");

        writer.openDivTag("section", "id=\"fileSelectId\"");
        String fileLabel = messages.getMessage(EDITOR_UPLOADPANEL_FILE_KEY);
        writer.htmlText("<label for=\"fileInputId\">" + EscapeUtils.escapeHtml(fileLabel) + "</label>");
        writer.htmlNewLine();
        writer.htmlText("<input type=\"file\" id=\"fileInputId\" name=\"fileSelect\" />");
        writer.closeTag(); // div.section

        writer.openDivTag("section");
        String repositoryLabel = messages.getMessage(EDITOR_UPLOADPANEL_REPOSITORY_PATH_KEY);
        writer.htmlText("<label for=\"uploadRepositoryPathId\">" + EscapeUtils.escapeHtml(repositoryLabel) + "</label>");
        writer.htmlNewLine();
        writer.htmlText("<input type=\"text\" id=\"uploadRepositoryPathId\" name=\"uploadRepositoryPath\" />");
        writer.closeTag(); // div.section

        writer.openDivTag("section");
        writer.htmlText("<input type=\"checkbox\" id=\"generateImageTagId\" name=\"generateImageTag\" checked=\"true\">");
        String imageTagLabel = messages.getMessage(EDITOR_UPLOADPANEL_IMAGE_TAG_KEY);
        writer.htmlText("<label id=\"generateImageTagLabelId\" for=\"generateImageTagId\">" + EscapeUtils.escapeHtml(imageTagLabel) + "</label>");
        writer.htmlNewLine();
        writer.htmlText("<input type=\"checkbox\" id=\"generateFileTagId\" name=\"generateFileTag\" checked=\"true\">");
        String fileTagLabel = messages.getMessage(EDITOR_UPLOADPANEL_FILE_TAG_KEY);
        writer.htmlText("<label id=\"generateFileTagLabelId\" for=\"generateFileTagId\">" + EscapeUtils.escapeHtml(fileTagLabel) + "</label>");
        writer.closeTag(); // div.section

        writer.openDivTag("footer");
        String saveButton = messages.getMessage(EDITOR_UPLOADPANEL_SAVE_KEY);
        writer.htmlText("<button type=\"button\" name=\"uploadButton\" class=\"save\" onclick=\"handleFileUpload('uploadPanelId')\">"
                + EscapeUtils.escapeHtml(saveButton) + "</button>");
        String cancelButton = messages.getMessage(EDITOR_UPLOADPANEL_CANCEL_KEY);
        writer.htmlText("<button type=\"button\" name=\"cancelButton\" class=\"cancel\" onclick=\"hidePanel('uploadPanelId')\">"
                + EscapeUtils.escapeHtml(cancelButton) + "</button>");
        writer.closeTag(); // div.footer

        writer.closeTag(); // form
        writer.closeTag(); // div.body
        writer.closeTag(); // div.panel

        writer.closeTag(); // div#uploadPanelId
    }

    /**
	 * Encode the template list as JSON string.
     *
     * @param templates enthält die Seitenvorlagen in der Form Name -> Inhalt.
     */
    private void putTemplateList(@NotNull HtmlWriter writer, @NotNull Map<String, String> templates) {
        List<String> templatePagePaths = new ArrayList<>(templates.keySet());
        Collections.sort(templatePagePaths);

        // format as JSON
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < templatePagePaths.size(); i++) {
            if (i >= 1) {
                sb.append(", ");
            }
            String templatePagePath = templatePagePaths.get(i);
            sb.append("{\"name\": \"");
            sb.append(JavaScriptUtils.escapeJavaScript(PathUtils.extractWebName(templatePagePath)));
            sb.append("\", \"content\": \"");
            sb.append(JavaScriptUtils.escapeJavaScript(templates.get(templatePagePath)));
            sb.append("\"}");
        }
        sb.append(']');

        // embed as JavaScript constant
        writer.setContinueInNewLine();
        writer.openTag("script", "type=\"text/javascript\"");
        writer.htmlText("\n");
        writer.htmlText("var templates = " + sb.toString() + ";\n");
        writer.closeTag(); // script
    }

    /**
	 * Uploads a binary file to the repository.
     */
    @NotNull
    public HttpResponse handleUploadRequest(@NotNull String urlPath, byte @NotNull [] httpBody) {
        // cut off "/upload" prefix
        String filePath = urlPath.substring(7).trim();

        // catch invalid filename
        if (filePath.length() == 0 || filePath.endsWith("/")) {
            logger.write("Upload file name '" + filePath + "' is invalid");
            String msg = messages.getMessage(UPLOAD_INVALID_NAME_KEY, filePath);
            return generateJsonResponse(400, msg);
        }
        // catch backwards path navigation
        if (filePath.contains("..")) {
            logger.write("Upload file name '" + filePath + "' contains illegal parent navigation");
            String msg = messages.getMessage(UPLOAD_PARENT_NAVIGATION_KEY, filePath);
            return generateJsonResponse(400, msg);
        }
        if (filePath.charAt(0) != '/') {
            filePath = '/' + filePath;
        }

        try {
            if (repositoryService.getFile(filePath) != null) {
                logger.write("Upload file name '" + filePath + "' already exists");
                String msg = messages.getMessage(UPLOAD_ALREADY_EXISTING_KEY, filePath);
                throw new Exception(msg);
            }
            AnyFile anyFile = new AnyFile(filePath);
            repositoryService.writeBinaryFile(anyFile, httpBody, null);
            logger.write("File '" + filePath + "' successfully uploaded");
        } catch (Exception e) {
            logger.write("Error uploading file '" + filePath + "'", e);
            return generateJsonResponse(500, e.getMessage());
        }

        return generateJsonResponse(200, "File upload successful: " + filePath);
    }

    private HttpResponse generateJsonResponse(int statusCode, @NotNull String jsonText) {
        HttpResponse result = new HttpResponse();
        result.statusCode = statusCode;
        result.contentType = HttpResponse.CONTENT_TYPE_JSON_UTF8;
        result.setContent(JavaScriptUtils.generateJson(jsonText));
        return result;
    }
}
