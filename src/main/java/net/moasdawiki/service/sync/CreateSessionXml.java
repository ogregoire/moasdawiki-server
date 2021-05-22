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

package net.moasdawiki.service.sync;

import net.moasdawiki.util.xml.XmlAttribute;
import net.moasdawiki.util.xml.XmlElement;
import net.moasdawiki.util.xml.XmlRootElement;

/**
 * JAXB-Bean für eine Session-Anfrage.
 */
@XmlRootElement(name = "create-session")
public class CreateSessionXml extends AbstractSyncXml {
	@XmlAttribute
	public String version;
	
	@XmlElement(name = "client-session-id")
	public String clientSessionId;

	@XmlElement(name = "client-name")
	public String clientName;

	@XmlElement(name = "client-version")
	public String clientVersion;

	@XmlElement(name = "client-host")
	public String clientHost;
}
