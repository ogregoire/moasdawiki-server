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

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Generisches Tag mit der Syntax <tt>{{name:value | option1=value1 | option2}}</tt>.
 * Spezielle Varianten davon werden in eigentständige Elementtypen übersetzt.
 */
public class WikiTag extends PageElement {

	@NotNull
	private final String tagname;

	/**
	 * null -> kein Wert nach Doppelpunkt ':'
	 */
	@Nullable
	private final String value;

	@NotNull
	private final Map<String, String> options;

	public WikiTag(@NotNull String tagname, @Nullable String value, @Nullable Map<String, String> options, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.tagname = tagname;
		this.value = value;
		if (options != null) {
			this.options = new HashMap<>(options);
		} else {
			this.options = Collections.emptyMap();
		}
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	@NotNull
	public String getTagname() {
		return tagname;
	}

	@Nullable
	public String getValue() {
		return value;
	}

	@NotNull
	public Map<String, String> getOptions() {
		return options;
	}

	@Override
	public boolean isInline() {
		return true;
	}

	/**
	 * Erstellt eine tiefe Kopie der Wikiseite.
	 */
	@NotNull
	public WikiTag cloneTyped() {
		return new WikiTag(tagname, value, options, fromPos, toPos);
	}

	@NotNull
	@Override
	public PageElement clonePageElement() {
		return cloneTyped();
	}
}
