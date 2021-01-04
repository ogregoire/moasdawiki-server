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

import java.util.ArrayList;
import java.util.List;

import net.moasdawiki.util.xml.XmlAttribute;
import net.moasdawiki.util.xml.XmlElementRef;
import net.moasdawiki.util.xml.XmlRootElement;

/**
 * JAXB-Bean mit der Liste der modifizierten Repository-Dateien auf dem
 * Server.
 */
@XmlRootElement(name = "list-modified-files-response")
public class ListModifiedFilesResponseXml extends AbstractSyncXml {
	@XmlAttribute
	public String version;

	@XmlAttribute(name = "current-server-time")
	public String currentServerTime;

	@XmlElementRef
	public List<SingleFileXml> fileList = new ArrayList<>();
}
