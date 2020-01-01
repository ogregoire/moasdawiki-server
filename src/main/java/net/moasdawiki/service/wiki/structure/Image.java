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

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Platzhalter für eine Grafik, die an dieser Stelle angezeigt werden soll.
 * 
 * @author Herbert Reiter
 */
public class Image extends PageElement {

	/**
	 * URL der Grafik.
	 */
	@NotNull
	private final String url;

	/**
	 * Darstellungsoptionen. Nicht null.
	 */
	@NotNull
	private final Map<String, String> options;

	/**
	 * Erzeugt einen Verweis auf eine Grafikdatei.
	 * 
	 * @param url Adresse relativ oder absolut zur Wikiseite. Eine absolute
	 *            Angabe beginnt mit "/".
	 * @param options Optionen zur Darstellung der Grafik.
	 */
	public Image(@NotNull String url, @Nullable Map<String, String> options, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.url = url;
		if (options != null) {
			this.options = options;
		} else {
			this.options = Collections.emptyMap();
		}
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Gibt die URL der Grafik zurück.
	 */
	@NotNull
	public String getUrl() {
		return url;
	}

	@NotNull
	public Map<String, String> getOptions() {
		return options;
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new Image(url, new HashMap<>(options), fromPos, toPos);
	}
}
