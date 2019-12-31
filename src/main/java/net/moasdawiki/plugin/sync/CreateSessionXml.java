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

import net.moasdawiki.util.xml.XmlAttribute;
import net.moasdawiki.util.xml.XmlElement;
import net.moasdawiki.util.xml.XmlRootElement;

/**
 * JAXB-Bean für eine Session-Anfrage.
 *
 * @author Herbert Reiter
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
