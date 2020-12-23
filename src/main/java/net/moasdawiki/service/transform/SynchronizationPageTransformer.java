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

package net.moasdawiki.service.transform;

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
 *
 * TODO: translate to English
 */
public class SynchronizationPageTransformer implements TransformWikiPage {

    private final SynchronizationService synchronizationService;

    /**
     * Constructor.
     */
    public SynchronizationPageTransformer(@NotNull SynchronizationService synchronizationService) {
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
                table.addCell(new TableCell(new TextOnly("Gerätename"), true, null));
                table.addCell(new TableCell(new TextOnly("Client"), true, null));
                table.addCell(new TableCell(new Html("Server-Session-ID /<br>Client-Session-ID"), true, null));
                table.addCell(new TableCell(new TextOnly("Erzeugt"), true, null));
                table.addCell(new TableCell(new TextOnly("Letzte Synchronisierung"), true, null));
                table.addCell(new TableCell(new TextOnly("Aktion"), true, null));

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
                DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
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
                        lastSync = "Erlaubnis erteilt";
                    }
                    table.addCell(new TableCell(new TextOnly(lastSync), false, null));
                    String buttonHtml = "<form>";
                    if (!sessionData.authorized) {
                        buttonHtml += "<button type=\"button\" class=\"save\" onclick=\"syncPermitSession('" + sessionData.serverSessionId
                                + "')\">Erlauben</button> ";
                    }
                    buttonHtml += "<button type=\"button\" class=\"cancel\" onclick=\"syncDropSession('" + sessionData.serverSessionId
                            + "')\">Löschen</button>";
                    buttonHtml += "</form>";
                    table.addCell(new TableCell(new Html(buttonHtml), false, null));
                }

                return table;
            }
        }
        return pageElement;
    }
}
