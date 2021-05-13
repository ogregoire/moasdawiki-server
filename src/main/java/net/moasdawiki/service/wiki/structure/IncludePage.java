/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;

/**
 * Includes a complete wiki page.
 * <p>
 * The tag will be replaced by the WikiPage object of the sub page.
 * <p>
 * Syntax: <code>{{includepage:pagePath}}</code>
 */
public class IncludePage extends PageElement {

    /**
     * Name of the wiki page the tag refers to.
     * <p>
     * If the value doesn't start with '/' the reference is relative to the
     * surrounding wiki page.
     */
    @NotNull
    private final String pagePath;

    public IncludePage(@NotNull String pagePath, Integer fromPos, Integer toPos) {
        super();
        this.pagePath = pagePath;
        this.fromPos = fromPos;
        this.toPos = toPos;
    }

    @NotNull
    public String getPagePath() {
        return pagePath;
    }

    public boolean isInline() {
        return false;
    }

    @NotNull
    public PageElement clonePageElement() {
        return new IncludePage(pagePath, fromPos, toPos);
    }
}
