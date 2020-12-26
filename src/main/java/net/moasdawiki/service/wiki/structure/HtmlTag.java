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

/**
 * Represents a generic HTML tag.
 */
public class HtmlTag extends PageElementWithChild {

	@NotNull
	private final String tagName;

	@Nullable
	private final String tagAttributes;

	public HtmlTag(@NotNull String tagName, @Nullable PageElement content) {
		this(tagName, null, content, null, null);
	}

	public HtmlTag(@NotNull String tagName, @Nullable String tagAttributes, @Nullable PageElement content) {
		this(tagName, tagAttributes, content, null, null);
	}

	public HtmlTag(@NotNull String tagName, @Nullable String tagAttributes, @Nullable PageElement content,
				   @Nullable Integer fromPos, @Nullable Integer toPos) {
		super(content, fromPos, toPos);
		this.tagName = tagName;
		this.tagAttributes = tagAttributes;
	}

	public String getTagName() {
		return tagName;
	}

	public String getTagAttributes() {
		return tagAttributes;
	}

	public boolean isInline() {
		return true;
	}

	@NotNull
	public PageElement clonePageElement() {
		if (child != null) {
			return new HtmlTag(tagName, tagAttributes, child.clonePageElement(), fromPos, toPos);
		} else {
			return new HtmlTag(tagName, tagAttributes, null, fromPos, toPos);
		}
	}
}
