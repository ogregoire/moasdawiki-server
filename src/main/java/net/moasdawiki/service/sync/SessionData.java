/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.service.sync;

import java.util.Date;

/**
 * Enthält die Daten einer Wiki-Client-Session.
 */
public class SessionData {
	public String serverSessionId;
	public String clientSessionId;
	public Date createTimestamp;
	public String clientName; // null --> unbekannt
	public String clientVersion; // null --> unbekannt
	public String clientHost; // null --> unbekannt
	public boolean authorized; // true --> Benutzer hat die Serversession genehmigt
	public Date lastSyncTimestamp; // null --> noch nie
}
