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

package net.moasdawiki.server;

import net.moasdawiki.service.HttpResponse;
import net.moasdawiki.service.handler.EditorHandler;
import net.moasdawiki.service.handler.FileDownloadHandler;
import net.moasdawiki.service.handler.SearchHandler;
import net.moasdawiki.service.handler.ViewPageHandler;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.sync.SynchronizationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Forwards incoming HTTP requests to the corresponding service method.
 */
public class RequestDispatcher {

    private final HtmlService htmlService;
    private final ViewPageHandler viewPageHandler;
    private final SearchHandler searchHandler;
    private final EditorHandler editorHandler;
    private final FileDownloadHandler fileDownloadHandler;

    /**
     * Is null in App.
     */
    @Nullable
    private final SynchronizationService synchronizationService;

    /**
     * Constructor.
     */
    public RequestDispatcher(@NotNull HtmlService htmlService, @NotNull ViewPageHandler viewPageHandler,
                             @NotNull SearchHandler searchHandler, @NotNull EditorHandler editorHandler,
                             @NotNull FileDownloadHandler fileDownloadHandler,
                             @Nullable SynchronizationService synchronizationService) {
        this.htmlService = htmlService;
        this.viewPageHandler = viewPageHandler;
        this.searchHandler = searchHandler;
        this.editorHandler = editorHandler;
        this.fileDownloadHandler = fileDownloadHandler;
        this.synchronizationService = synchronizationService;
    }

    /**
     * This method is called on every incoming HTTP request. It forwards the
     * call to a corresponding service method. The response is transformed to
     * a HTTP response.
     */
    @NotNull
    public HttpResponse handleRequest(@NotNull HttpRequest httpRequest) {
        String urlPath = httpRequest.urlPath;
        HttpResponse httpResponse = null;
        if (urlPath.equals("/")) {
            httpResponse = viewPageHandler.handleRootPath();
        }
        else if (urlPath.startsWith("/view/")) {
            httpResponse = viewPageHandler.handleViewPath(urlPath);
        }
        else if (urlPath.startsWith("/search/")) {
            String query = httpRequest.urlParameters.get("text");
            httpResponse = searchHandler.handleSearchRequest(query);
        }
        else if (urlPath.startsWith("/edit/")) {
            httpResponse = editorHandler.handleEditRequest(urlPath, httpRequest.urlParameters);
        }
        else if (urlPath.startsWith("/upload")) {
            httpResponse = editorHandler.handleUploadRequest(urlPath, httpRequest.httpBody);
        }
        else if (urlPath.startsWith("/sync")) {
            httpResponse = handleSynchronizationService(httpRequest);
        }
        else if (urlPath.startsWith("/img/")) {
            httpResponse = fileDownloadHandler.handleDownloadImg(urlPath);
        }
        else if (urlPath.startsWith("/file/")) {
            httpResponse = fileDownloadHandler.handleDownloadFile(urlPath);
        }
        else if (urlPath.startsWith("/") && urlPath.lastIndexOf('/') == 0) {
            httpResponse = fileDownloadHandler.handleDownloadRoot(urlPath);
        }

        if (httpResponse == null) {
            httpResponse = htmlService.generateErrorPage(404, "wiki.server.url.unmapped", httpRequest.urlPath);
        }
        return httpResponse;
    }

    @Nullable
    private HttpResponse handleSynchronizationService(@NotNull HttpRequest httpRequest) {
        if (synchronizationService == null) {
            return null;
        }

        String urlPath = httpRequest.urlPath;
        switch (urlPath) {
            case "/sync-gui/session-permit": {
                String sessionId = httpRequest.urlParameters.get("session-id");
                return synchronizationService.handleSessionPermit(sessionId);
            }
            case "/sync-gui/session-drop": {
                String sessionId = httpRequest.urlParameters.get("session-id");
                return synchronizationService.handleSessionDrop(sessionId);
            }
            case "/sync/create-session": {
                return synchronizationService.handleCreateSession(httpRequest.httpBody);
            }
            case "/sync/check-session": {
                return synchronizationService.handleCheckSession(httpRequest.httpBody);
            }
            case "/sync/list-modified-files": {
                return synchronizationService.handleListModifiedFiles(httpRequest.httpBody);
            }
            case "/sync/read-file": {
                return synchronizationService.handleReadFile(httpRequest.httpBody);
            }
            default:
                return null;
        }
    }
}
