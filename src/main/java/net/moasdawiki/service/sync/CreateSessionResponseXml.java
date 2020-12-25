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

package net.moasdawiki.service.sync;

import net.moasdawiki.util.xml.XmlAttribute;
import net.moasdawiki.util.xml.XmlElement;
import net.moasdawiki.util.xml.XmlRootElement;

/**
 * JAXB-Bean mit der zugewiesenen Session-ID.
 */
@XmlRootElement(name = "create-session-response")
public class CreateSessionResponseXml extends AbstractSyncXml {
	@XmlAttribute
	public String version;

	@XmlElement(name = "server-session-id")
	public String serverSessionId;

	@XmlElement(name = "server-name")
	public String serverName;

	@XmlElement(name = "server-version")
	public String serverVersion;

	@XmlElement(name = "server-host")
	public String serverHost;
}