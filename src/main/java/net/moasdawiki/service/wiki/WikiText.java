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

package net.moasdawiki.service.wiki;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains the content of a wiki file or a section of a wiki file.
 */
public class WikiText {

    /**
     * Content of the wiki file. Uses wiki syntax.
     */
    @NotNull
    private final String text;

    /**
     * Position of the first character of the wiki file.<br>
     * <br>
     * <code>fromPos</code> und <code>toPos</code> geben den Ausschnitt in der
     * Wikidatei an, dem der Text in {@link #text} entspricht. Ist
     * <code>fromPos == toPos</code>, wird eine leere Zeichenkette referenziert.
     * Wenn beide Werte gesetzt sind, muss <code>0 <= fromPos <= toPos</code>
     * gelten. <code>null</code> -> gesamte Wikidatei.
     */
    @Nullable
    private Integer fromPos;

    /**
     * Position behind the last character of the wiki file.<br>
     * <br>
     * <code>fromPos</code> und <code>toPos</code> geben den Ausschnitt in der
     * Wikidatei an, dem der Text in {@link #text} entspricht. Ist
     * <code>fromPos == toPos</code>, wird eine leere Zeichenkette referenziert.
     * Wenn beide Werte gesetzt sind, muss <code>0 <= fromPos <= toPos</code>
     * gelten. <code>null</code> -> gesamte Wikidatei.
     */
    @Nullable
    private Integer toPos;

    /**
     * Constructor.
     *
     * @param text content of the wiki file.
     */
    public WikiText(@NotNull String text) {
        this.text = text;
    }

    /**
     * Constructor.
     *
     * @param text    content of the wiki file.
     * @param fromPos position of the first character of the wiki file.
     * @param toPos   position behind the last character of the wiki file.
     */
    public WikiText(@NotNull String text, @Nullable Integer fromPos, @Nullable Integer toPos) {
        this.text = text;
        this.fromPos = fromPos;
        this.toPos = toPos;
    }

    /**
     * Returns the content of the wiki file.
     */
    @NotNull
    public String getText() {
        return text;
    }

    /**
     * Returns the position of the first character of the wiki file.
     * <p>
     * If {@link #fromPos} and {@link #toPos} are not null, they define a section in a wiki file
     * that corresponds to the {@link #text}. They must hold <code>0 <= fromPos <= toPos</code>.
     * If <code>fromPos == toPos</code> an empty string is referred.
     * In case of null the whole wiki file is referred.
     */
    @Nullable
    public Integer getFromPos() {
        return fromPos;
    }

    /**
     * Returns the position behind the last character of the wiki file.
	 * <p>
	 * If {@link #fromPos} and {@link #toPos} are not null, they define a section in a wiki file
	 * that corresponds to the {@link #text}. They must hold <code>0 <= fromPos <= toPos</code>.
	 * If <code>fromPos == toPos</code> an empty string is referred.
	 * In case of null the whole wiki file is referred.
     */
    @Nullable
    public Integer getToPos() {
        return toPos;
    }
}
