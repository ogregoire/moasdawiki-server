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
 * Enthält semantisch annotierten Inhalt. Optional kann auch ein Präfix
 * angegeben sein. Semantische Tags bestehen immer aus einem öffnenden und einem
 * schließenden Tag bzw. in Kurznotation &lt;tag /&gt;, wenn es keinen Inhalt
 * hat.<br>
 * <br>
 * Beispiel: &lt;tel>0160/1234567&lt;/tel>
 */
public class XmlTag extends PageElementWithChild {

	/**
	 * Präfix des XML-Tags. Ein XML-Tag mit Präfix hat die Notation
	 * <tt>&lt;präfix:tagname&gt;</tt>. null -> kein Präfix.
	 */
	@Nullable
	private final String prefix;

	/**
	 * Name des XML-Tags.
	 */
	@NotNull
	private final String name;

	/**
	 * Parameter des XML-Tags. Nicht null.
	 */
	private Map<String, String> options;

	public XmlTag(@Nullable String prefix, @NotNull String name, @Nullable Map<String, String> options, @Nullable PageElement content,
				  @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.prefix = prefix;
		this.name = name;
		this.options = options;
		if (this.options == null) {
			this.options = Collections.emptyMap();
		}
	}

	/**
	 * Gibt den Präfix des XML-Tags zurück. Der Präfix ist optional und wird mit
	 * einem Doppelpunkt getrennt vor dem Namen angegeben.<br>
	 * Syntax: <tt>&lt;präfix:name&gt;</tt>
	 * 
	 * @return null -> kein Präfix vorhanden.
	 */
	@Nullable
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Gibt den Namen des XML-Tags zurück.<br>
	 * Syntax:<br>
	 * <tt>&lt;name&gt;</tt><br>
	 * <tt>&lt;präfix:name&gt;</tt>
	 */
	@NotNull
	public String getName() {
		return name;
	}

	/**
	 * Gibt die Parameter des XML-Tags zurück.
	 * 
	 * @return Nicht null.
	 */
	@NotNull
	public Map<String, String> getOptions() {
		return options;
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new XmlTag(prefix, name, new HashMap<>(options), child.clonePageElement(), fromPos,
					toPos);
		} else {
			return new XmlTag(prefix, name, new HashMap<>(options), null, fromPos, toPos);
		}
	}
}
