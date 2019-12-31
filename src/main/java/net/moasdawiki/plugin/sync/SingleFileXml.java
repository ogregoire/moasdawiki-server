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
import net.moasdawiki.util.xml.XmlRootElement;
import net.moasdawiki.util.xml.XmlValue;

/**
 * Enthält die Angaben für eine einzelne Repository-Datei.
 *
 * @author Herbert Reiter
 */
@XmlRootElement(name = "file")
public class SingleFileXml extends AbstractSyncXml {

	/**
	 * Zeitstempel der letzten Inhaltsänderung der Datei. Nicht
	 * <code>null</code>.
	 */
	@XmlAttribute
	public String timestamp;

	/**
	 * Der absolute Pfad der Datei innerhalb des Repositories inkl. Dateiendung.
	 * Nicht <code>null</code>.
	 */
	@XmlValue
	public String filePath;
}
