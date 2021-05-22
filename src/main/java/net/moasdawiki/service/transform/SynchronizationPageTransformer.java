/*
 * MoasdaWiki Server
 *
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3 as
 * published by the Free Software Foundation (AGPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.moasdawiki.service.transform;

import net.moasdawiki.base.Messages;
import net.moasdawiki.service.sync.SessionData;
import net.moasdawiki.service.sync.SynchronizationService;
import net.moasdawiki.service.wiki.structure.*;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Transforms the wiki tag <code>{{sync-status}}</code> used on the
 * synchronization status page.
 */
public class SynchronizationPageTransformer implements TransformWikiPage {

    private static final String ACTION_KEY = "SynchronizationPageTransformer.action";
    private static final String ACTION_PERMIT_KEY = "SynchronizationPageTransformer.action.permit";
    private static final String ACTION_REMOVE_KEY = "SynchronizationPageTransformer.action.remove";
    private static final String CLIENT_KEY = "SynchronizationPageTransformer.client";
    private static final String CLIENT_DEVICE_NAME_KEY = "SynchronizationPageTransformer.client.device-name";
    private static final String CLIENT_SESSION_ID_KEY = "SynchronizationPageTransformer.client.session-id";
    private static final String SERVER_SESSION_ID_KEY = "SynchronizationPageTransformer.server.session-id";
    private static final String SERVER_SESSION_CREATED_KEY = "SynchronizationPageTransformer.server.session.created";
    private static final String SERVER_SESSION_LAST_SYNC_KEY = "SynchronizationPageTransformer.server.session.last-sync";
    private static final String SERVER_SESSION_PERMITTED_KEY = "SynchronizationPageTransformer.server.session.permitted";
    private static final String DATEFORMAT_KEY = "WikiTagsTransformer.dateformat.datetime";

    private final Messages messages;
    private final SynchronizationService synchronizationService;

    /**
     * Constructor.
     */
    public SynchronizationPageTransformer(@NotNull Messages messages, @NotNull SynchronizationService synchronizationService) {
        this.messages = messages;
        this.synchronizationService = synchronizationService;
    }

    @NotNull
    @Override
    public WikiPage transformWikiPage(@NotNull WikiPage wikiPage) {
        return TransformerHelper.transformPageElements(wikiPage, this::transformPageElement);
    }

    /**
     * Replaces the <code>{{sync-status}}</code> tag by a list of sessions and attributes.
     */
    private PageElement transformPageElement(@NotNull PageElement pageElement) {
        if (pageElement instanceof WikiTag) {
            WikiTag wikiTag = (WikiTag) pageElement;
            if ("sync-status".equals(wikiTag.getTagname())) {
                Table table = new Table(null, null, null);
                // headers
                table.newRow(null);
                table.addCell(new TableCell(new TextOnly(messages.getMessage(CLIENT_DEVICE_NAME_KEY)), true, null));
                table.addCell(new TableCell(new TextOnly(messages.getMessage(CLIENT_KEY)), true, null));
                table.addCell(new TableCell(new Html(messages.getMessage(SERVER_SESSION_ID_KEY) + " /<br>"
                        + messages.getMessage(CLIENT_SESSION_ID_KEY)), true, null));
                table.addCell(new TableCell(new TextOnly(messages.getMessage(SERVER_SESSION_CREATED_KEY)), true, null));
                table.addCell(new TableCell(new TextOnly(messages.getMessage(SERVER_SESSION_LAST_SYNC_KEY)), true, null));
                table.addCell(new TableCell(new TextOnly(messages.getMessage(ACTION_KEY)), true, null));

                // session list
                List<SessionData> sessionList = synchronizationService.getSessions();
                sessionList.sort((sessionData1, sessionData2) -> {
                    int comp;
                    if (sessionData1.clientHost == null && sessionData2.clientHost == null) {
                        comp = 0;
                    } else if (sessionData2.clientHost == null) {
                        comp = -1;
                    } else if (sessionData1.clientHost == null) {
                        comp = 1;
                    } else {
                        comp = sessionData1.clientHost.compareTo(sessionData2.clientHost);
                    }
                    if (comp == 0) {
                        comp = sessionData1.createTimestamp.compareTo(sessionData2.createTimestamp);
                    }
                    return comp;
                });
                DateFormat df = new SimpleDateFormat(messages.getMessage(DATEFORMAT_KEY));
                for (SessionData sessionData : sessionList) {
                    table.newRow(null);
                    table.addCell(new TableCell(new TextOnly(sessionData.clientHost), false, null));
                    table.addCell(new TableCell(new TextOnly(sessionData.clientName + " " + sessionData.clientVersion), false, null));
                    table.addCell(new TableCell(new Html(sessionData.serverSessionId + " /<br>" + sessionData.clientSessionId), false, null));
                    String timestamp = df.format(sessionData.createTimestamp);
                    table.addCell(new TableCell(new TextOnly(timestamp), false, null));
                    String lastSync = "";
                    if (sessionData.lastSyncTimestamp != null) {
                        lastSync = df.format(sessionData.lastSyncTimestamp);
                    } else if (sessionData.authorized) {
                        lastSync = messages.getMessage(SERVER_SESSION_PERMITTED_KEY);
                    }
                    table.addCell(new TableCell(new TextOnly(lastSync), false, null));
                    String buttonHtml = "<form>";
                    if (!sessionData.authorized) {
                        buttonHtml += "<button type=\"button\" class=\"save\" onclick=\"syncPermitSession('" + sessionData.serverSessionId
                                + "')\">" + messages.getMessage(ACTION_PERMIT_KEY) + "</button> ";
                    }
                    buttonHtml += "<button type=\"button\" class=\"cancel\" onclick=\"syncDropSession('" + sessionData.serverSessionId
                            + "')\">" + messages.getMessage(ACTION_REMOVE_KEY) + "</button>";
                    buttonHtml += "</form>";
                    table.addCell(new TableCell(new Html(buttonHtml), false, null));
                }

                return table;
            }
        }
        return pageElement;
    }
}
