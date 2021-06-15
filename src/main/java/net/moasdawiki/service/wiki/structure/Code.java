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

/**
 * Contains a programming language code snippet or any other preformatted text.
 *
 * The text is displayed with a monospaced font, line breaks are retained.
 * For some content types there is also syntax highlighting.
 *
 * The content will be fully escaped, wiki commands or HTML tags are NOT
 * interpreted.
 */
public class Code extends PageElement {
	/**
	 * Name of the programming language or content type for syntax
	 * highlighting.
	 */
	private final ContentType contentType;

	/**
	 * Content text.
	 */
	@NotNull
	private final String text;

	/**
	 * Constructor.
	 */
	public Code(ContentType contentType, @NotNull String text, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.contentType = contentType;
		this.text = text;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	public ContentType getContentType() {
		return contentType;
	}

	@NotNull
	public String getText() {
		return text;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new Code(contentType, text, fromPos, toPos);
	}

	/**
	 * Supported content types for syntax highlighting.
	 */
	public enum ContentType {
		NONE, HTML, INI, JAVA, PROPERTIES, XML, YAML
	}
}
