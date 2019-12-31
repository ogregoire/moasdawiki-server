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

package net.moasdawiki.plugin.sync;

import java.util.Date;

/**
 * Enthält die Daten einer Wiki-Client-Session.
 *
 * @author Herbert Reiter
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
