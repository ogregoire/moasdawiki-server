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

package net.moasdawiki.service.sync;

import net.moasdawiki.util.xml.XmlAttribute;
import net.moasdawiki.util.xml.XmlElement;
import net.moasdawiki.util.xml.XmlRootElement;

/**
 * JAXB-Bean mit dem Inhalt einer einzelnen Datei.
 */
@XmlRootElement(name = "read-file-response")
public class ReadFileResponseXml extends AbstractSyncXml {
	@XmlAttribute
	public String version;

	/**
	 * Zeitstempel der letzten Änderung.
	 */
	@XmlElement
	public String timestamp;

	/**
	 * Binärdaten base64-kodiert.
	 */
	@XmlElement
	public String content;
}
