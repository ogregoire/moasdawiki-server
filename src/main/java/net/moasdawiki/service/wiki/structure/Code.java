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

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enthält Programmcode einer Programmiersprache oder beliebigen anderen
 * vorformatierten Text.<br>
 * <br>
 * Wenn der Name der Programmiersprache angegeben wurde und für diese die
 * Hervorhebungsregeln bekannt sind, werden bestimmte Schlüsselwörter und
 * Sprachelemente beim Rendern automatisch formatiert. Die Darstellung erfolgt
 * stets mit dicktengleicher Schrift, Zeilenumbrüche werden beibehalten.<br>
 * <br>
 * Der anzuzeigende Text wird grundsätzlich voll escaped, d.h. Wikikommandos
 * oder HTML werden keinesfalls interpretiert. Wenn Wikikommandos benutzt werden
 * sollen, muss stattdessen die Inlinevariante <code>@@...@@</code> verwendet
 * werden.
 * 
 * @author Herbert Reiter
 */
public class Code extends PageElement {
	/**
	 * Name der Programmiersprache. <code>null</code> --> keine
	 * Syntaxhervorhebung.
	 */
	@Nullable
	private final String language;

	/**
	 * Programmcode, der dargestellt werden soll.
	 */
	@NotNull
	private final String text;

	public Code(@Nullable String language, @NotNull String text, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.language = language;
		this.text = text;
		this.fromPos = fromPos;
		this.toPos = toPos;
	}

	/**
	 * Gibt den Namen der Programmiersprache zurück. null -> automatisch
	 * bestimmen.
	 */
	@Nullable
	public String getLanguage() {
		return language;
	}

	/**
	 * Gibt den Programmcode zurück.
	 */
	@NotNull
	public String getText() {
		return text;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new Code(language, text, fromPos, toPos);
	}
}
