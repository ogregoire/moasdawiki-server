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

package net.moasdawiki.service.transform;

import net.moasdawiki.service.wiki.structure.WikiPage;
import org.jetbrains.annotations.NotNull;

/**
 * Interface that has to be implemented by transformer classes which interpret
 * functional tags and replace them by an appropriate content.
 */
public interface TransformWikiPage {

    /**
     * Interprets functional tags and replaces them by an appropriate content.
     *
     * @param wikiPage Wiki page to be transformed. The object must not be modified!
     */
    @NotNull
    WikiPage transformWikiPage(@NotNull WikiPage wikiPage);
}
